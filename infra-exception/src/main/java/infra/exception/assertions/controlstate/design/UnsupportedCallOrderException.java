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
package infra.exception.assertions.controlstate.design;

/**
 * The method or operation was not intended to be called in arbitrary order relative to other methods or operations, although it is available from the interface.
 * A better interface design should not provide methods that are not intended to be called.
 * However, the interface may have been designed by third party or you may be overriding
 * the interface for a purpose that was not originally designed.
 * @author Daniel Felix Ferber
 */
public class UnsupportedCallOrderException extends UnsupportedConstrolStateException {
	private static final long serialVersionUID = 1L;
	public UnsupportedCallOrderException() { super(); }
	public UnsupportedCallOrderException(String message) { super(message); }
}
