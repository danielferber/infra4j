package infra.exception;

import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.IllegalAttributeException;
import infra.exception.assertions.datastate.IllegalEnvironmentDataException;
import infra.exception.assertions.datastate.IllegalInputDataException;
import infra.exception.assertions.datastate.SystemRule;

public final class Precondition {
	private Precondition() {
		// cannot create instance
	}

	/*
	 * ARGUMENTS
	 *************************************************************************/

	public static final boolean argument(boolean conditions) throws IllegalArgumentException {
		if (! conditions) throw new IllegalArgumentException();
		return true;
	}

	public static final boolean argument(boolean ... conditions) throws IllegalArgumentException {
		for (boolean b : conditions) {
			if (!b) throw new IllegalArgumentException();
		}
		return true;
	}

	public static final boolean argument(SystemRule rule, boolean expression) throws IllegalArgumentException {
		if (! expression) throw new IllegalArgumentException(rule);
		return true;
	}

	public static final boolean argumentRange(int value, int min, int max) throws IllegalArgumentException {
		if (value < min || value > max ) throw new IllegalArgumentException(String.format("%d not in range [%d-%d]", value, min, max));
		return true;
	}

	public static final boolean argumentPositive(int value) throws IllegalArgumentException {
		if (value <= 0) throw new IllegalArgumentException(String.format("%d not in range [1-inf]", value));
		return true;
	}

	public static final <T> boolean nonNullArgument(T argument) throws IllegalArgumentException {
		if (argument == null) throw new IllegalArgumentException("null");
		return true;
	}

	public static final <T> boolean nonNullArgument(SystemRule rule, T argument) throws IllegalArgumentException {
		if (argument == null) throw new IllegalArgumentException("null");
		return true;
	}

	/*
	 * ATTRIBUTES
	 *************************************************************************/

	public static final boolean attribute(boolean ... conditions) throws IllegalAttributeException {
		for (boolean b : conditions) {
			if (!b) throw new IllegalAttributeException();
		}
		return true;
	}

	public static final boolean attribute(SystemRule rule, boolean expression) throws IllegalAttributeException {
		if (! expression) throw new IllegalAttributeException(rule);
		return true;
	}

	public static final <T> boolean nonNullAttribute(T attribute) throws IllegalAttributeException {
		if (attribute == null) throw new IllegalAttributeException();
		return true;
	}

	public static final <T> boolean nonNullAttribute(SystemRule rule, T attribute) throws IllegalAttributeException {
		if (attribute == null) throw new IllegalAttributeException(rule);
		return true;
	}

	/*
	 * ENVIRONMENT
	 *************************************************************************/

	public static final boolean environment(boolean ... conditions) throws IllegalEnvironmentDataException {
		for (boolean b : conditions) {
			if (!b) throw new IllegalEnvironmentDataException();
		}
		return true;
	}

	public static final boolean environment(SystemRule rule, boolean expression) throws IllegalEnvironmentDataException {
		if (! expression) throw new IllegalEnvironmentDataException(rule);
		return true;
	}

	public static final <T> boolean nonNullEnvironment(T environment) throws IllegalEnvironmentDataException {
		if (environment == null) throw new IllegalEnvironmentDataException();
		return true;
	}

	public static final <T> boolean nonNullEnvironment(SystemRule rule, T environment) throws IllegalEnvironmentDataException {
		if (environment == null) throw new IllegalEnvironmentDataException(rule);
		return true;
	}

	/*
	 * DATA
	 *************************************************************************/

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
