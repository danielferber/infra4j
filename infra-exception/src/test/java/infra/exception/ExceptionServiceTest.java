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
package infra.exception;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ExceptionServiceTest {

	public enum MotivoA {
		ARQUIVO("O arquivo não existe."),
		DIRETORIO("O diretório não existe."),
		;
		public final String message;
		private MotivoA(String message) { this.message = message; }
		public String getMensagem() { return this.message; }
	}

	public static void main(String[] args) {
		try {
			try {
				throw new FileNotFoundException("O arquivo não foi encontrado!");
			} catch (IOException e) {
				throw new RuntimeException("Problemas na execução.", e);
			}
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
		}
		try {
			try {
				throw new FileNotFoundException("O arquivo não foi encontrado!");
			} catch (IOException e) {
				throw new RichException(MotivoA.ARQUIVO, e);
			}
		} catch (Exception e) {
			ExceptionService.reportException(System.err, e);
		}
		ExceptionService.install();
		throw new RuntimeException("Um outro erro...");
	}
}
