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

import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;
import infra.exception.motivo.MotivoRuntimeException;

import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Constitui uma forma padrão de reportar exceções.
 * <p/>
 * O método {@link #reportarException(PrintStream, Throwable)} imprime uma exceção de forma mais legível.
 * <p>
 * O método {@link #instalar()} aplica um handler padrão de exceção para toda aplicação. Os métodos {@link #setUncaughtExceptionHandler()} e
 * {@link #setUncaughtExceptionHandler(Thread)} aplicam um handler para uma thread específica.
 * <p>
 * O handler padrão escreve a exceção em {@link System#err} e também em um log específico de exceções não tratadas.
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 *
 */
public class ServicoExcecao {
	private static final Logger logger = LoggerFactory.getLogger(ServicoExcecao.class);

	/**
	 * O handler padão de exceções. Imprime imprime em {@link System#err} uma descrição da exceção utilizando {@link #reportarException(PrintStream, Throwable)}
	 * . Também registra a exceção em um log específico de exceções não tratadas..
	 */
	private static final Thread.UncaughtExceptionHandler defaultExceptionHandler = new Thread.UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			ServicoExcecao.reportarException(System.err, e);
			ServicoExcecao.logger.error("Falha durante a execução.", e);
		}
	};

	/**
	 * Aplica as configurações a JVM. Define o hander de exceção padrão da JVM para o hander padrão do {@link ServicoExcecao}.
	 */
	public static void instalar() {
		Thread.setDefaultUncaughtExceptionHandler(ServicoExcecao.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado a uma thread. */
	public static void setUncaughtExceptionHandler(Thread thread) {
		thread.setUncaughtExceptionHandler(ServicoExcecao.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado à thread atual. É interessante chamar este método para todas as novas threads criadas pela aplicação. */
	public static void setUncaughtExceptionHandler() {
		ServicoExcecao.setUncaughtExceptionHandler(Thread.currentThread());
	}

	/**
	 * Imprime uma descrição da exceção. De preferência, o erro deve ser do tipo {@link MotivoException} ou {@link MotivoRuntimeException}, pois desta forma é
	 * possível apresentar informações mais objetivas ao usuário.
	 *
	 * @param throwable
	 *            Exceção para ser reportada.
	 */
	public static void reportarException(PrintStream output, Throwable throwable) {
		output.println("Ocorreu uma falha durante a execução.");

		output.print("Mensagem: ");
		output.println(throwable.getMessage());

		Motivo motivo = null;
		if (throwable instanceof MotivoException) {
			motivo = ((MotivoException) throwable).getMotivo();
		} else if (throwable instanceof MotivoRuntimeException) {
			motivo = ((MotivoRuntimeException) throwable).getMotivo();
		}

		if (motivo != null) {
			output.print("Operação: ");
			output.println(motivo.getOperacao());
			output.print("Código: ");
			output.println(MotivoException.codigoMotivo(motivo));
		} else {
			output.print("Classe: ");
			output.println(throwable.getClass().getName());
		}
		output.println("Rota até a falha: ");
		throwable.printStackTrace(output);
		output.flush();
	}
}
