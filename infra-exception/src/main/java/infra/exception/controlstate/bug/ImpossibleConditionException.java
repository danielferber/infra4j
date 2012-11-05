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
package infra.exception.controlstate.bug;


/**
 * Marks a condition that is not possible. If code is correct, then this
 * condition must not be true.
 * <p>
 * When writing if/then/else or switch statements:
 * <ul>
 * <li>Test explicitly each expected condition.
 * <li>Also test explicitly conditions that are never possible and mark them with
 * {@link ImpossibleConditionException}.
 * <li>Do not trust <code>else</code> or <code>default</code> clauses. Instead, test explicitly the
 * "other" condition. Mark <code>else</code> and <code>default</code> clauses with
 * {@link ImpossibleConditionException}.
 * </ul>
 * <p>
 * Example:
 * <pre>
 * switch (state) {
 *   case A: bla bla bla; break;
 *   case B: bla bla bla; break;
 *   default: throw new ImpossibleConditionException();
 * }
 * </pre>
 * Example:
 * <pre>
 * Object a = null;
 * Object b = null;
 * while (a == null && b == null) {
 *   a = getA();
 *   a = getB();
 * }
 * // here, both a and b are never null!
 * if (a != null && b != null) {
 *   bla bla bla;
 * } else if (a != null) {
 *   bla bla bla;
 * } else if (b != null) {
 *   bla bla bla;
 * } else {
 *   throw new ImpossibleConditionException();
 * }
 * </pre>
 *
 * @author Daniel Felix Ferber
 */
public class ImpossibleConditionException extends ImpossibleControlStateException {
	private static final long serialVersionUID = 1L;
	public ImpossibleConditionException() { super(); }
	public ImpossibleConditionException(String message) { super(message); }
}
