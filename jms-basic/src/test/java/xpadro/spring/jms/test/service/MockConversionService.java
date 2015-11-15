package xpadro.spring.jms.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import xpadro.spring.jms.util.QueueListener;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class MockConversionService implements Callable<Long> {

  private Logger log = LoggerFactory.getLogger(getClass());

  private boolean stopped;

  @Autowired
  private JmsTemplate jmsTemplate;

  @Autowired
  private Connection connection;

  @Autowired
  private ExecutorService executor;

  @Autowired
  @Qualifier("conversionRequest")
  private Queue conversionRequestQueue;

  @Autowired
  @Qualifier("conversionResponse")
  private Queue conversionResponseQueue;

  @Override
  public Long call() throws Exception {
    Stream<Message> conversionRequests = QueueListener.connect(connection, conversionRequestQueue)
        .timeout(100, TimeUnit.MILLISECONDS).start();
    Iterator<Message> iterator = conversionRequests.iterator();
    long numProcessed = 0;
    while (iterator.hasNext() && !stopped) {
      Message message = iterator.next();
      if (message != null) {
        jmsTemplate.convertAndSend(conversionResponseQueue, message);
        numProcessed++;
      }
    }
    return numProcessed;
  }

  public void setStopped() {
    stopped = true;
  }

}
