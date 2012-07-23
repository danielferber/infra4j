package infra.ilog;

import infra.exception.RichException;

/**
 * Signals that the solver was not able to determine any solution within allowed
 * time or number of iterations. Though, the model might be feasible. The reason
 * for not being able to find the solution is given by the {@link #reason}
 * attribute.
 */
public class NoSolutionException extends RichException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		/**
		 * The problem is unbounded. The objective function may increase or
		 * decrease infinitely as there are no constraints.
		 */
		UNBOUNDED,
		/**
		 * The problem is infeasible. There is no solution that might satisfy
		 * all constraints.
		 */
		INFEASIBLE,
		/**
		 * The problem does not have a solution. But it was not possible to
		 * determine if the problem is unbound or infeasible.
		 */
		UNBOUNDED_INFEASIBLE,
		/**
		 * There was not yet enough time to find a solution.
		 */
		INCOMPLETE,
		/**
		 * The execution was interrupted programmatically from another thread.
		 */
		INTERRUPTED;
	}

	/** The reason why the execution failed. */
	public final Reason reason;

	public NoSolutionException(Reason reason) {
		super(reason);
		this.reason = reason;
	}

	@Override
	public NoSolutionException data(String key, Object value) {
		return (NoSolutionException) super.data(key, value);
	}

	@Override
	public NoSolutionException reason(Object reason) {
		return (NoSolutionException) super.reason(reason);
	}

	@Override
	public NoSolutionException operation(Object operation) {
		return (NoSolutionException) super.operation(operation);
	}
}
