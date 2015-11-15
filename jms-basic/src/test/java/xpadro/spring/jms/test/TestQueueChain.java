package xpadro.spring.jms.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import xpadro.spring.jms.receiver.StreamListener;

import javax.jms.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@ContextConfiguration(locations = {
    "/xpadro/spring/jms/config/jms-config.xml",
    "/xpadro/spring/jms/config/app-config.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestQueueChain {

  public static final int TOTAL_MESSAGES = 100;

  @Autowired
  @Qualifier("jmsTemplate")
  private JmsTemplate jmsTemplate;

  @Autowired
  private ConnectionFactory connectionFactory;

  @Autowired
  @Qualifier("asyncTestQueue")
  private Queue queue;

  @Autowired
  @Qualifier("asyncTestQueue2")
  private Queue queue2;

  private Stream<String> sourceStream() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return IntStream.range(0, TOTAL_MESSAGES).boxed().map(i -> "" + i);
  }

  @Test
  public void testTopicSending() throws Exception {
    Connection con = connectionFactory.createConnection();
    con.start();
    ExecutorService executor = Executors.newCachedThreadPool();
    Stream<Message> receiver = StreamListener.listeningOn(con, queue).start();
    Stream<Message> receiver2 = StreamListener.listeningOn(con, queue2).start();

    executor.submit(() -> {
      sourceStream().forEach(message -> jmsTemplate.convertAndSend(queue, message));
      return null;
    });

    executor.submit(() -> {
      receiver.limit(TOTAL_MESSAGES).forEach(message ->
          jmsTemplate.convertAndSend(queue2, message));
      return null;
    });

    // Count the non-null messages at the end of the chain.
    Future<Long> consumerFuture = executor.submit(() -> {
      long[] count = new long[1];
      receiver2.limit(TOTAL_MESSAGES).forEach(message -> {
        if (message != null) {
          count[0]++;
        }
      });
      return count[0];
    });
    assertThat((long) TOTAL_MESSAGES, is(consumerFuture.get(1, TimeUnit.MINUTES)));
    executor.shutdown();
    con.close();

  }

//  private StreamListener createDynamicReceiver(Connection con, Queue queue) throws JMSException {
//    Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
//    MessageConsumer consumer = session.createConsumer(queue);
//    StreamListener listener = new StreamListener();
//    consumer.setMessageListener(listener);
//    return listener;
//  }
}
