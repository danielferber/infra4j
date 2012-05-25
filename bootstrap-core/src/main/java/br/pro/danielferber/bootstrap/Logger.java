package br.pro.danielferber.bootstrap;

import java.util.logging.Level;

/**
 * Um mecanismo bem simples para escrever mensagens de depuração durante a execução do bootstrap.
 * A interface é semelhante ao logger original do Java ({@link java.util.logging.Logger}).
 * <p>
 * Esta foi a alternativa encontrada para prover um mecanismo logging durante o bootstrap.
 * O uso de um framework específico de logging durante um bootstrap causa falhas aleatórias e não rastreáveis.
 * <p>
 * Escreve as mensagens em stdout/stderr.
 * O único ajuste possível é o nível de detalhamento das mensagens de depuração.
 * A propriedade 'bootstrap.logger=level' controla o nível geral de todos os loggers.
 * O nível de cada logger pode ser atribuído individualmente pela propriedade 'bootstrap.logger.nome=level', com prioridade sobre o nível geral.
 * O nível de detalhamento é o mesmo {@link Level} do logger original do Java.
 * Para aprender como informar uma propriedade para o bootstrap, consulte {@link br.pro.danielferber.bootstrap.Configuracao}.
 * <p>
 * Por simplicidade, a criação de um logger é cara, pois não há cache das configurações.
 * Não há suporte para uma hierarquia de loggers. O nome do logger precisa ser uma palavra simples.
 * 
 * @author Daniel Felix Ferber
 * 
 */
class Logger {
	/** Nível do detalhamento das mensagens do logger. */
	private final Level level;
	/** Nome do logger. */
	private final String name;

	/**
	 * Obtém um logger para nome.
	 * @param name Nome do logger.
	 * @return O Logger associado ao nome.
	 */
	public static Logger getLogger(String name) {
		if (name == null) throw new IllegalArgumentException("name == null");
				
		/* Tenta o nível específico do logger. */
		Configuracao configuracao = new Configuracao("bootstrap");
		String levelStr = configuracao.getProperty("logger."+name, null);
		if (levelStr == null) {
			/* Tenta o nível geral dos loggers. */
			levelStr = configuracao.getProperty("logger", Level.INFO.getName());
		}
		
		/* Interpreta o nome do nível. */
		if (levelStr == null) throw new IllegalArgumentException("levelStr == null");
		Level level;
		try {
			level = Level.parse(levelStr);
		} catch (Exception e) {
			throw new IllegalArgumentException("Valor inválido para nível de logger.", e);
		}
		
		/* Cria o logger. */
		if (level == null) throw new IllegalArgumentException("level == null");
		return new Logger(name, level);
	}

	/**
	 * Construtor padrão.
	 * @param name Nome do logger. Deve ser uma palavra simples.
	 */
	protected Logger(String name, Level level) {
		if (level == null) throw new IllegalArgumentException("level == null");
		if (name == null) throw new IllegalArgumentException("name == null");
		this.level = level;
		this.name = name;
	}

	/**
	 * Construtor padrão.
	 * @param name Nome do logger. Deve ser uma palavra simples.
	 */
	public void log(Level level, String msg, Object... params) {
		if (level == null) throw new IllegalArgumentException("level == null");
		if (msg == null) throw new IllegalArgumentException("msg == null");
	
		if (this.level.intValue() <= level.intValue()) {
			try {
				System.out.println(level + " " + name + " " + String.format(msg, params));
			} catch (Exception e) {
				System.out.println(level + " " + name + " " + msg);
			}
		}
	}

	public void log(Level level, String msg, Throwable thrown) {
		if (level == null) throw new IllegalArgumentException("level == null");
		if (msg == null) throw new IllegalArgumentException("msg == null");
		if (thrown == null) throw new IllegalArgumentException("thrown == null");
		
		if (this.level.intValue() <= level.intValue()) {
			System.out.println(level + " " + name + " " + String.format(msg));
			thrown.printStackTrace(System.out);
		}
	}

	public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object... params) {
		this.log(level, sourceClass + "." + sourceMethod + ": " + msg, params);
	}

	public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
		this.log(level, sourceClass + "." + sourceMethod + ": " + msg, thrown);
	}

	public void entering(String sourceClass, String sourceMethod, Object... params) {
		StringBuilder sb = new StringBuilder("enter " + sourceClass + "." + sourceMethod + "(");
		boolean first = true;
		for (Object object : params) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(object);
		}
		sb.append(")");
		this.log(Level.FINE, sb.toString());
	}

	public void exiting(String sourceClass, String sourceMethod) {
		this.log(Level.FINE, "leave " + sourceClass + "." + sourceMethod + "()");
	}

	public void exiting(String sourceClass, String sourceMethod, Object result) {
		this.log(Level.FINE, "leave " + sourceClass + "." + sourceMethod + "(): " + result);
	}

	public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
		this.log(Level.WARNING, "leave " + sourceClass + "." + sourceMethod + "()", thrown);
	}

}
