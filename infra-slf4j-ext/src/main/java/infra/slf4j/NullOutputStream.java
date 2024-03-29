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
package infra.slf4j;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Outputstream que descarta todo conteúdo.
 * <p>
 * É utilizado para otimizar {@link LoggerOutputStream} quando a prioridade do logger encapsulado não permite escrever o
 * conteúdo no logger.
 *
 * @author Daniel Felix Ferber
 *
 */
class NullOutputStream extends OutputStream {
	@Override
	public void write(int b) throws IOException {
		// Ingora todo conteúdo.
	}

	@Override
	public void write(byte[] b) throws IOException {
		// Ingora todo conteúdo.
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		// Ingora todo conteúdo.
	}
}
