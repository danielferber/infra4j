/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Executa um teste do apache commons logger.
 * 
 * @author Daniel Felix Ferber(x7ws)
 */
public class CommonsRunnable implements Runnable {

	public void run() {
		Log loggerA = LogFactory.getLog("apache.a");
		Log loggerB = LogFactory.getLog("apache.b");
		Log loggerAC = LogFactory.getLog("apache.a.c");

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
