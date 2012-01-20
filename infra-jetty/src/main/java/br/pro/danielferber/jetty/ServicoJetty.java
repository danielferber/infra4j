package br.pro.danielferber.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import br.pro.danielferber.infra.exception.assertions.datastate.IllegalEnvironmentDataException;
import br.pro.danielferber.infra.exception.motivo.Motivo;
import br.pro.danielferber.infra.exception.motivo.MotivoException;


/**
 * Gerencia uma instância do Jetty para embutir servlets.
 * <p>
 * Não existe um mecanismo padrão para obter a configuração do classpath (tal como faz o hibernate ou o logback).
 * Por este motivo é necessário realizar a configuração através do arquivo "config/jetty.cfg.xml" em relação ao diretório de execução.
 * O XML deste arquivo segue a convenção de XmlConfiguration como explicado na documentação do jetty.
 * 
 * @author Daniel Felix Ferber
 * 
 */
public class ServicoJetty {
	private static final Logger logger = LoggerFactory.getLogger(ServicoJetty.class);

	private final static String nomeDir = "config";
	private final static String nomeArq = "jetty.cfg.xml";

	/** Lock que sincroniza alteração de estado do serviço. */
	private Lock lock = new ReentrantLock();
	/** Singleton. */
	private static ServicoJetty instance = new ServicoJetty();
	/** Server gerenciado pelo serviço. */
	private Server server;
	
	protected ServicoJetty() {
		logger.debug("Singleton criado.");
	}
	
	public static ServicoJetty getInstance() { return instance; }
	
	public Server getServer() { return server; }

	public static enum MotivoConfiguracaoJetty implements Motivo {
		ARQUIVO_CONFIG("Falha ao abrir arquivo de configuração."), 
		CONFIGURACAO("Falha ao interpretar configuração."), ;
		private String mensagem;

		private MotivoConfiguracaoJetty(String mensagem) { this.mensagem = mensagem; }
		@Override public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Ler configuração de Jetty."; }
	}
	
	public static enum MotivoJettyStart implements Motivo {
		INICIAR("Falha iniciar server."),
		;
		private String mensagem;
		MotivoJettyStart(String mensagem) { this.mensagem = mensagem; }
		@Override public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Criar servidor Jetty."; }
	}
	
	/**
	 * Inicia o servidor Jetty.
	 * @throws MotivoException {@link MotivoJettyStart} ou {@link MotivoConfiguracaoJetty}.
	 */
	public void start(Runnable starter) throws MotivoException {		
		try {
			lock.lock();
			logger.info("Iniciando.");

			/* Evita múltiplas chamadas para o método start. */
			if (server != null) {
				logger.error("ServicoJetty já estava iniciado.");
				return;
			}
	
			/* Criar server. */
			this.server = (Server) obterServerConfigurado();
			
			starter.run();
			
			try {
				this.server.start();
			} catch (Exception e) {
				throw new MotivoException(e, MotivoJettyStart.INICIAR);
			}
			
			logger.info("Executando.");
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Finaliza o servidor Jetty.
	 */
	public void stop() {
		try {
			lock.lock();
			logger.info("Terminando.");
			
			/* Evita múltiplas chamadas para o método start. */
			if (server == null) {
				logger.error("ServicoJetty já estava inativo.");
				return;
			}

			/* Termina o servidor. */
			try {
				server.stop();
			} catch (Exception e) {
				logger.warn("stop: falha terminar Jetty.", e);
			}
			server = null;
			logger.info("Inativo.");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Estratégia para obter a configuração do JEtty.
	 * @return Configuração para o Jetty
	 * @throws MotivoException #{@link MotivoConfiguracaoJetty}.
	 */
	protected Server obterServerConfigurado() throws MotivoException {
		logger.trace("Obter configuração para hibernate.");

		/* 
		 * Cria a configuração manualmente do arquivo. 
		 */
		String nomeDirAtual = System.getProperty("user.dir"); assert IllegalEnvironmentDataException.notNull(nomeDirAtual);
		File dirConfig = new File(nomeDirAtual, nomeDir);
		File arqConfig = new File(dirConfig, nomeArq);
		InputStream is = null;
		try {
			is = new FileInputStream(arqConfig);
		} catch (IOException e) {
			throw new MotivoException(e, MotivoConfiguracaoJetty.ARQUIVO_CONFIG);
		}

		/* 
		 * Ler e aplicar configuração do arquivo XML. 
		 */
		XmlConfiguration configuration = null;
		try {
			configuration = new XmlConfiguration(is);
		} catch (SAXException e) {
			throw new MotivoException(e, MotivoConfiguracaoJetty.CONFIGURACAO);
		} catch (IOException e) {
			throw new MotivoException(e, MotivoConfiguracaoJetty.CONFIGURACAO);
		}
		
		/*
		 * Criar server configurado
		 */
		Server server = null;
		try {
			server = (Server) configuration.configure();
		} catch (Exception e) {
			throw new MotivoException(e, MotivoConfiguracaoJetty.CONFIGURACAO);
		}

		return server;
	}
}
