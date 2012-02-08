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

import infra.exception.assertions.controlstate.design.UnsupportedCallOrderException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;



public class ServicoLoggerTest {

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
		Assert.assertFalse(ServicoLogback.isInstalado());
		ServicoLogback.instalar();
		Assert.assertTrue(ServicoLogback.isInstalado());
		((Logger) LoggerFactory.getLogger("c")).addAppender(ServicoLoggerTest.appC);
		((Logger) LoggerFactory.getLogger("d")).addAppender(ServicoLoggerTest.appD);
		((Logger) LoggerFactory.getLogger("e")).addAppender(ServicoLoggerTest.appE);
		((Logger) LoggerFactory.getLogger("f")).addAppender(ServicoLoggerTest.appF);
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		ServicoLoggerTest.appC.setContext(lc);
		ServicoLoggerTest.appD.setContext(lc);
		ServicoLoggerTest.appE.setContext(lc);
		ServicoLoggerTest.appF.setContext(lc);
		ServicoLoggerTest.appC.start();
		ServicoLoggerTest.appD.start();
		ServicoLoggerTest.appE.start();
		ServicoLoggerTest.appF.start();
	}

	@Test
	public void a() {
		Assert.assertFalse(ServicoLogback.isUsandoConfiguracaoClasspath());
		Assert.assertFalse(ServicoLogback.isUsandoConfiguracaoProperty());
		Assert.assertFalse(ServicoLogback.isUsandoConfiguracaoEspecifica());
	}

	@Test(expected=UnsupportedCallOrderException.class)
	public void b() {
		ServicoLogback.instalar();
	}

	@Test
	public void c() {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("c");
		String msg = "Teste C";
		logger.info(msg);
		Assert.assertEquals(msg, ServicoLoggerTest.appC.lastMessage);
	}

	@Test
	public void d() {
		org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("d");
		String msg = "Teste D";
		logger.info(msg);
		Assert.assertEquals(msg, ServicoLoggerTest.appD.lastMessage);
	}

	@Test
	public void e() {
		org.apache.log4j.Logger logger = org.apache.log4j.LogManager.getLogger("e");
		String msg = "Teste E";
		logger.info(msg);
		Assert.assertEquals(msg, ServicoLoggerTest.appE.lastMessage);
	}

	@Test
	public void f() {
		org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog("f");
		String msg = "Teste F";
		log.info(msg);
		Assert.assertEquals(msg, ServicoLoggerTest.appF.lastMessage);
	}
}
