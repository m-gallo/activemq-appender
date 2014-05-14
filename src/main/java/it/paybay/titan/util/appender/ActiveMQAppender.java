package it.paybay.titan.util.appender;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.PreSerializationTransformer;

public class ActiveMQAppender extends ActiveMQAppenderBase<ILoggingEvent> {

	private PreSerializationTransformer<ILoggingEvent> transfomer = new LoggingEventPreSerializationTransformer();
	
	@Override
	protected void append(ILoggingEvent event) {
		System.out.println("SCRIVO");
		try {
			ObjectMessage objectMessage = session.createObjectMessage();
			objectMessage.setObject(transfomer.transform(event));
			producer.send(destination, objectMessage);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
