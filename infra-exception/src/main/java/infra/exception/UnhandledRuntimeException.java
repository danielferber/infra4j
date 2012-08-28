package infra.exception;

import org.slf4j.Logger;


public final class UnhandledRuntimeException extends RichRuntimeException {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = ExceptionService.logger;

	public static enum Reason {
		UNHANDLED_RUNTIME_EXCEPTION
	}

	public UnhandledRuntimeException(Throwable cause) {
		super(cause instanceof UnhandledRuntimeException ? cause.getCause() : cause);
		if (cause == null) {
			UnhandledRuntimeException.logger.error("Called UnhandledRuntimeException(cause='null') with null parameter.");
		}
		if (cause instanceof UnhandledRuntimeException) {
			UnhandledRuntimeException.logger.error("Called UnhandledRuntimeException(cause={}) with recursive UnhandledRuntimeException parameter.", cause);
		}
		this.reasons.add(Reason.UNHANDLED_RUNTIME_EXCEPTION);
	}

	@Override
	public UnhandledRuntimeException data(String key, Object value) {
		return (UnhandledRuntimeException) super.data(key, value);
	}

	@Override
	public UnhandledRuntimeException reason(Object reason) {
		return (UnhandledRuntimeException) super.reason(reason);
	}

	@Override
	public UnhandledRuntimeException operation(Object operation) {
		return (UnhandledRuntimeException) super.operation(operation);
	}
}
