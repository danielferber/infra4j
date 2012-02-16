package infra.ilog;


/**
 * Signals that the solver was not able to determine any solution within allowed time, though the model might be feasible.
 * The reason for not being able to find the solution is given by the {@link Reason} enumeration.
 */
public class NoSolutionException extends Exception {
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
		this.reason = reason;
	}
}
