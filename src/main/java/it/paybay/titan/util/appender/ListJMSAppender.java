package it.paybay.titan.util.appender;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.NamingException;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.JMSAppenderBase;
import ch.qos.logback.core.spi.PreSerializationTransformer;

public class ListJMSAppender extends JMSAppenderBase<List<ILoggingEvent>> {

	protected String queueName;
	private Connection connection;
	protected MessageProducer producer;
	protected Session session;
	protected Destination destination;
	private PreSerializationTransformer<ILoggingEvent> transfomer = new LoggingEventPreSerializationTransformer();
	
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
			addError("JMS Exception - ", ex);
		}
		super.stop();
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	@Override
	protected void append(List<ILoggingEvent> eventObject) {
		try {
			ObjectMessage objectMessage = session.createObjectMessage();
			List<Serializable> serialized = new LinkedList<Serializable>();
			for (ILoggingEvent singleEvent : eventObject) {
				serialized.add(transfomer.transform(singleEvent));
			}
			objectMessage.setObject((Serializable) serialized);
			producer.send(objectMessage);
			
		} catch (JMSException ex) {
			addError("JMS Exception - ", ex);
		}		
	}
}
