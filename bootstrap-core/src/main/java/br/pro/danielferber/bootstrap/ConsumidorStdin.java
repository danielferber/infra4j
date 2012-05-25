package br.pro.danielferber.bootstrap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * Thread que consome o stdin a procura pela palavra {@value #STOP_CMD} que sinaliza a intensão do administrador de terminar o servidor.
 * <p>
 * Quando constatada a intensão de terminar o servidor, a thread notifica o ciclo de vida com a chamada do método {@link CicloVida#stop() }.
 * <p>
 * Esta thread morre espontaneamente quando o stdin deixa de existir. Não é necessário preocupar-se com a sua finalização.
 * <p>
 * Nota: Stdin precisa ser consumido em uma thread independente, pois a leitura de stdin é bloqueante.
 */
class ConsumidorStdin extends Thread {
	/* Strings para mensagens de logger. */ 
	private static final String nomeSimplesClasse = ConsumidorStdin.class.getSimpleName();
	private static final String nomeMetodoRun = "run";
	private static final String nomeConstrutor = "constructor";
	private static final String nomeThreadConsumidorStdin = "bootstrap - consumidor stdin";

	/** Instância do ciclo de vida que será notificada. */
	private final CicloVida cicloVida;
	
	/** Comando esperado no stdin para terminar a JVM. */
	private final static String STOP_CMD = "stop";
	
	/**
	 * Construtor. Não esqueça de chamar o método {@link #start()} após chamar o construtor.
	 * 
	 * @param cicloVida
	 *            {@link CicloVida} Instância global que será notificada.
	 */
	public ConsumidorStdin(CicloVida cicloVida) {
		super(nomeThreadConsumidorStdin);
		
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeConstrutor);
		
		if (cicloVida == null) throw new IllegalArgumentException("cicloVida == null");
		this.cicloVida = cicloVida;

		/* 
		 * Thread deamon: Garante que a thread morre espontaneamente quando o stdin deixa existir.
		 * Uma das formas do stdin deixar de existir é quando o programa termina.
		 * Este é um artifício para evitar que a thread segure indefinidamente o stdin e assim
		 * impeça o término do programa.
		 */
		setDaemon(true);
		/*
		 * Thread de baixa prioridade: Para minimizar uso de recursos.
		 */
		setPriority(MIN_PRIORITY);
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

		logger.exiting(nomeSimplesClasse, nomeConstrutor);
	}
	
	@Override
	public void run() {
		final Logger logger = Logger.getLogger(nomeSimplesClasse);
		logger.entering(nomeSimplesClasse, nomeMetodoRun);
		
		try {
			/* Monitora STDIN pela palavra. */
			try {
				String mensagem = "Digite '"+STOP_CMD+"' + ENTER para terminar.";
				System.out.println(mensagem);
				logger.logp(Level.INFO, nomeSimplesClasse, nomeMetodoRun, mensagem);
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					String s = null;
					do {
						s = in.readLine();
					} while (! s.equalsIgnoreCase(STOP_CMD));
				}
				logger.logp(Level.FINE, nomeSimplesClasse, nomeMetodoRun, "Reconheu comando para terminar.");
			} catch (Exception e) {
				if (! interrupted()) {
					logger.logp(Level.WARNING, nomeSimplesClasse, nomeMetodoRun, "Interrompido por exceção.", e);
				}
			}
			
			/* Chamada blindada para notificar o ciclo de vida. */
			try {
				logger.logp(Level.INFO, nomeSimplesClasse, nomeMetodoRun, "Término por stdin acionado.");
				{
					cicloVida.stop();
				}
				logger.logp(Level.FINE, nomeSimplesClasse, nomeMetodoRun, "Sucesso do término por stdin.");
			} catch (Exception e) {
				logger.logp(Level.SEVERE, nomeSimplesClasse, nomeMetodoRun, "Falha durante término por stdin.");
			}
			
		} finally {
			logger.exiting(nomeSimplesClasse, nomeMetodoRun);
		}
	}
};
