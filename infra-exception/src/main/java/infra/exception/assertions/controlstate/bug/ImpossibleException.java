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
 * The exception should not have been thrown if code were correct.
 */
public class ImpossibleException extends ImpossibleControlStateException {
	private static final long serialVersionUID = 1L;
	public ImpossibleException(Throwable cause) { super(cause); }
	public ImpossibleException(String message, Throwable cause) { super(message, cause); }

	/** Simply consumes and unsupported exception. */
	public static void consume(Throwable e) {
		e.printStackTrace(System.err);
	}

}
