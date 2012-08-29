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
package infra.exception.controlstate.design;

import java.io.IOException;

/**
 * The control flow reached a catch clause for an exception that is known not to be possible.
 * Probably, the assumption that the exception is not possible was wrong. Or the algorithm is not
 * behaving as expected, causing exceptions that were not expected to be thrown. For example,
 * many methods use to declare {@link IOException} for interfaces they implement, but actually
 * never throw exceptions.
 * @author Daniel Felix Ferber
 */
public class UnsupportedException extends UnsupportedConstrolStateException {
	private static final long serialVersionUID = 1L;
	public UnsupportedException(Throwable cause) { super(cause); }
	public UnsupportedException(String message, Throwable cause) { super(message, cause); }

	/** Simply consumes and unsupported exception. */
	public static void consume(Throwable e) {
		e.printStackTrace(System.err);
	}
}
