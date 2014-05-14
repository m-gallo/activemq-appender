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
import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import it.paybay.titan.util.appender.ActiveMQAppender;
import static org.junit.Assert.*;

public class ActiveMQAppenderWithAsyncAppenderTest {

	private static final String BROKER_URL = "tcp://localhost:61626";
	private static final String QUEUE_NAME = "test";

	private LoggerContext context = new LoggerContext();
	private Logger logger;
	private AsyncAppender asyncAppender;
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
//		activeMQAppender.setBrokerUrl(BROKER_URL);
		activeMQAppender.start();
		
		asyncAppender = new AsyncAppender();
		asyncAppender.setContext(context);
		asyncAppender.addAppender(activeMQAppender);
		asyncAppender.setQueueSize(10);
		asyncAppender.start();
	}
	
	@After
	public void tearDown() throws Exception {
		
	}

	@Test
	@Ignore
	public void sendingLoggingEventTest() throws Exception {
		for (int i = 0; i < 10; i++) {
			ILoggingEvent event = createLoggingEvent("Test Message" + i, Level.ERROR);
			asyncAppender.doAppend(event);
		}
		
		Thread.sleep(20000);
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		QueueBrowser queueBrowser = session.createBrowser(session.createQueue(QUEUE_NAME));
		System.out.println(queueBrowser.getEnumeration().toString());
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
