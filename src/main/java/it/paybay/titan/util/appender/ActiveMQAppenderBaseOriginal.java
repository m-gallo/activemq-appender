package it.paybay.titan.util.appender;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

import ch.qos.logback.core.AppenderBase;

public abstract class ActiveMQAppenderBaseOriginal<E> extends AppenderBase<E> {

	protected String username;
	protected String password;
	protected String brokerUrl;
	protected String queueName;
	
	protected Connection connection;
	protected Session session;
	protected MessageProducer producer;
	protected Destination destination;
		
	public String getQueueName() {
		return queueName;
	}


	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getBrokerUrl() {
		return brokerUrl;
	}


	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	@Override
	public void start() {
		addInfo(String.format("Creating ActiveMQConnectionFactory with brokerUrL=%s, username=%s and password=%s", brokerUrl, username, password));
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		
		if (username != null) {
			connectionFactory.setUserName(username);
			connectionFactory.setPassword(password);
		}
		
		connectionFactory.setBrokerURL(brokerUrl);
		try {
			connection = connectionFactory.createQueueConnection();
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			destination = session.createQueue(queueName);
			
			producer = session.createProducer(destination);
			
			connection.start();
		} catch (JMSException ex) {
			addError("Cannot connect to the broker:", ex);
			ex.printStackTrace();
		}
		
		
		super.start();
	}
}
