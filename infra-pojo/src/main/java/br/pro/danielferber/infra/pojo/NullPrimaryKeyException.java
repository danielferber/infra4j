package br.pro.danielferber.infra.pojo;

public class NullPrimaryKeyException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public NullPrimaryKeyException() {
		super();
	}

	public NullPrimaryKeyException(String message, Throwable cause) {
		super(message, cause);
	}

	public NullPrimaryKeyException(String s) {
		super(s);
	}

	public NullPrimaryKeyException(Throwable cause) {
		super(cause);
	}
}
