package infra.ilog;

import infra.exception.RichException;


/**
 * Signals that the solver was not able to determine any solution within allowed time or number of iterations, though the model might be feasible.
 * The reason for not being able to find the solution is given by the {@link #reason} attribute.
 */
public class NoSolutionException extends RichException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		ILIMITADO,
		INVIAVEL,
		ILIMITADO_INVIAVEL,
		INCOMPLETO,
		INTERROMPIDO;
	}

	public final Reason reason;

	public NoSolutionException(Reason reason) {
		super(reason);
		this.reason = reason;
	}
}
