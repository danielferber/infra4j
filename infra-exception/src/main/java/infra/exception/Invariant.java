package infra.exception;

import infra.exception.assertions.datastate.IllegalInputDataException;
import infra.exception.assertions.datastate.SystemRule;

public class Invariant {
	public static final boolean data(boolean ... conditions) throws IllegalInputDataException {
		for (boolean b : conditions) {
			if (!b) throw new IllegalInputDataException();
		}
		return true;
	}

	public static final boolean data(SystemRule rule, boolean expression) throws IllegalInputDataException {
		if (! expression) throw new IllegalInputDataException(rule);
		return true;
	}

	public static final <T> boolean nonNullData(T data) throws IllegalInputDataException {
		if (data == null) throw new IllegalInputDataException();
		return true;
	}

	public static final <T> boolean nonNullData(SystemRule rule, T data) throws IllegalInputDataException {
		if (data == null) throw new IllegalInputDataException(rule);
		return true;
	}
}
