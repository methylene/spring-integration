package xpadro.spring.jms.receiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
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

  private StreamListener(int capacity, long timeout, TimeUnit unit) {
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

  public static class Builder {
    private int capacity = -1;
    private long timeout = -1;
    private TimeUnit unit = TimeUnit.MILLISECONDS;
    private final Connection connection;
    private final Queue queue;

    private Builder(Connection connection, Queue queue) {
      this.connection = connection;
      this.queue = queue;
    }

    public Builder timeout(long timeout, TimeUnit unit) {
      this.timeout = timeout;
      this.unit = unit;
      return this;
    }

    public Builder capacity(int capacity) {
      this.capacity = capacity;
      return this;
    }

    public Stream<Message> start() throws JMSException {
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      MessageConsumer consumer = session.createConsumer(queue);
      StreamListener listener = new StreamListener(capacity, timeout, unit);
      consumer.setMessageListener(listener);
      return listener.openStream();
    }

  }

  public static Builder listeningOn(Connection connection, Queue queue) {
    return new Builder(connection, queue);
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
      throw new java.lang.IllegalStateException("Stream already open");
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
