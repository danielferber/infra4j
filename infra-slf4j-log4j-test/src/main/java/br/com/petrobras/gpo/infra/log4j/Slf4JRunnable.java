/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executa um teste do Slf4J.
 * 
 * @author Daniel Felix Ferber(x7ws)
 */
public class Slf4JRunnable implements Runnable {

	public void run() {
		Logger loggerA = LoggerFactory.getLogger("log4j.a");
		Logger loggerB = LoggerFactory.getLogger("log4j.b");
		Logger loggerAC = LoggerFactory.getLogger("log4j.a.c");
		
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
