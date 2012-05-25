package br.pro.danielferber.bootstrap;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Mantém o estado de ciclo de vida de {@link BootstrapHandler}. 
 * <p>
 * O ciclo de vida utiliza a classe que implementa {@link br.pro.danielferber.bootstrap.BootstrapHandler}, 
 * responsável por executar a inicialização e a finalização do servidor, obtida como uma propriedade 'bootstrap.handler=nomeQualificadoClasse'.
 * Para aprender como informar uma propriedade para o bootstrap, consulte {@link br.pro.danielferber.bootstrap.Configuracao}.
 * <p>
 * O ciclo de vida apresenta um dos quatro estados.
 * <ul>
 * <li>{@link CicloVida.Estado#INATIVO}: Ainda não iniciou ou já terminou.
 * <li>{@link CicloVida.Estado#INICIANDO}: Iniciando os módulos.
 * <li>{@link CicloVida.Estado#EXECUTANDO}: Os módulos estão em execução..
 * <li>{@link CicloVida.Estado#TERMINANDO}: Terminando módulos.
 * </ul>
 * Se ocorre uma exceção durante a inicialização, então a inicializada é revertida executando o término e o estado volta
 * para {@link CicloVida.Estado#INATIVO}. 
 * <p>Existem várias condições de término, que podem ocorrer concorrentemente. Apenas
 * uma delas prevalecerá e executará a o processo de término. 
 */
class CicloVida {
	/* Strings para mensagens de log. */
	private static final String nomeSimplesClasse = CicloVida.class.getSimpleName();
	private static final String nomeMethodoStart = "start";
	private static final String nomeMethodoStop = "stop";
	
	/* Estados do ciclo de vida. */
	public static enum Estado {
		INICIANDO, EXECUTANDO, TERMINANDO, INATIVO
	}
	
	/* Estado do ciclo de vida do bootstrap. */
	private Estado estado = Estado.INATIVO;

	/** Handler que implementa o bootstrap. */
	private BootstrapHandler bootstrapHandler;
	
	/**  Lock que controla as transições de estado. */
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition estadoAlterado = lock.newCondition();
		
	/**
	 * Construtor padrão. 
	 */
	public CicloVida() {
		super();
	}
	
	/**
	 * Espera até o ciclo de vida completar e voltar ao estado 'inativo'.
	 * @throws InterruptedException 
	 */
	public void aguardarTerminoExecucao() throws InterruptedException {
		try {
			lock.lock();
			while (estado != Estado.INATIVO) {
				estadoAlterado.await();
			}
		} finally {
			lock.unlock();
		}
	}
	
	/** Método auxiliar para realizar a transição. */
	private void executaTransicao(final Logger logger, String nomeClasse, String nomeMethodo, Estado novoEstado) {
		if (! lock.isHeldByCurrentThread()) throw new IllegalStateException("(! lock.isHeldByCurrentThread()");
		
		logger.logp(Level.INFO, nomeClasse, nomeMethodo, "Transição de %s para %s.", estado, novoEstado);
		estado = novoEstado;
		estadoAlterado.signalAll();
	}
	
	/**
	 * Inicia o ciclo de vida. Pressupõe que o estado seja 'inativo'.
	 * @param bootstrapHandler Handler que implementa o bootstrap
	 */
	public void start(BootstrapHandler bootstrapHandler) {
		if (bootstrapHandler == null) throw new IllegalArgumentException("bootstrapHandler == null");
		this.bootstrapHandler = bootstrapHandler;
		
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMethodoStart);
		
		/*
		 * O start só acontece no estado INATIVO, e somente para a primeira thread chamar o start.
		 * Se o estado não for INATIVO, então ignora a chamada.
		 */
		try {
			lock.lock();
			if (estado == Estado.INATIVO) {
				executaTransicao(logger, nomeSimplesClasse, nomeMethodoStart, Estado.INICIANDO);
			} else {
				logger.logp(Level.FINE, nomeSimplesClasse, nomeMethodoStart, "Ignorou transição de %s para %s.", estado, Estado.INICIANDO);
				return;
			}
		} finally {
			lock.unlock();
		}

		/*
		 * Tenta executar o start enquanto mantém o estado INICIANDO. 
		 */
		boolean sucesso = false;
		try {
			bootstrapHandler.startServer();
			sucesso = true;
			logger.logp(Level.INFO, nomeSimplesClasse, nomeMethodoStart, "Iniciou com sucesso.");
		} catch (Exception e) {
			logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMethodoStart, "Falha ao iniciar.", e);
			try {
				bootstrapHandler.stopServer();
			} catch (Exception ee) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMethodoStart, "Falha reverter para estado inativo.", ee);
			}
		}
		
		/*
		 * Dependendo do sucesso ou falha, o estado após a inicialização
		 * será, respectivamente, EXECUTANDO ou INATIVO.
		 */
		try {
			lock.lock();
			if (sucesso) {
				executaTransicao(logger, nomeSimplesClasse, nomeMethodoStart, Estado.EXECUTANDO);
			} else {
				executaTransicao(logger, nomeSimplesClasse, nomeMethodoStart, Estado.INATIVO);
			}
		} finally {
			lock.unlock();
		}
		
		logger.exiting(nomeSimplesClasse, nomeMethodoStart);
	}

	public void stop() {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMethodoStop);
		
		/*
		 * O stop só acontece no estado EXECUTANDO, e somente para a primeira thread chamar o start.
		 * Se o estado não for EXECUTANDO, então ignora a chamada.
		 * Um pequeno detalhe: Caso o estado seja INICIANDO, então é necessário esperar até que o
		 * estado seja EXECUTANDO, para somente então decidir qual thread prevalecerá.
		 */
		try {
			lock.lock();
			
			while (estado == Estado.INICIANDO) {
				try {
					estadoAlterado.await();
				} catch (InterruptedException e) {
					return;
				}
			}
			
			if (estado == Estado.EXECUTANDO) {
				executaTransicao(logger, nomeSimplesClasse, nomeMethodoStop, Estado.TERMINANDO);
			} else {
				logger.logp(Level.FINE, nomeSimplesClasse, nomeMethodoStop, "Ignorou transição de %s para %s.", estado, Estado.TERMINANDO);
				return;
			}
		} finally {
			lock.unlock();
		}
		
		try {
			bootstrapHandler.stopServer();
		} catch (Exception e) {
			logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMethodoStop, "Falha ao terminar.", e);
		}

		try {
			lock.lock();
			executaTransicao(logger, nomeSimplesClasse, nomeMethodoStart, Estado.INATIVO);
		} finally {
			lock.unlock();
		}
		
		logger.exiting(nomeSimplesClasse, nomeMethodoStop);
	}
}
