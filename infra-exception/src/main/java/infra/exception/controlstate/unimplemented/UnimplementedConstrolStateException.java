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
package infra.exception.controlstate.unimplemented;

import infra.exception.controlstate.IllegalControlStateException;

/**
 * Marks a code point that has not yet been implemented. Signals that there is
 * pending work of design or programming to be done.
 * <p>
 * This is a generic mark. There are more specific marks that should be
 * preferred:
 * <ul>
 * <li> {@link UnimplementedConditionException}
 * <li> {@link UnimplementedException}
 * <li> {@link UnimplementedMethodException}
 * </ul>
 * <p>Example:
 * <pre>
 * while (true) {
 *   bla bla bla (no break anywhere);
 * }
 * // One does not know how to implement this algorithm after the loop.
 * throw new UnimplementedConstrolStateException();
 * </pre>
 * @author Daniel Felix Ferber
 */
public class UnimplementedConstrolStateException extends IllegalControlStateException {
	private static final long serialVersionUID = 1L;
	public UnimplementedConstrolStateException() { super(); }
	public UnimplementedConstrolStateException(String message, Throwable cause) { super(message, cause); }
	protected UnimplementedConstrolStateException(String message) { super(message); }
	protected UnimplementedConstrolStateException(Throwable cause) { super(cause); }
}
