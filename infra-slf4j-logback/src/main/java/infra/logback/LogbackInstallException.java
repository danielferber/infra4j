package infra.logback;

/**
 * Signals that the Logback install method failed.
 * Check the {@link #reason} attribute for further details.
 *
 * @author Daniel Felix Ferber
 *
 */
public class LogbackInstallException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		/** The install was called simultaneously by multiple threads. */
		REENTRANT_INSTALL,
		/** The install was called again after Logback was already installed. */
		DUPLICATED_INSTALL,
		/** There are insuficient permissions to redirect java logging to slf4j/logback. */
		MISSING_PERMISSIONS
	}

	/** Describes why the Logback install method failed.. */
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
