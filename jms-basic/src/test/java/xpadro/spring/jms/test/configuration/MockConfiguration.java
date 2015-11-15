package xpadro.spring.jms.test.configuration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Configuration
@ComponentScan({"xpadro.spring.jms.app"})
public class MockConfiguration {

  public static final long NUM_MESSAGES = 10;

  @Bean(name = "connectionFactory")
  public ConnectionFactory getConnectionFactory() {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    factory.setBrokerURL("vm://embedded?broker.persistent=false&broker.useJmx=false");
    return new CachingConnectionFactory(factory);
  }

  @Bean(name = "jmsTemplate")
  public JmsTemplate getJmsTemplate() {
    JmsTemplate jmsTemplate = new JmsTemplate();
    jmsTemplate.setDefaultDestination(getAsyncTestQueue());
    jmsTemplate.setConnectionFactory(getConnectionFactory());
    return jmsTemplate;
  }

  @Bean(name = "asyncTestQueue")
  public Queue getAsyncTestQueue() {
    return new ActiveMQQueue("test.async.queue");
  }

  @Bean(name = "asyncTestQueue2")
  public Queue getAsyncTestQueue2() {
    return new ActiveMQQueue("test.async.queue2");
  }

  @Bean(name = "sourceStream")
  public Supplier<Stream<String>> getSourceStream() {
    return () -> LongStream.range(0, NUM_MESSAGES).boxed().map(i -> "" + i);
  }

}
