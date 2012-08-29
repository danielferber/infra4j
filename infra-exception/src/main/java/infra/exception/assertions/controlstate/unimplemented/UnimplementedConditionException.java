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
package infra.exception.assertions.controlstate.unimplemented;



/**
 * The current condition not yet been implemented. Or conditional statement (if/switch) has a
 * condition that is not yet handled. There is pending work of design of programming to be done.
 * Or used to indicate that the conditional statement does not yet know how to handle all
 * conditions it is supposed to handle.
 * @author Daniel Felix Ferber
 */
public class UnimplementedConditionException extends UnimplementedConstrolStateException {
	private static final long serialVersionUID = 1L;
	public UnimplementedConditionException() { super(); }
	public UnimplementedConditionException(String message) { super(message); }
}
