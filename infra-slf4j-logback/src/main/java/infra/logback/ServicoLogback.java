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
import infra.exception.assertions.controlstate.design.UnsupportedException;
import infra.exception.assertions.controlstate.design.UnsupportedReentrantException;
import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Inicializa o mecanismo de logger do framework SLF4J sobre Logback. Ajusta os
 * demais frameworks de logger recorrerem ao SLF4J e imprimirem mensagens pelo
 * Logback.
 * <p>
 * Se existe um diretório 'config' (em relação ao diretório de execução dado
 * pela propriedade 'user.dir') com um arquivo 'logback.cfg.xml', então esta
 * configuração substituirá a configuração padrão do logback.
 * <p>
 * Caso contrário, mantém o comportamento padrão de configuração conforme
 * descrito no manual do Logback. Ou seja, se existe(m) arquivo(s) de
 * configuração no classpath, ou se o arquivo por indicado através da property
 * 'logback.configurationFile', então mantém o comportamento do Logback para
 * carregar as configurações.
 *
 * @author Daniel Felix Ferber
 *
 */
public class ServicoLogback {
	public static enum MotivoConfiguracaoLogback implements Motivo {
		ARQUIVO_CONFIG("Falha ao abrir arquivo de configuração."),
		CONFIGURACAO("Falha ao interpretar configuração."), ;
		private String mensagem;

		private MotivoConfiguracaoLogback(String mensagem) { this.mensagem = mensagem; }
		@Override public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Ler configuração de logger."; }
	}

	/** Se a instalação utiliza uma configuração do classpath. */
	private static boolean usandoConfiguracaoClasspath = false;
	/** @return Se a instalação utiliza uma configuração do classpath. */
	public static boolean isUsandoConfiguracaoEspecifica() { return ServicoLogback.usandoConfiguracaoEspecifica; }

	/** Se a instalação encontrou e leu um arquivo externo ao invés do padrão logback. */
	private static boolean usandoConfiguracaoEspecifica = false;
	/** @return Se a instalação encontrou e leu um arquivo externo ao invés do padrão logback. */
	public static boolean isUsandoConfiguracaoClasspath() { 	return ServicoLogback.usandoConfiguracaoClasspath; }

	/** Se a instalação encontrou e leu um arquivo especificado através da propriedade de sistema. */
	private static boolean usandoConfiguracaoProperty = false;
	/** @return Se a instalação encontrou e leu um arquivo especificado através da propriedade de sistema. */
	public static boolean isUsandoConfiguracaoProperty() { return ServicoLogback.usandoConfiguracaoProperty; }


	private final static Lock lockInstalacao = new ReentrantLock();

	public static void reconfigurar(File arqConfig) throws MotivoException {
		if (! ServicoLogback.lockInstalacao.tryLock()) throw new UnsupportedReentrantException();
		try {
			if (! ServicoLogback.instalado) ServicoLogback.instalar();

			/*
			 * Apagar configuração padrão que eventualmente criada anteriormente.
			 */
			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			lc.reset();

			/*
			 * Ler e aplicar configuração do arquivo XML.
			 */
			InputStream is = null;
			try {
				is = new FileInputStream(arqConfig);
			} catch (Exception e) {
				throw new MotivoException(e, MotivoConfiguracaoLogback.ARQUIVO_CONFIG);
			}

			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(lc);
			try {
				configurator.doConfigure(is);
			} catch (JoranException je) {
				StatusPrinter.printIfErrorsOccured(lc);
				throw new MotivoException(je, MotivoConfiguracaoLogback.CONFIGURACAO);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					UnsupportedException.consume(e);
				}
			}

			List<Status> list = configurator.getStatusManager().getCopyOfStatusList();
			StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
			for (Status status : list) {
				if (status.getLevel() == Status.ERROR) {
					if (status.getThrowable() != null) {
						throw new MotivoException(status.getThrowable(), MotivoConfiguracaoLogback.CONFIGURACAO);
					}
					throw new MotivoException(new Exception(status.getMessage()), MotivoConfiguracaoLogback.CONFIGURACAO);
				}
			}

			ServicoLogback.usandoConfiguracaoClasspath = false;
			ServicoLogback.usandoConfiguracaoProperty = false;
			ServicoLogback.usandoConfiguracaoEspecifica = true;
		} finally {
			ServicoLogback.lockInstalacao.unlock();
		}
	}

	private static boolean instalado = false;
	public static void instalar() {
		if (! ServicoLogback.lockInstalacao.tryLock()) throw new UnsupportedReentrantException();
		try {
			if (ServicoLogback.instalado) throw new UnsupportedCallOrderException();

			/*
			 * Instalar ponte entre JUL (logger padrão do java) e SLF4J. Antes, reinicia toda a configuração do LogManager do JUL.
			 * Caso contrário, sempre haverá um ConsoleHandler padrão que gerará mensagens repetidas indesejadas no stdout
			 * quando for acionado um logger do JUL.
			 */
			java.util.logging.LogManager.getLogManager().reset();
			java.util.logging.LogManager.getLogManager().getLogger("").setLevel(java.util.logging.Level.ALL);
			SLF4JBridgeHandler.install();

			ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			root.setLevel(Level.ALL);

			LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

			PatternLayout pl = new PatternLayout();
	      pl.setContext(lc);

			ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
			appender.setContext(lc);
			appender.setOutputStream(System.err);
			appender.setLayout(pl);
			appender.start();

			ServicoLogback.instalado = true;

			/*
			 * Verifica se existem arquivos de configuração no classpath de acordo com a convenção do logback.
			 * Se existir, então não faz nada e deixa o logback realizar a configuração mais tarde de acordo com sua convenção.
			 */
			if (System.getProperty("logback.configurationFile") != null) {
				ServicoLogback.usandoConfiguracaoProperty = true;
			} else 	if ((ServicoLogback.class.getResource("/logback.groovy") != null) ||
					(ServicoLogback.class.getResource("/logback-test.xml") != null) ||
					(ServicoLogback.class.getResource("/logback.xml") != null)) {
				ServicoLogback.usandoConfiguracaoClasspath = true;
			}

		} finally {
			ServicoLogback.lockInstalacao.unlock();
		}
	}
}
