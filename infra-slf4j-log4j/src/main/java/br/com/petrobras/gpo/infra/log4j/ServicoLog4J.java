/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.logging.Level;

import org.apache.log4j.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;

import br.com.petrobras.gpo.infra.exception.motivo.MotivoException;

/**
 * Inicializa o mecanismo de logger do framework SLF4J sobre Log4J. 
 * Faz os ajustes para todos os demais frameworks de logger recorrerem ao SLF4J
 * e imprimirem mensagens pelo Log4J.
 * <p>
 * A configuração é obtida através do mecanismo padrão do Log4J (arquivos 'log4j.properties' ou 'log4J.xml') no
 * classpath, ou através da configuração por System Properties, conforme descrito no manual do Log4J.
 * 
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class ServicoLog4J {
	/**
	 * Aplica as configurações de logging à JVM.
	 * 
	 * @throws MotivoException
	 *             {@link MotivoConfiguracaoLogback}.
	 */
	public static void instalar() throws MotivoException {
		/*
		 * Instalar ponte entre JUL (logger padrão do java) e SLF4J. Antes, reinicia toda a configuração do LogManager.
		 * Caso contrário, sempre haverá um ConsoleHandler padrão que gerará mensagens repetidas indesejadas no stdout
		 * quando for acionado um logger do JUL.
		 */
		java.util.logging.LogManager.getLogManager().reset();
		java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
		
		/* 
		 * Instalar ponte entre logger do java e logback.
		 */
		SLF4JBridgeHandler.install();

		/*
		 * Obtém referência do Root Logger apenas com objetivo de forçar uma tentativa de carregar as configurações de
		 * acordo com o padrão Log4J.
		 */
		org.apache.log4j.Logger.getRootLogger();
	}

	/**
	 * Imprime a relação de todos os loggers conhecidos.
	 */
	public static void imprimirLoggersConhecidos(PrintStream out) {
		out.println("Loggers conhecidos:");
		@SuppressWarnings("unchecked")
		Enumeration<org.apache.log4j.Logger> loggerEnum = LogManager.getCurrentLoggers();
		TreeSet<org.apache.log4j.Logger> loggers = new TreeSet<org.apache.log4j.Logger>(new Comparator<org.apache.log4j.Logger>() {
			public int compare(org.apache.log4j.Logger o1, org.apache.log4j.Logger o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		loggers.addAll(Collections.list(loggerEnum));
		for (org.apache.log4j.Logger logger : loggers) {
			out.println(" - " + logger.getName() + ": " + (logger.getLevel() != null ? logger.getLevel().toString() : "NULL"));
		}
		if (loggers.size() == 0) {
			out.println(" - Não há loggers conhecidos.");
		}
	}
}
