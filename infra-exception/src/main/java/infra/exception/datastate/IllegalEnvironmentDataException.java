/*
 * Copyright 2012 Daniel Felix Ferber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package infra.exception.datastate;


/**
 * Data that is being consumed is not valid.
 * @author Daniel Felix Ferber
 */
public class IllegalEnvironmentDataException extends IllegalPreConditionException {
	private static final long serialVersionUID = 1L;

	public IllegalEnvironmentDataException() { super(); }
	public IllegalEnvironmentDataException(String message) { super(message); }
	public IllegalEnvironmentDataException(SystemRule sr) { super(sr); }

//	/** Raises exception if condition is false. */
//	public static boolean apply(boolean condition) {
//		if (!condition) throw new IllegalEnvironmentDataException();
//		return true;
//	}
//
//	/** Raises exception if condition is false. */
//	public static boolean apply(SystemRule sr, boolean condition) {
//		if (!condition) throw new IllegalEnvironmentDataException(sr);
//		return true;
//	}
//
//	public static boolean notNull(Object value) {
//		if (value == null) throw new IllegalEnvironmentDataException();
//		return true;
//	}
//
//	public static boolean notNull(SystemRule sr, Object value) {
//		if (value == null) throw new IllegalEnvironmentDataException(sr);
//		return true;
//	}
//
//	/** Raises exception if one condition is false. */
//	public static boolean apply(boolean ... conditions) {
//		for (boolean b : conditions) {
//			if (!b) throw new IllegalEnvironmentDataException();
//		}
//		return true;
//	}
}
