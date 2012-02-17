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

import infra.logback.InstallTest.Appender;

import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/*
 * Tests the Logback install utility method:
 * - Might be called only one.
 * - Sucessfully redirects log from supported logger APIs.
 */
public class ReconfigurelTest {
	@BeforeClass
	public static void inicio() {
		if (! ServicoLogback.isInstalado()) {
			ServicoLogback.instalar();
		}
		Assert.assertTrue(ServicoLogback.isInstalado());
	}

	/**
	 * Configuration file does not exist.
	 */
	@Test
	public void a() {
		try {
			ServicoLogback.reconfigurar(new File("config/notFound.xml"));
		} catch (LogbackReconfigureException e) {
			Assert.assertEquals(LogbackReconfigureException.Reason.ARQUIVO_CONFIG, e.reason);
		}
	}

	/**
	 * Configuration file with invalid XML.
	 */
	@Test()
	public void b() {
		try {
			ServicoLogback.reconfigurar(new File("config/invalidXml.xml"));
		} catch (LogbackReconfigureException e) {
			Assert.assertEquals(LogbackReconfigureException.Reason.CONFIGURACAO, e.reason);
		}
	}

	/**
	 * Configuration file with good XML.
	 */
	@Test()
	public void c() {
		Properties properties = new Properties();
		properties.setProperty("globalLevel", "WARN");
		ServicoLogback.reconfigurar(new File("config/goodXml.xml"), properties);
		Assert.assertTrue(ServicoLogback.isInstalado());
		Assert.assertTrue(ServicoLogback.isUsandoConfiguracaoEspecifica());
		Assert.assertFalse(ServicoLogback.isUsandoConfiguracaoClasspath());
		Assert.assertFalse(ServicoLogback.isUsandoConfiguracaoProperty());
	}

	/**
	 * Test properties injection.
	 */
	@Test()
	public void d() {
		Properties properties = new Properties();
		properties.setProperty("globalLevel", "WARN");
		ServicoLogback.reconfigurar(new File("config/propertiesXml.xml"), properties);

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		final Appender dummyAppender = new Appender("dummy");
		dummyAppender.setContext(lc);
		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(dummyAppender);
		dummyAppender.start();

		Logger logger = LoggerFactory.getLogger("dummy");
		logger.error("yes1");
		Assert.assertEquals("yes1", dummyAppender.lastMessage);
		Assert.assertEquals(Level.ERROR, dummyAppender.lastLevel);
		logger.warn("yes2");
		Assert.assertEquals("yes2", dummyAppender.lastMessage);
		Assert.assertEquals(Level.WARN, dummyAppender.lastLevel);
		logger.info("no1");
		Assert.assertEquals("yes2", dummyAppender.lastMessage);
		Assert.assertEquals(Level.WARN, dummyAppender.lastLevel);
		logger.debug("no2");
		Assert.assertEquals("yes2", dummyAppender.lastMessage);
		Assert.assertEquals(Level.WARN, dummyAppender.lastLevel);
		logger.trace("no3");
		Assert.assertEquals("yes2", dummyAppender.lastMessage);
		Assert.assertEquals(Level.WARN, dummyAppender.lastLevel);
	}

}
