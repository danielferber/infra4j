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
package infra.exception.controlstate;

import infra.exception.datastate.SystemRule;

/**
 * Common class for errors caused by control flow (execution) achieving invalid
 * state. This exception supplements the standard {@link RuntimeException} by
 * providing a more semantically rich description of the problem.
 *
 * @author Daniel Felix Ferber
 */
public class IllegalControlStateException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private SystemRule systemRule;

	public IllegalControlStateException() { super(); }
	public IllegalControlStateException(String message, Throwable cause) { super(message, cause); }
	public IllegalControlStateException(String message) { super(message); }
	public IllegalControlStateException(Throwable cause) { super(cause); }
	public IllegalControlStateException(SystemRule sr) { super(); this.systemRule = sr; }

	public SystemRule getSystemRule() { return this.systemRule; }
}
