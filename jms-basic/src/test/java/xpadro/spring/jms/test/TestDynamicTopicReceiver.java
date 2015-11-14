package xpadro.spring.jms.test;

import static org.junit.Assert.assertEquals;

import javax.jms.*;

import org.apache.activemq.command.ActiveMQQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import xpadro.spring.jms.model.Notification;
import xpadro.spring.jms.producer.Producer;
import xpadro.spring.jms.receiver.DynamicTopicReceiver;
import xpadro.spring.jms.receiver.NotificationRegistry;

@ContextConfiguration(locations = {
			"/xpadro/spring/jms/config/jms-config.xml", 
			"/xpadro/spring/jms/config/app-config.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDynamicTopicReceiver {
	
	@Autowired
	private Producer producer;
	
	@Autowired
	private NotificationRegistry registry;
	
	@Autowired
	private ConnectionFactory connectionFactory;
	
	@Autowired
	private Topic destination;

  @Autowired
  private Queue queue;

	@Before
	public void prepareTest() {
		registry.clear();
	}
	
	@Test
	public void testTopicSending() throws InterruptedException, JMSException {
		Notification notification = new Notification("3", "this is a topic");
		
		Connection con = connectionFactory.createConnection();
		con.start();
		createDynamicReceiver(con);
		producer.convertAndSendMessage(notification);
		
		Thread.sleep(2000);
		con.close();
		
		assertEquals(1, registry.getReceivedNotifications().size());
		assertEquals("this is a topic", registry.getReceivedNotifications().get(0).getMessage());
	}
	
	
	private void createDynamicReceiver(Connection con) throws JMSException {
		Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageConsumer consumer = session.createConsumer(queue);
		MessageListener listener = new DynamicTopicReceiver(registry);
		consumer.setMessageListener(listener);
	}
}
