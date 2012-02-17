package infra.logback;

import org.junit.Assert;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 *  A dummy appender that just remembers the last event. It allows to test if the log statement really redirects the message to a logback logger.
 */
class DummyAppender extends  UnsynchronizedAppenderBase<ILoggingEvent> {
	public String lastMessage;
	public Level lastLevel;
	private final String loggerName;

	public DummyAppender(String loggerName) {
		super();
		this.loggerName = loggerName;
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		Assert.assertEquals(loggerName, eventObject.getLoggerName());
		lastMessage = eventObject.getMessage();
		lastLevel =  eventObject.getLevel();
	}
}