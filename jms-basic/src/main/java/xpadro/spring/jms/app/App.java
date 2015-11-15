package xpadro.spring.jms.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import xpadro.spring.jms.util.StreamListener;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
public class App {

  @Autowired
  @Qualifier("jmsTemplate")
  private JmsTemplate jmsTemplate;

  @Autowired
  @Qualifier("connectionFactory")
  private ConnectionFactory connectionFactory;

  @Autowired
  @Qualifier("asyncTestQueue")
  private Queue queue;

  @Autowired
  @Qualifier("asyncTestQueue2")
  private Queue queue2;

  @Autowired
  @Qualifier("sourceStream")
  private Supplier<Stream<String>> sourceStream;

  public long testTopicSending() throws Exception {
    long numberOfMessages = sourceStream.get().count();
    Connection con = connectionFactory.createConnection();
    con.start();
    ExecutorService executor = Executors.newCachedThreadPool();
    Stream<Message> receiver = StreamListener.listeningOn(con, queue).start();
    Stream<Message> receiver2 = StreamListener.listeningOn(con, queue2).start();

    executor.submit(() -> {
      sourceStream.get().forEach(message -> jmsTemplate.convertAndSend(queue, message));
      return null;
    });

    executor.submit(() -> {
      receiver.limit(numberOfMessages).forEach(message ->
          jmsTemplate.convertAndSend(queue2, message));
      return null;
    });

    // Count the non-null messages at the end of the chain.
    Future<Long> consumerFuture = executor.submit(() -> {
      long[] count = new long[1];
      receiver2.limit(numberOfMessages).forEach(message -> {
        if (message != null) {
          count[0]++;
        }
      });
      return count[0];
    });
    Long result = consumerFuture.get(1, TimeUnit.MINUTES);
    executor.shutdown();
    con.close();
    return result;

  }


}
