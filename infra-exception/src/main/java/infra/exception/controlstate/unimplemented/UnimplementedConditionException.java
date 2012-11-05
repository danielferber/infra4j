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



/**
 * Marks a condition that has not yet been implemented in a <code>if</code> or <code>switch</code> statement.
 * Signals that there is pending work of design or programming to be done.
 * <p>
 * Example:
 * <pre>
 * switch (state) {
 *   case A: bla bla bla; break;
 *   case B: throw new UnimplementedConditionException("Condition for state 'B' not implemented.");
 *   default: bla bla bla;
 * }
 * </pre>
 * Example:
 * <pre>
 * if (a != null && b != null) {
 *   bla bla bla;
 * } else if (a != null) {
 *   throw new UnimplementedConditionException("Condition for 'a != null && b ==null' not implemented.");
 * } else if (b != null) {
 *   bla bla bla;
 * } else {
 *   throw new ImpossibleConditionException();
 * }
 * </pre>
 * @author Daniel Felix Ferber
 */
public class UnimplementedConditionException extends UnimplementedConstrolStateException {
	private static final long serialVersionUID = 1L;
	public UnimplementedConditionException() { super(); }
	public UnimplementedConditionException(String message) { super(message); }
}
