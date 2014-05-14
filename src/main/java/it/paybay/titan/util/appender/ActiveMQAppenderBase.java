package it.paybay.titan.util.appender;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import ch.qos.logback.classic.net.JMSQueueAppender;
import ch.qos.logback.core.net.JMSAppenderBase;

public abstract class ActiveMQAppenderBase<E> extends JMSAppenderBase<E> {

	protected String queueName;
	private Connection connection;
	protected MessageProducer producer;
	protected Session session;
	protected Destination destination;
	
	@Override
	public void start() {
		ConnectionFactory connectionFactory = null;
		
		try {
			Context jndiContext = buildJNDIContext();
			connectionFactory = (ConnectionFactory) lookup(jndiContext, "ConnectionFactory");
			
			
		} catch (NamingException ex) {
			addError("Error JNDI Context - ", ex);
		}
		
		try {
			connection = connectionFactory.createConnection();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = session.createQueue(queueName);
			producer = session.createProducer(destination);
			
		} catch (JMSException ex) {
			addError("JMS Exception - ", ex);
		}
		
		
		
		super.start();
	}
	
	@Override
	public void stop() {
		try {
			producer.close();
			session.close();
			connection.close();
		} catch (JMSException ex) {
			
		}
		super.stop();
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
}
