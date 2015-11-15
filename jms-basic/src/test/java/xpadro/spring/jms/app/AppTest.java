package xpadro.spring.jms.app;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import xpadro.spring.jms.test.configuration.MockConfiguration;
import xpadro.spring.jms.test.service.MockArchiveService;
import xpadro.spring.jms.test.service.MockConversionService;

import javax.jms.Connection;
import javax.jms.JMSException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@ContextConfiguration(classes = MockConfiguration.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class AppTest {

  @Autowired
  private Connection connection;

  @Autowired
  private ExecutorService executor;

  @Autowired
  private MockConversionService conversionService;

  @Autowired
  private MockArchiveService archiveService;

  @Autowired
  private App app;

  @Autowired
  @Qualifier("numberOfMessages")
  private Long numberOfMessages;

  private Future<Long> appCount;
  private Future<Long> archiveCount;
  private Future<Long> conversionCount;

  @Test
  public void testApp() throws Exception {
    assertThat(numberOfMessages, is(MockConfiguration.NUM_MESSAGES));
    assertThat(appCount.get(), is(MockConfiguration.NUM_MESSAGES));
    archiveService.setStopped();
    conversionService.setStopped();
    assertThat(conversionCount.get(), is(MockConfiguration.NUM_MESSAGES));
    assertThat(archiveCount.get(), is(MockConfiguration.NUM_MESSAGES));
  }

  @Before
  public void before() throws JMSException {
    connection.start();
    conversionCount = executor.submit(conversionService);
    archiveCount = executor.submit(archiveService);
    appCount = executor.submit(app);
  }

  @After
  public void after() throws JMSException {
    connection.stop();
    executor.shutdown();
  }

}
