package xpadro.spring.jms.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import xpadro.spring.jms.util.QueueListener;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
public class App implements Callable<Long> {

  @Autowired
  private JmsTemplate jmsTemplate;

  @Autowired
  private Connection connection;

  @Autowired
  private ExecutorService executor;

  @Autowired
  private Long appTimeoutMillis;

  @Autowired
  @Qualifier("numberOfMessages")
  private Long numberOfMessages;

  @Autowired
  @Qualifier("conversionRequest")
  private Queue conversionRequestQueue;

  @Autowired
  @Qualifier("conversionResponse")
  private Queue conversionResponseQueue;

  @Autowired
  @Qualifier("archiveRequest")
  private Queue archiveRequestQueue;

  @Autowired
  @Qualifier("archiveResponse")
  private Queue archiveResponseQueue;

  @Autowired
  @Qualifier("sourceStream")
  private Supplier<Stream<String>> sourceStream;

  public Long call() throws Exception {

    Stream<Message> conversionResponses = QueueListener.connect(connection, conversionResponseQueue).start();
    Stream<Message> archiveResponses = QueueListener.connect(connection, archiveResponseQueue).start();

    executor.submit(() -> {
      sourceStream.get().forEachOrdered(message ->
          jmsTemplate.convertAndSend(conversionRequestQueue, message));
      return null;
    });

    executor.submit(() -> {
      conversionResponses.limit(numberOfMessages).forEachOrdered(message ->
          jmsTemplate.convertAndSend(archiveRequestQueue, message));
      return null;
    });

    Future<Long> count = executor.submit(() ->
        archiveResponses.limit(numberOfMessages).count());

    return count.get(appTimeoutMillis, TimeUnit.MILLISECONDS);

  }

}
