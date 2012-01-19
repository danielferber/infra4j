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
package infra.exception.assertions.controlstate.bug;

import infra.exception.assertions.controlstate.IllegalControlStateException;

/**
 * The control flow reached a state that should not be reachable if the algorithm were correct.
 * Useful to document points of code that an algorithm must never achieve.
 */
public class ImpossibleControlStateException extends IllegalControlStateException {
	private static final long serialVersionUID = 1L;
	public ImpossibleControlStateException() { super(); }
	public ImpossibleControlStateException(String message) { super(message); }
	public ImpossibleControlStateException(String message, Throwable cause) { super(message, cause); }
	public ImpossibleControlStateException(Throwable cause) { super(cause); }

}
