package br.pro.danielferber.bootstrap;

import java.util.logging.Level;

/**
 * Thread executada quando a JVM deseja terminar.
 * <p>
 * Quando constatada a intensão de terminar o servidor, a thread notifica o ciclo de vida com a chamada do método {@link CicloVida#stop() }.
 */
class ShutdownHook extends Thread {
	/* Strings para mensagens de logger. */
	private static final String nomeSimplesClasse = ShutdownHook.class.getSimpleName();
	private static final String nomeConstrutor = "constructor";
	private static final String nomeMetodoRun = "run";
	private static final String nomeThread = "bootstrap - shutdown hook";

	/** Instância do ciclo de vida que será notificada. */
	private final CicloVida cicloVida;

	/**
	 * Construtor. Registra a thread como shutdown hook na JVM.
	 * <p>
	 * Apesar de ser uma thread, não é permitido chamar o método {@link #start()}.
	 * Registra automaticamente a si mesmo como shutdown hook da JVM.
	 * 
	 * @param cicloVida
	 *            {@link CicloVida} Instância global que será notificada.
	 */
	public ShutdownHook(CicloVida cicloVida) {
		super(nomeThread);
		
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeConstrutor, cicloVida);

		if (cicloVida == null) throw new IllegalArgumentException("cicloVida == null");
		this.cicloVida = cicloVida;
		
		/*
		 * Handler para exceções não previstas.
		 */
		setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoRun, "Falha durante execução.", e);
				e.printStackTrace();
			}
		});


		/* Chamada controlada para registrar a thread na JVM. */
		try {
			logger.logp(Level.INFO, nomeSimplesClasse, nomeConstrutor, "Instalar shutdown hook.");
			{
				Runtime.getRuntime().addShutdownHook(this);
			}
			logger.logp(Level.FINE, nomeSimplesClasse, nomeConstrutor, "Sucesso em instalar shutdown hook.");			
		} catch (Exception e) {
			logger.logp(Level.SEVERE, nomeSimplesClasse, nomeConstrutor, "Falha ao instalar shutdown hook.", e);
			throw new RuntimeException("Falha ao instalar shutdown hook.", e);
		}
		
		logger.exiting(nomeSimplesClasse, nomeConstrutor);			
	}

	@Override
	public void run() {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMetodoRun);
		
		try {
			try {
				logger.logp(Level.INFO, nomeSimplesClasse, nomeMetodoRun, "Shutdown hook acionado.");
				{
					cicloVida.stop();
				}
				logger.logp(Level.FINE, nomeSimplesClasse, nomeMetodoRun, "Susucesso em executar shutdown hook.");
			} catch (Exception e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoRun, "Falha ao executar shutdown hook.", e);
			}
			
		} finally {
			logger.exiting(nomeSimplesClasse, nomeMetodoRun);
		}
	}
}