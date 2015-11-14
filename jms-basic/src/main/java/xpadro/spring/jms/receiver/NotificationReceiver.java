package xpadro.spring.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.JmsUtils;
import xpadro.spring.jms.model.Notification;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NotificationReceiver implements MessageListener {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final int expected;
  private int received;
  private boolean error;
  private final AtomicBoolean streamOpened = new AtomicBoolean();

  private LinkedBlockingDeque<Notification> queue = new LinkedBlockingDeque<>();

  private int pollTimeout = 1;

  public NotificationReceiver(int expected) {
    this.expected = expected;
  }

  @Override
  public void onMessage(Message message) {
    try {
      Notification notification = (Notification) ((ObjectMessage) message).getObject();
      queue.offer(notification);
      if (++received > expected) {
        log.warn("Received " + received + " messages, but expecting only " + expected + "!");
      } else {
        log.info("Received " + received + " messages.");
      }
    } catch (JMSException e) {
      error = true;
      throw JmsUtils.convertJmsAccessException(e);
    }
  }

  private class MessageIterator implements Iterator<Notification> {
    private int count;

    @Override
    public boolean hasNext() {
      return count < expected && !error;
    }

    @Override
    public Notification next() {
      while (true) {
        try {
          Notification result = queue.poll(pollTimeout, TimeUnit.MINUTES);
          if (result != null) {
            count++;
            return result;
          } else {
            if (error) {
              return null;
            }
            log.info("Waiting " + pollTimeout + " minute(s)");
          }
        } catch (InterruptedException e) {
          error = true;
          log.error("Timeout", e);
          return null;
        }
      }
    }

  }

  public Stream<Notification> openStream() {
    if (!streamOpened.compareAndSet(false, true)) {
      throw new IllegalStateException("Stream already open");
    }
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(new MessageIterator(), Spliterator.ORDERED),
        false);
  }

}
