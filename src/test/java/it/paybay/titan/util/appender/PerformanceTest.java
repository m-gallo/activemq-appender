package it.paybay.titan.util.appender;

import static org.junit.Assert.assertEquals;

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

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.JMSQueueAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class PerformanceTest {

	private static final String BROKER_URL = "tcp://localhost:61626";
	private static final String QUEUE_NAME = "test";

	private LoggerContext context = new LoggerContext();
	private Logger logger;
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
	}
	
	@After
	public void tearDown() throws Exception {
		broker.stop();
	}

	@Test
	public void testWithSingleJMSAppender() throws Exception {
		JMSQueueAppender appender = new JMSQueueAppender();
		appender.setName("JMSQueueAppender");
		appender.setContext(context);
		appender.setInitialContextFactoryName("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		appender.setProviderURL(BROKER_URL);
		appender.setQueueBindingName(QUEUE_NAME);
		
		ILoggingEvent event = createLoggingEvent("Test Message", Level.INFO);
		long start = System.nanoTime();
		for (int i = 0; i < 100; i++)
			appender.append(event);
		long stop = System.nanoTime();
		
		System.out.println("JMS Appender");
		System.out.println("Elapsed:" + (stop-start) / 1000 + " ms");
	}
	
	@Test
	public void testWithProvidedAsyncAppender() throws Exception {
		JMSQueueAppender appender = new JMSQueueAppender();
		appender.setName("JMSQueueAppender");
		appender.setContext(context);
		appender.setInitialContextFactoryName("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		appender.setProviderURL(BROKER_URL);
		appender.setQueueBindingName(QUEUE_NAME);
		
		AsyncAppender async = new AsyncAppender();
		async.setName("AsyncAppender");
		async.setContext(context);
		async.addAppender(appender);
		
		ILoggingEvent event = createLoggingEvent("Test Message", Level.INFO);
		long start = System.nanoTime();
		for (int i = 0; i < 100; i++)
			async.doAppend(event);
		long stop = System.nanoTime();
		
		System.out.println("Async Appender");
		System.out.println("Elapsed:" + (stop-start) / 1000 + " ms");
	}
	
	private LoggingEvent createLoggingEvent(String message, Level level) {
		return new LoggingEvent(this.getClass().getName(), logger, level,
				message, null, new Object[] {});
	}

	
}
