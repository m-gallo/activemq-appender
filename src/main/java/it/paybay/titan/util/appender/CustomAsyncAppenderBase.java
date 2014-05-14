package it.paybay.titan.util.appender;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

public class CustomAsyncAppenderBase<E> extends UnsynchronizedAppenderBase<E>
		implements AppenderAttachable<E> {

	public static final int DEFAULT_MIN_LOG_EVENT_SIZE = 10;
	public static final int DEFAULT_MAX_FLUSH_TIME = 1000;

	private int maxFlushTime = DEFAULT_MAX_FLUSH_TIME;
	private int queueSize;
	private int minLogEventSize;
	private int appenderCount;
	private BlockingQueue<E> blockingQueue;
	private AppenderAttachableImpl<E> attachedAppenders = new AppenderAttachableImpl<E>();
	private Worker worker = new Worker();

	@Override
	public void start() {

		if (appenderCount == 0) {
			addError("No attached appenders found.");
			return;
		}

		if (queueSize < 1) {
			addError("Invalid queue Size [" + queueSize + "]");
			return;
		}

		if (minLogEventSize == 0) {
			addInfo("Setting minLogEventSize to [" + DEFAULT_MIN_LOG_EVENT_SIZE
					+ "]");
			minLogEventSize = DEFAULT_MIN_LOG_EVENT_SIZE;
		}

		blockingQueue = new ArrayBlockingQueue<E>(queueSize);
		worker.setDaemon(true);
		worker.start();

		super.start();
	}

	@Override
	public void stop() {
		if (!isStarted())
			return;

		super.stop();

		worker.interrupt();
		try {
			worker.join(maxFlushTime);

			if (worker.isAlive()) {
				addWarn("Max queue flush timeout (" + maxFlushTime
						+ " ms) exceeded. Approximately "
						+ blockingQueue.size()
						+ " queued events were possibly discarded.");
			} else {
				addInfo("Queue flush finished successfully within timeout.");
			}

		} catch (InterruptedException e) {
			addError("Failed to join worker thread. " + blockingQueue.size()
					+ " queued events may be discarded.", e);
		}
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public void addAppender(Appender<E> newAppender) {
		if (appenderCount == 0) {
			appenderCount++;
			addInfo("Attaching appender named [" + newAppender.getName()
					+ "] to CustomAsyncAppender.");
			attachedAppenders.addAppender(newAppender);
		} else {
			addWarn("One and only one appender may be attached to CustomAsyncAppender.");
			addWarn("Ignoring additional appender named ["
					+ newAppender.getName() + "]");
		}

	}

	public Iterator<Appender<E>> iteratorForAppenders() {
		return attachedAppenders.iteratorForAppenders();
	}

	public Appender<E> getAppender(String name) {
		return attachedAppenders.getAppender(name);
	}

	public boolean isAttached(Appender<E> eAppender) {
		return attachedAppenders.isAttached(eAppender);
	}

	public void detachAndStopAllAppenders() {
		attachedAppenders.detachAndStopAllAppenders();
	}

	public boolean detachAppender(Appender<E> eAppender) {
		return attachedAppenders.detachAppender(eAppender);
	}

	public boolean detachAppender(String name) {
		return attachedAppenders.detachAppender(name);
	}

	class Worker extends Thread {

		private long backoff;
		private int counter;

		@Override
		public void run() {
			CustomAsyncAppenderBase<E> parent = CustomAsyncAppenderBase.this;
			AppenderAttachableImpl<E> attachedAppenders = parent.attachedAppenders;

			backoff = 1000;
			counter = 0;

			while (parent.isStarted()) {

				try {
					if (parent.blockingQueue.size() < parent.minLogEventSize) {
						backoff *= ++counter;
						System.out.println("Waiting:" + backoff);
						Thread.sleep(backoff);
					}

					List<E> elements = new LinkedList<E>();
					parent.blockingQueue.drainTo(elements, 100);

					attachedAppenders.appendLoopOnAppenders((E) elements);

				} catch (InterruptedException e) {
					break;
				}
			}

			addInfo("Worker thread will flush remaining events before exiting. ");

			while (parent.blockingQueue.size() > 0) {
				List<E> elements = new LinkedList<E>();
				parent.blockingQueue.drainTo(elements, 100);
				attachedAppenders.appendLoopOnAppenders((E) elements);
			}

			attachedAppenders.detachAndStopAllAppenders();

		}
	}

	@Override
	protected void append(E eventObject) {
		try {

			blockingQueue.put(eventObject);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public int getMinLogEventSize() {
		return minLogEventSize;
	}

	public void setMinLogEventSize(int minLogEventSize) {
		this.minLogEventSize = minLogEventSize;
	}

}
