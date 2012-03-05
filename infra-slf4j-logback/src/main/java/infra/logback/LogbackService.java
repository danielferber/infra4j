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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

/**
 * Initializes the SLF4J logging mechanism over Logback and configures Logback.
 * When installing logback, adjusts the other supported logging frameworks to
 * redirect messages to Logback.
 * <p>
 * The {@link LogbackService#install()} method preserves the default behavior as described
 * in the Logback manual. If there are one of the supported configuration files
 * in the classpath, or if there is a configuration file specified by the
 * 'logback.configurationFile' property, then this configuration is applied.
 * However, the Logback configuration may be reapplied from another file
 * according to the application logic by calling the {@link #reconfigure(File)} method..
 *
 * @author Daniel Felix Ferber
 *
 */
public class LogbackService {
	/** Se a instalação utiliza uma configuração do classpath. */
	private static boolean usandoConfiguracaoClasspath = false;
	/** @return Se a instalação utiliza uma configuração do classpath. */
	public static boolean isUsandoConfiguracaoClasspath() { 	return LogbackService.usandoConfiguracaoClasspath; }

	/** Se a instalação encontrou e leu um arquivo externo ao invés do padrão logback. */
	private static boolean usandoConfiguracaoEspecifica = false;
	/** @return Se a instalação encontrou e leu um arquivo externo ao invés do padrão logback. */
	public static boolean isUsandoConfiguracaoEspecifica() { return LogbackService.usandoConfiguracaoEspecifica; }

	/** Se a instalação encontrou e leu um arquivo especificado através da propriedade de sistema. */
	private static boolean usandoConfiguracaoProperty = false;
	/** @return Se a instalação encontrou e leu um arquivo especificado através da propriedade de sistema. */
	public static boolean isUsandoConfiguracaoProperty() { return LogbackService.usandoConfiguracaoProperty; }

	/** Se a configuração do Logback sobre foi realizada. */
	private static boolean instalado = false;
	/** @param Se a configuração do Logback sobre foi realizada. */
	public static boolean isInstalado() { return LogbackService.instalado; }

	/** Lock que previne tentativas de configurações simultâneas por threads diferentes. */
	private final static Lock lockInstalacao = new ReentrantLock();

	public static void reconfigure(File arqConfig) throws LogbackReconfigureException {
		LogbackService.reconfigure(arqConfig, null);
	}

	public static void reconfigure(File arqConfig, Properties properties) throws LogbackReconfigureException {
		if (! LogbackService.lockInstalacao.tryLock()) throw new LogbackReconfigureException(LogbackReconfigureException.Reason.REENTRANT_RECONFIGURE);
		try {
			if (! LogbackService.instalado) LogbackService.install();

			/*
			 * Apagar configuração criada anteriormente.
			 */
			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			lc.reset();
			List<Status> originalList = lc.getStatusManager().getCopyOfStatusList();

			/*
			 * Ler e aplicar configuração do arquivo XML.
			 */
			InputStream is = null;
			try {
				is = new FileInputStream(arqConfig);
			} catch (Exception e) {
				throw new LogbackReconfigureException(LogbackReconfigureException.Reason.ARQUIVO_CONFIG, e);
			}

			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			if (properties != null) {
				for (String key : properties.stringPropertyNames()) {
					lc.putProperty(key, properties.getProperty(key));
				}
			}
			try {
				configurator.doConfigure(is);
			} catch (JoranException e) {
				throw new LogbackReconfigureException(LogbackReconfigureException.Reason.CONFIGURACAO, e, lc);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			List<Status> list = configurator.getStatusManager().getCopyOfStatusList();
			for (Status status : list.subList(originalList.size(), list.size())) {
				if (status.getLevel() == Status.ERROR) {
					if (status.getThrowable() != null) {
						throw new LogbackReconfigureException(LogbackReconfigureException.Reason.CONFIGURACAO, status.getThrowable());
					}
					throw new LogbackReconfigureException(LogbackReconfigureException.Reason.CONFIGURACAO);
				}
			}

			LogbackService.usandoConfiguracaoClasspath = false;
			LogbackService.usandoConfiguracaoProperty = false;
			LogbackService.usandoConfiguracaoEspecifica = true;
		} finally {
			LogbackService.lockInstalacao.unlock();
		}
	}

	public static void install() {
		if (! LogbackService.lockInstalacao.tryLock()) throw new LogbackInstallException(LogbackInstallException.Reason.REENTRANT_INSTALL);
		try {
			if (LogbackService.instalado) throw new LogbackInstallException(LogbackInstallException.Reason.DUPLICATED_INSTALL);

			/*
			 * Instalar ponte entre JUL (logger padrão do java) e SLF4J. Antes, reinicia toda a configuração do LogManager do JUL.
			 * Caso contrário, sempre haverá um ConsoleHandler padrão que gerará mensagens repetidas indesejadas no stdout
			 * quando for acionado um logger do JUL.
			 */
			try {
				java.util.logging.LogManager.getLogManager().reset();
				java.util.logging.LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.ALL);
			} catch (SecurityException e) {
				throw new LogbackInstallException(LogbackInstallException.Reason.MISSING_PERMISSIONS, e);
			}
			SLF4JBridgeHandler.install();

			/*
			 * Não é preciso fazer mais nada, uma vez que o Logback já cria por padrão um appender no root logger direcionado para Stdout.
			 * Isto é diferente do log4j, que por padrão não escrevia as mensagens.
			 */

			/*
			 * Verifica se existem arquivos de configuração no classpath de acordo com a convenção do logback.
			 * Se existir, então não faz nada e deixa o logback realizar a configuração mais tarde de acordo com sua convenção.
			 */
			if (System.getProperty("logback.configurationFile") != null) {
				LogbackService.usandoConfiguracaoProperty = true;
			} else if ((LogbackService.class.getResource("/logback.groovy") != null) ||
					(LogbackService.class.getResource("/logback-test.xml") != null) ||
					(LogbackService.class.getResource("/logback.xml") != null)) {
				LogbackService.usandoConfiguracaoClasspath = true;
			}

			LogbackService.instalado = true;
		} finally {
			LogbackService.lockInstalacao.unlock();
		}
	}
}