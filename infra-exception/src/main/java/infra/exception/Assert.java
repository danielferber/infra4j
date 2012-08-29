package infra.exception;

import infra.exception.datastate.IllegalArgumentDataException;
import infra.exception.datastate.IllegalAttributeDataException;
import infra.exception.datastate.IllegalEnvironmentDataException;
import infra.exception.datastate.IllegalInvariantException;
import infra.exception.datastate.IllegalPosConditionException;
import infra.exception.datastate.IllegalPreConditionException;
import infra.exception.datastate.SystemRule;


public abstract class Assert {
	private Assert() {
		// cannot create instance
	}

	public static final Assert Argument = new Assert() {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalArgumentDataException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalArgumentDataException(message); }
		@Override protected RuntimeException createException() { return new IllegalArgumentDataException(); }
	};

	public static final Assert Attribute = new Assert() {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalAttributeDataException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalAttributeDataException(message); }
		@Override protected RuntimeException createException() { return new IllegalAttributeDataException(); }
	};

	public static final Assert Environment = new Environment();

	public static class Environment extends Assert {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalEnvironmentDataException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalEnvironmentDataException(message); }
		@Override protected RuntimeException createException() { return new IllegalEnvironmentDataException(); }

		public final boolean exist(String name) throws RuntimeException {
			if (System.getenv(name) == null) throw createException(String.format("%s must exist in environment", name));
			return true;
		}
	}

	public static final Assert Precondition = new Assert() {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalPreConditionException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalPreConditionException(message); }
		@Override protected RuntimeException createException() { return new IllegalPreConditionException(); }
	};

	public static final Assert Poscondition = new Assert() {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalPosConditionException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalPosConditionException(message); }
		@Override protected RuntimeException createException() { return new IllegalPosConditionException(); }
	};

	public static final Assert Invariant = new Assert() {
		@Override protected RuntimeException createException(SystemRule rule) { return new IllegalInvariantException(rule); }
		@Override protected RuntimeException createException(String message) { return new IllegalInvariantException(message); }
		@Override protected RuntimeException createException() { return new IllegalInvariantException(); }
	};

	protected abstract RuntimeException createException();
	protected abstract RuntimeException createException(String message);
	protected abstract RuntimeException createException(SystemRule rule);

	/** Argument must satisfy a boolean condition. */
	public final boolean check(boolean condition) throws RuntimeException {
		if (! condition) throw createException();
		return true;
	}

	/** Argument(s) must satisfy one or more boolean condition(s). */
	public final boolean check(boolean ... conditions) throws RuntimeException {
		for (boolean b : conditions) {
			if (!b) throw createException();
		}
		return true;
	}

	/** Argument must satisfy a boolean condition expected by a system rule. */
	public final boolean check(SystemRule rule, boolean expression) throws RuntimeException {
		if (! expression) throw createException(rule);
		return true;
	}

	/** Argument(s) must be equal to expected value. */
	public final boolean equal(int value, int expected) throws RuntimeException {
		if (value != expected) throw createException(String.format("%d must be equal to %d]", Integer.valueOf(value), Integer.valueOf(expected)));
		return true;
	}

	/** Argument(s) must be equal to expected value. */
	public final <T> boolean equal(T value, T expected) throws RuntimeException {
		if (value == expected) return true;
		if (value != expected && (value == null || ! value.equals(expected))) throw createException(String.format("%d must be equal to %d]", Assert.nullToStr(value), Assert.nullToStr(expected)));
		return true;
	}

	/** Argument(s) must be within a range. */
	public final boolean range(int value, int min, int max) throws RuntimeException {
		if (value < min || value > max ) throw createException(String.format("%d must be in range [%d-%d]", Integer.valueOf(value), Integer.valueOf(min), Integer.valueOf(max)));
		return true;
	}

	/** Argument(s) must not be negative ( >= 0). */
	public final boolean notNegative(int value) throws RuntimeException {
		if (value < 0) throw createException(String.format("%d must not be negative", Integer.valueOf(value)));
		return true;
	}

	/** Argument(s) must not be positive ( > 0). */
	public final boolean positive(int value) throws RuntimeException {
		if (value <= 0) throw createException(String.format("%d must be positive", Integer.valueOf(value)));
		return true;
	}
	public final boolean positive(Integer value) throws RuntimeException {
		if (value == null || value.intValue() <= 0) throw createException(String.format("%d must be positive", value));
		return true;
	}
	public final boolean positive(double value) throws RuntimeException {
		if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) throw createException(String.format("%f must be positive", Double.valueOf(value)));
		return true;
	}
	public final boolean positive(Double value) throws RuntimeException {
		if (value == null || value.doubleValue() <= 0.0 || value.isNaN() || value.isInfinite()) throw createException(String.format("%f must be positive", value));
		return true;
	}


	/** Argument(s) must not be <code>null</code>. */
	public final <T> boolean notNull(T argument) throws RuntimeException {
		if (argument == null) throw createException("null");
		return true;
	}

	/** Argument(s) must not be <code>null</code>. */
	public final boolean notNull(Object ... arguments) throws RuntimeException {
		for (Object b : arguments) {
			if (b == null) throw createException("null");
		}
		return true;
	}

	/** Argument(s) must not be <code>null</code> as expected by a system rule. */
	public final <T> boolean notNull(SystemRule rule, T argument) throws RuntimeException {
		if (argument == null) throw createException(rule);
		return true;
	}

	private static Object nullToStr(Object o) {
		if (o == null) return "null";
		return o;
	}
}
