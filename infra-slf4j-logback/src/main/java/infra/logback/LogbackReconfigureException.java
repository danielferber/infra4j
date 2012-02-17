package infra.logback;

import ch.qos.logback.classic.LoggerContext;


public class LogbackReconfigureException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		REENTRANT_RECONFIGURE,
		MISSING_PERMISSIONS,
		ARQUIVO_CONFIG,
		CONFIGURACAO
	}

	public final Reason reason;
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
