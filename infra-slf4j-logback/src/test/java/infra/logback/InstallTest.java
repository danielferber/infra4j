/*
 * Copyright 2012 Daniel Felix Ferber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.logback;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/*
 * Tests the Logback install utility method:
 * - Might be called only one.
 * - Sucessfully redirects log from supported logger APIs.
 */
public class InstallTest {
	/**
	 *  A dummy appender that just remembers the last event. It allows to test if the log statement really redirects the message to a logback logger.
	 */
	static class Appender extends  UnsynchronizedAppenderBase<ILoggingEvent> {
		public String lastMessage;
		public Level lastLevel;
		private final String loggerName;

		public Appender(String loggerName) {
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
	static Appender appC = new Appender("c");
	static Appender appD = new Appender("d");
	static Appender appE = new Appender("e");
	static Appender appF = new Appender("f");

	@BeforeClass
	public static void inicio() {
		if (! LogbackService.isInstalled()) {
			LogbackService.install();
		}
		Assert.assertTrue(LogbackService.isInstalled());
		((Logger) LoggerFactory.getLogger("c")).addAppender(InstallTest.appC);
		((Logger) LoggerFactory.getLogger("d")).addAppender(InstallTest.appD);
		((Logger) LoggerFactory.getLogger("e")).addAppender(InstallTest.appE);
		((Logger) LoggerFactory.getLogger("f")).addAppender(InstallTest.appF);
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		InstallTest.appC.setContext(lc);
		InstallTest.appD.setContext(lc);
		InstallTest.appE.setContext(lc);
		InstallTest.appF.setContext(lc);
		InstallTest.appC.start();
		InstallTest.appD.start();
		InstallTest.appE.start();
		InstallTest.appF.start();
	}

	/**
	 * Default install (without any configuration file on classpath) does not report any special install configuration flag.
	 */
	@SuppressWarnings("static-method")
	@Test
	public void a() {
		Assert.assertFalse(LogbackService.isUsingClasspath());
		Assert.assertFalse(LogbackService.isUsingSystemProperty());
		Assert.assertFalse(LogbackService.isUsingExternalFile());
	}

	/**
	 * Does not allow to install twice.
	 */
	@SuppressWarnings("static-method")
	@Test()
	public void b() {
		try {
			LogbackService.install();
			Assert.fail();
		} catch (LogbackInstallException e) {
			Assert.assertEquals(LogbackInstallException.Reason.DUPLICATED_INSTALL, e.reason);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void c() {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("c");
		String msg = "Teste C";
		logger.info(msg);
		Assert.assertEquals(msg, InstallTest.appC.lastMessage);
	}

	@SuppressWarnings("static-method")
	@Test
	public void d() {
		org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("d");
		String msg = "Teste D";
		logger.info(msg);
		Assert.assertEquals(msg, InstallTest.appD.lastMessage);
	}

	@SuppressWarnings("static-method")
	@Test
	public void e() {
		org.apache.log4j.Logger logger = org.apache.log4j.LogManager.getLogger("e");
		String msg = "Teste E";
		logger.info(msg);
		Assert.assertEquals(msg, InstallTest.appE.lastMessage);
	}

	@SuppressWarnings("static-method")
	@Test
	public void f() {
		org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("f");
		String msg = "Teste F";
		log.info(msg);
		Assert.assertEquals(msg, InstallTest.appF.lastMessage);
	}
}
