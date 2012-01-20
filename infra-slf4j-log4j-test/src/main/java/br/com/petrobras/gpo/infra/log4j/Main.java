/*
 * Copyright 2011 Petrobras
 * Este arquivo segue o padrão PE-2T0-00250.
 * 
 * Análise e implementação pelo Grupo de Pesquisa Operacional.
 */
package br.com.petrobras.gpo.infra.log4j;

import br.com.petrobras.gpo.infra.exception.motivo.MotivoException;
import br.com.petrobras.gpo.infra.log4j.ServicoLog4J;

/**
 * Executa um teste de vários frameworks de logger configurados pelo {@link ServicoLog4J}.
 * 
 * @author Daniel Felix Ferber(x7ws)
 */

public class Main {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ServicoLog4J.instalar();
			ServicoLog4J.imprimirLoggersConhecidos(System.out);
		} catch (MotivoException e) {
			e.printStackTrace();
			return;
		}
		new CommonsRunnable().run();
		new JulRunnable().run();
		new Log4JRunnable().run();
		new Slf4JRunnable().run();
		new MeterRunnable().run();
	}

}
