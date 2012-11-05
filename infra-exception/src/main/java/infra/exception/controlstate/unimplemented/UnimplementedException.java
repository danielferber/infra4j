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
 * Marks a declared exception that must be handled but is not yet known how to
 * handle. Signals that there is pending work of design or programming to be
 * done.
 * <p>
 * Example:
 * <pre>
 * try {
 * 	// open file, read content
 * } catch (IOException e) {
 * 	// in future, it is intended to provide logic to retry the operation.
 * 	throw new UnimplementedException(&quot;Missing handling for exception.&quot;, e);
 * }
 * </pre>
 *
 * @author Daniel Felix Ferber
 */
public class UnimplementedException extends UnimplementedConstrolStateException {
	private static final long serialVersionUID = 1L;
	public UnimplementedException(Throwable e) { super(e); }
	public UnimplementedException(String message, Throwable e) { super(message, e); }
}
