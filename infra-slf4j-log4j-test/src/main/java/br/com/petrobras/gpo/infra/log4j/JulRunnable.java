/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import java.util.logging.Logger;

/**
 * Executa um teste do JUL (logger padrão do java).
 * 
 * @author Daniel Felix Ferber(x7ws)
 */
public class JulRunnable implements Runnable {

	public void run() {
		Logger loggerA = Logger.getLogger("jul.a");
		Logger loggerB = Logger.getLogger("jul.b");
		Logger loggerAC = Logger.getLogger("jul.a.c");

		loggerA.severe("Teste severe em A");
		loggerB.severe("Teste severe em B");
		loggerAC.severe("Teste severe em AC");

		loggerA.warning("Teste warning em A");
		loggerB.warning("Teste warning em B");
		loggerAC.warning("Teste warning em AC");

		loggerA.info("Teste info em A");
		loggerB.info("Teste info em B");
		loggerAC.info("Teste info em AC");

		loggerA.config("Teste info em A");
		loggerB.config("Teste info em B");
		loggerAC.config("Teste info em AC");

		loggerA.fine("Teste fine em A");
		loggerB.fine("Teste fine em B");
		loggerAC.fine("Teste fine em AC");

		loggerA.finer("Teste finer em A");
		loggerB.finer("Teste finer em B");
		loggerAC.finer("Teste finer em AC");

		loggerA.finest("Teste finest em A");
		loggerB.finest("Teste finest em B");
		loggerAC.finest("Teste finest em AC");

	}

}
