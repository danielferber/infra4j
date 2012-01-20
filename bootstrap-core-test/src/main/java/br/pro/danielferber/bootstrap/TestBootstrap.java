package br.pro.danielferber.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import br.pro.danielferber.bootstrap.BootstrapHandler;


/**
 * Esta classe é apenas um teste para o mecanismo de bootstrap.
 * <p>
 * Implementa um 'servidor' que mostra seu status e não faz nada além disso.
 * Todas as mensagens são direcionadas para um framework de logging, de forma que seja possível
 * testar se a existência de tal framework não interfere no funcionamento correto do bootstrap.
 * @author Daniel Felix Ferber
 */
public class TestBootstrap implements BootstrapHandler {
	private final Logger logger = LoggerFactory.getLogger(BootstrapHandler.class);
	static {
		SLF4JBridgeHandler.install();
	}
	
	@Override
	public void startServer() {
		
		logger.info("Começando server...");
		synchronized (this) {
			try {
				this.wait(1000);
				logger.info("Começando 1");
				this.wait(1000);
				logger.info("Começando 2");
				this.wait(1000);
				logger.info("Começando 3");
				this.wait(1000);
				logger.info("Começando pronto");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("Começando server: OK");
	}

	@Override
	public void stopServer() {
		logger.info("Terminando server...");
		synchronized (this) {
			try {
				this.wait(1000);
				logger.info("Terminando 1");
				this.wait(1000);
				logger.info("Terminando 2");
				this.wait(1000);
				logger.info("Terminando 3");
				this.wait(1000);
				logger.info("Terminando pronto");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("Terminando server: OK");
	}
}
