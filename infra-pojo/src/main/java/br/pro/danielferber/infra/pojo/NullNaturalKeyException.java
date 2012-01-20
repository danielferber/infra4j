package br.pro.danielferber.infra.pojo;

public class NullNaturalKeyException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public NullNaturalKeyException() {
		super();
	}

	public NullNaturalKeyException(String message, Throwable cause) {
		super(message, cause);
	}

	public NullNaturalKeyException(String s) {
		super(s);
	}

	public NullNaturalKeyException(Throwable cause) {
		super(cause);
	}
}
