package xpadro.spring.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamListener implements MessageListener {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private boolean streamOpened;

  private LinkedBlockingDeque<Message> queue;

  private final long timeoutMillis;

  public StreamListener(int capacity, long timeout, TimeUnit unit) {
    if (capacity < 1) {
      queue = new LinkedBlockingDeque<>();
    } else {
      queue = new LinkedBlockingDeque<>(capacity);
    }
    if (timeout < 1) {
      timeoutMillis = timeout;
    } else {
      timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
    }
  }

  public StreamListener(int capacity) {
    this(capacity, -1, TimeUnit.MILLISECONDS);
  }

  public StreamListener() {
    this(-1, -1, TimeUnit.MILLISECONDS);
  }

  @Override
  public void onMessage(Message message) {
    queue.offer(message);
  }

  private class MessageIterator implements Iterator<Message> {

    private long lastPoll;

    @Override
    public boolean hasNext() {
      return true;
    }

    @Override
    public Message next() {
      synchronized (this) {
        lastPoll = System.currentTimeMillis();
        while (timeoutMillis < 0 || lastPoll + timeoutMillis > System.currentTimeMillis()) {
          try {
            Message result = queue.poll(1, TimeUnit.MINUTES);
            if (result != null) {
              return result;
            }
          } catch (InterruptedException e) {
            log.error("Timeout", e);
          }
        }
        return null;
      }
    }

  }

  public synchronized Stream<Message> openStream() {
    if (streamOpened) {
      throw new IllegalStateException("Stream already open");
    }
    streamOpened = true;
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(new MessageIterator(), Spliterator.ORDERED),
        false);
  }

  public boolean isStreamOpened() {
    return streamOpened;
  }

}
