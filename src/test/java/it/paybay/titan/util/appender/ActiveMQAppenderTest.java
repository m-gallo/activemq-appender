package it.paybay.titan.util.appender;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ObjectMessage;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import it.paybay.titan.util.appender.ActiveMQAppender;
import static org.junit.Assert.*;

public class ActiveMQAppenderTest {

	private static final String BROKER_URL = "tcp://localhost:61626";
	private static final String QUEUE_NAME = "test";

	private LoggerContext context = new LoggerContext();
	private Logger logger;
	private ActiveMQAppender activeMQAppender;
	private BrokerService broker;

	@Before
	public void setUp() throws Exception {
		broker = new BrokerService();
		broker.addConnector(BROKER_URL);
		broker.setPersistent(false);
		broker.start();

		context.setName("default");
		logger = context.getLogger("root");
		logger.setLevel(Level.INFO);
		activeMQAppender = new ActiveMQAppender();
		activeMQAppender.setName("activeMQ");
		activeMQAppender.setContext(context);
		activeMQAppender.setQueueName(QUEUE_NAME);
		
		activeMQAppender.setInitialContextFactoryName("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		activeMQAppender.setProviderURL(BROKER_URL);
		/* 
		 * activeMQAppender.setBrokerUrl(BROKER_URL);
		 */
		
		activeMQAppender.start();
	}
	
	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	public void sendingLoggingEventTest() throws Exception {
		ILoggingEvent event = createLoggingEvent("Test Message", Level.INFO);
		activeMQAppender.append(event);

		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueBrowser queueBrowser = session.createBrowser(session.createQueue(QUEUE_NAME));
		Enumeration<?> enumeration = queueBrowser.getEnumeration();
		ObjectMessage message = (ObjectMessage) enumeration.nextElement();
		ILoggingEvent receivedEvent = (ILoggingEvent) message.getObject();

		assertEquals("Test Message", receivedEvent.getMessage());
		assertEquals(Level.INFO, receivedEvent.getLevel());

	}

	private LoggingEvent createLoggingEvent(String message, Level level) {
		return new LoggingEvent(this.getClass().getName(), logger, level,
				message, null, new Object[] {});
	}

}
