package br.pro.danielferber.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.logging.Level;

/**
 * Provê o ponto de entrada para um Windows Service/Unix Deamon.
 * <p>
 * Esta classe possui apenas os métodos estáticos que são os pontos de entrada.
 * O estado do bootstrap é mantido pela instância global definida por {@link CicloVida}.
 * O método {@link #main(String[])} é um proxy para chamar os métodos {@link #start(String[], boolean, boolean)} ou {@link #stop(String[])}.
 */
public class Bootstrap {
	/* Strings para mensagens de log. */
	private static final String nomeMetodoMain = "main";
	private static final String nomeThreadMain = "bootstrap - main";
	private static final String nomeMetodoStart = "start";
	private static final String nomeThreadStart = "bootstrap - start";
	private static final String nomeMetodoStop = "stop";
	private static final String nomeThreadStop = "bootstrap - stop";
	private static final String nomeSimplesClasse = Bootstrap.class.getSimpleName();

	/* Strings usadas na linha de comando que inicia/termina o Windows Service/Unix Deamon. */
	private static final String RUN_CMD = "run";
	private static final String STOP_CMD = "stop";
	private static final String START_CMD = "start";

	/** Instância global que gerencia o estado do bootstrap. */  
	private static final CicloVida cicloVida = new CicloVida();

	/**
	 * Ponto de entrada Windows Service/Unix Deamon. O método é apenas um proxy que chama
	 * {@link #start(String[], boolean, boolean)} ou {@link #stop(String[])} de acordo com a linha de comando.
	 */
	public static void main(String[] args) {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMetodoMain, args != null ? Arrays.asList(args) : "null");

		/*
		 * Tratamento manual de erro, pois o Windows Service/Unix Deamon pode omitir a exceção ao invés de escrever as
		 * em stdout/stderr.
		 */
		Thread.currentThread().setName(nomeThreadMain);
		Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoMain, "Falha durante execução.", e);
				e.printStackTrace();
				printClasspath();
			}
		});
		
		/*
		 * Inicia o serviço de acordo com o 'modo' da linha de comando.
		 */
		if (args.length == 0) {
			/* Sem comando, executa como um processo interativo. */
			logger.logp(Level.FINE, nomeSimplesClasse, nomeMetodoMain, "main(): 'modo' omitido");
			start(args, true, true);
			return;
		}

		/*
		 * Remove o primeiro elemento do vetor da linha de comando, que foi consumido como sendo o 'modo' de execução.
		 */
		String comando = args[0];
		{
			String[] newargs = new String[args.length - 1];
			System.arraycopy(args, 1, newargs, 0, args.length - 1);
			args = newargs;
		}

		logger.logp(Level.CONFIG, nomeSimplesClasse, nomeMetodoMain, "comando=%s", comando);
		if (STOP_CMD.equals(comando)) {
			/* Comando para interromper Windows Service/Unix Deamon existente. */
			stop(args);
			return;
		} else if (START_CMD.equals(comando)) {
			/* Comando para iniciar novo Windows Service/Unix Deamon. */
			start(args, false, true);
			return;
		} else if (RUN_CMD.equals(comando)) {
			/* Comando para iniciar como processo. */
			start(args, false, true);
			return;
		} else {
			logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoMain, "commando inválido: %s.", comando);
		}

		logger.exiting(nomeSimplesClasse, nomeMetodoMain);
	}

	/**
	 * Creates the global {@link BootstrapHandler} helper object, runs it and optionally wait until it gets inactive
	 * again. The parameters allow different combinations (modes) to run the server as a Windows Service/Unix Deamon or
	 * as a command line process.
	 * 
	 * @param args
	 *            Command line arguments to start the server.
	 * @param createConsumerStdin
	 *            True if stdin should be read and consumed. When True, stdin will be parsed for the stop command, that
	 *            will shutdown the server.
	 * @param waitUntilInactive
	 *            True if the thread shall block until the server gets inactive again.
	 */
	protected static void start(String args[], boolean createConsumerStdin, boolean waitUntilInactive) {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMetodoStart, args != null ? Arrays.asList(args) : "null");

		Thread.currentThread().setName(nomeThreadStart);
		Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoStart, "Falha durante execução.", e);
				e.printStackTrace();
				printClasspath();
			}
		});

		try {

			/* Obtêm o objeto BootstrapHandler das extensões. */
			BootstrapHandler bootstrapHandler = null;
			Configuracao extensao = new Configuracao("bootstrap");
			bootstrapHandler = (BootstrapHandler) extensao.getObjeto("handler", (String)null);
			
			/* Cria o controlador CicloVida que gerenciará as transições de estado do BootstrapHandler. */
			cicloVida.start(bootstrapHandler);
			
			/* Registra um shutdown hook para terminar o estado do BootstrapHandler caso a JVM deseje terminar. */
			new ShutdownHook(cicloVida);

			/* Cria consumidor de stdin para evitar que seu buffer atinja o limite. */
			if (createConsumerStdin) {
				new ConsumidorStdin(cicloVida).start();
			}
		} catch (Exception e) {
			logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoStart, "Falha ao iniciar bootstrap", e);
			return;
		}

		if (waitUntilInactive) {
			logger.logp(Level.INFO, nomeSimplesClasse, nomeMetodoStart, "Aguardar até terminar.");
			try {
				cicloVida.aguardarTerminoExecucao();
			} catch (InterruptedException e) {
				logger.logp(Level.WARNING, nomeSimplesClasse, nomeMetodoStart, "Interrupção ao aguardar até terminar.", e);
			}
		}
		
		logger.exiting(nomeSimplesClasse, nomeMetodoStart);
	}

	/**
	 * Stops the server and destroys the bootstrap helper object.
	 * 
	 * @param args
	 *            Command line arguments to stop the server.
	 */
	public static void stop(String args[]) {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMetodoStop, args != null ? Arrays.asList(args) : "null");
		Thread.currentThread().setName(nomeThreadStop);
		Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoStart, "Falha durante execução.", e);
				e.printStackTrace();
				printClasspath();
			}
		});
		
		cicloVida.stop();

		logger.exiting(nomeSimplesClasse, nomeMetodoStop);
	}

	/** For debugging purpose only. */
	protected static void printClasspath() {
		// Get the System Classloader
		ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

		// Get the URLs
		URL[] urls = ((URLClassLoader) sysClassLoader).getURLs();

		for (int i = 0; i < urls.length; i++) {
			System.out.println(urls[i].getFile());
		}
	}
}
