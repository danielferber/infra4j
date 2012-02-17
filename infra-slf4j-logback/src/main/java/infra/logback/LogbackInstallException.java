package infra.logback;


public class LogbackInstallException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		REENTRANT_INSTALL,
		DUPLICATED_INSTALL,
		MISSING_PERMISSIONS
	}

	public final Reason reason;

	protected LogbackInstallException(Reason reason) {
		super();
		this.reason = reason;
	}

	public LogbackInstallException(Reason reason, Throwable e) {
		super(e);
		this.reason = reason;
	}
}
