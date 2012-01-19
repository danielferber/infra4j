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


/**
 * The condition should not be possible to be called if code were correct.
 * Useful to protect if/then and switch statements against conditions that should not be possible by the algorithm.
 */
public class ImpossibleConditionException extends ImpossibleControlStateException {
	private static final long serialVersionUID = 1L;
	public ImpossibleConditionException() { super(); }
	public ImpossibleConditionException(String message) { super(message); }
}
