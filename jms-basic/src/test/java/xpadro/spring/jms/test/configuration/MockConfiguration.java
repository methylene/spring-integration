package xpadro.spring.jms.test.configuration;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Configuration
@ComponentScan({"xpadro.spring.jms.app", "xpadro.spring.jms.test.service"})
public class MockConfiguration {

  public static final long NUM_MESSAGES = 10;

  @Bean(name = "connectionFactory")
  public ConnectionFactory getConnectionFactory() {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    factory.setBrokerURL("vm://embedded?broker.persistent=false&broker.useJmx=false");
    return new CachingConnectionFactory(factory);
  }

  @Bean(name = "connection")
  public Connection getConnection() throws JMSException {
    ConnectionFactory connectionFactory = getConnectionFactory();
    return connectionFactory.createConnection();
  }

  @Bean(name = "jmsTemplate")
  public JmsTemplate getJmsTemplate() {
    JmsTemplate jmsTemplate = new JmsTemplate();
//    jmsTemplate.setDefaultDestination(getAsyncTestQueue());
    jmsTemplate.setConnectionFactory(getConnectionFactory());
    return jmsTemplate;
  }

  @Bean(name = "conversionRequest")
  public Queue getConversionRequestQueue() {
    return new ActiveMQQueue("queue.conversion.request");
  }

  @Bean(name = "conversionResponse")
  public Queue getConversionResponseQueue() {
    return new ActiveMQQueue("queue.conversion.response");
  }

  @Bean(name = "archiveRequest")
  public Queue getArchiveRequestQueue() {
    return new ActiveMQQueue("queue.archive.request");
  }

  @Bean(name = "archiveResponse")
  public Queue getArchiveResponseQueue() {
    return new ActiveMQQueue("queue.archive.response");
  }

  @Bean(name = "numberOfMessages")
  public Long getNumberOfMessages() {
    return getSourceStream().get().count();
  }

  @Bean(name = "executorService")
  public ExecutorService getExecutorService() {
    return Executors.newCachedThreadPool();
  }

  @Bean(name = "sourceStream")
  public Supplier<Stream<String>> getSourceStream() {
    return () -> LongStream.range(0, NUM_MESSAGES).boxed().map(i -> "" + i);
  }

  @Bean(name = "appTimeoutMillis")
  public Long getAppTimeoutMillis() {
    return TimeUnit.SECONDS.toMillis(10);
  }

}
