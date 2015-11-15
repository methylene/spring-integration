package xpadro.spring.jms.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import xpadro.spring.jms.app.App;
import xpadro.spring.jms.test.configuration.MockConfiguration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@ContextConfiguration(classes = MockConfiguration.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TestQueueChain {

  @Autowired
  private App app;

  @Test
  public void testTopicSending() throws Exception {
    assertThat(app.testTopicSending(), is(MockConfiguration.NUM_MESSAGES));
  }

}
