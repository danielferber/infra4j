package infra.logback;

import ch.qos.logback.classic.LoggerContext;

/**
 * Signals that the Logback reconfigure method failed.
 * Check the {@link #reason} attribute for further details.
 * If the configuration file was read, then {@link #loggerContext} contains further details according to Logback implementation.
 * @author Daniel Felix Ferber
 *
 */
public class LogbackReconfigureException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		/** The install was called simultaneously by multiple threads. */
		REENTRANT_RECONFIGURE,
		/** The configuration file could not be opened or read. Check the wrapped exception for details. */
		ARQUIVO_CONFIG,
		/** The configuration file content is invalid. Check {@link LogbackReconfigureException#loggerContext} for details. */
		CONFIGURACAO
	}

	/** Describes why the Logback install method failed.. */
	public final Reason reason;
	/** The logging context that contains Logback details. */
	public final LoggerContext loggerContext;

	protected LogbackReconfigureException(Reason reason) {
		super();
		this.reason = reason;
		this.loggerContext = null;
	}

	public LogbackReconfigureException(Reason reason, Throwable e) {
		super(e);
		this.reason = reason;
		this.loggerContext = null;
	}

	public LogbackReconfigureException(Reason reason, Throwable e, LoggerContext lc) {
		super(e);
		this.reason = reason;
		this.loggerContext = lc;
	}
}
