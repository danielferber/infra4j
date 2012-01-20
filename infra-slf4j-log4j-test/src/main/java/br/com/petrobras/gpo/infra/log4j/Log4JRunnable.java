/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import org.apache.log4j.Logger;

/**
 * Executa um teste do Log4J.
 * 
 * @author Daniel Felix Ferber(x7ws)
 */
public class Log4JRunnable implements Runnable {

	public void run() {
		Logger loggerA = Logger.getLogger("log4j.a");
		Logger loggerB = Logger.getLogger("log4j.b");
		Logger loggerAC = Logger.getLogger("log4j.a.c");

		loggerA.fatal("Teste fatal em A");
		loggerB.fatal("Teste fatal em B");
		loggerAC.fatal("Teste fatal em AC");

		loggerA.error("Teste error em A");
		loggerB.error("Teste error em B");
		loggerAC.error("Teste error em AC");

		loggerA.warn("Teste warn em A");
		loggerB.warn("Teste warn em B");
		loggerAC.warn("Teste warn em AC");

		loggerA.info("Teste info em A");
		loggerB.info("Teste info em B");
		loggerAC.info("Teste info em AC");

		loggerA.debug("Teste debug em A");
		loggerB.debug("Teste debug em B");
		loggerAC.debug("Teste debug em AC");

		loggerA.trace("Teste trace em A");
		loggerB.trace("Teste trace em B");
		loggerAC.trace("Teste trace em AC");
	}

}
