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
package infra.exception.assertions.datastate;

/**
 * The argument passed to method is null.
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class NullArgumentException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public NullArgumentException() { super(); }
	public NullArgumentException(String message) { super(message); }

	/** Raises exception any parameter is null. */
	public static boolean apply(Object... values) {
		for (Object value : values) {
			if (value == null) throw new NullArgumentException();
		}
		return true;
	}
}
