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

import infra.exception.assertions.controlstate.IllegalControlStateException;
import infra.exception.assertions.datastate.IllegalDataStateException;
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
 * É interessante chamar este método para todas as novas threads criadas pela aplicação.
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
	 * Aplica um hander de exceção padronizado para toda a  JVM..
	 */
	public static void instalar() {
		Thread.setDefaultUncaughtExceptionHandler(ServicoExcecao.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado a uma thread específica. */
	public static void setUncaughtExceptionHandler(Thread thread) {
		thread.setUncaughtExceptionHandler(ServicoExcecao.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado à thread atual.  */
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
		output.println();
		ServicoExcecao.titulo(output, "FALHA DE EXECUÇÃO", 80);
		output.println();

		ServicoExcecao.linha(output, 80, '/', '-', '\\');
		Throwable t = throwable;
		while (t != null) {
			if (t.getLocalizedMessage() != null) {
				ServicoExcecao.caixa(output, t.getLocalizedMessage(), 80, 1);
				ServicoExcecao.linha(output, 80, '+', ' ', '+');
			}
			if (MotivoException.class.isInstance(t)) {
				Motivo motivo = ((MotivoException) t).getMotivo();
				ServicoExcecao.caixa(output, "Operação: "+motivo.getOperacao(), 80);
				ServicoExcecao.caixa(output, "Código: "+MotivoException.codigoMotivo(motivo), 80);
			} else if (IllegalControlStateException.class.isInstance(t)) {
				ServicoExcecao.caixa(output, "Violação de integridade da execução.", 80);
			} else if (IllegalDataStateException.class.isInstance(t)) {
				ServicoExcecao.caixa(output, "Violação de integridade de dados.", 80);
			} else {
				ServicoExcecao.caixa(output, "Tipo: "+t.getClass().getName(), 80, 0);
			}
			t = t.getCause();
			if (t != null) {
				ServicoExcecao.linha(output, 80, '+', '-', '+');
			}

		}
		ServicoExcecao.linha(output, 80, '\\', '-', '/');

		output.println();
		output.println("Rota até a falha: ");
		throwable.printStackTrace(output);
		output.println();
		output.flush();
	}

	private static void printChars(PrintStream output, char c, int count) {
		for (int i = 0; i < count; i++) {
			output.print(c);
		}
	}

	private static void linha(PrintStream output, int width, char l, char c, char r) {
		output.print(l);
		ServicoExcecao.printChars(output, c, width-2);
		output.print(r);
		output.println();
	}

	private static void titulo(PrintStream output, String mensagem, int width) {
		ServicoExcecao.printChars(output, '*', width);
		output.println();
		ServicoExcecao.printChars(output, '*', 3);
		int paddingL = (width-3-3-mensagem.length()) / 2;
		int paddingR = width-3-3-paddingL-mensagem.length();
		ServicoExcecao.printChars(output, ' ', paddingL);
		output.print(mensagem);
		ServicoExcecao.printChars(output, ' ', paddingR);
		ServicoExcecao.printChars(output, '*', 3);
		output.println();
		ServicoExcecao.printChars(output, '*', width);
		output.println();
	}

	private static void caixa(PrintStream output, String str, int width) {
		ServicoExcecao.caixa(output, str, width, 0);
	}

    private static void caixa(PrintStream output, String str, int width, int align) {
        String leftStr = "| ";
        String rightStr = " |";
        int len = str.length();
        int start = 0;
        int wrapLength = width - leftStr.length() - rightStr.length();

        while (start < len) {
        	while (start < len && Character.isWhitespace(str.charAt(start))) start++;
        	if (start >= len) break;

        	int end = start + wrapLength;
        	if (end > len) {
        		end = len;
        	} else {
        		while (end > start && ! Character.isWhitespace(str.charAt(end))) end--;
        	}

			if (end == start) {
            	// no space found, very long line
            	end = start + wrapLength;
            	while (end < len && ! Character.isWhitespace(str.charAt(end))) end++;
                String substring = str.substring(start, end);
            	output.print(leftStr);
            	output.println(substring);
            } else {
            	// normal case
        		while (end > start && Character.isWhitespace(str.charAt(end-1))) end--;
                String substring = str.substring(start, end);
            	output.print(leftStr);
            	int padL = 0;
            	if (align == 1) {
            		padL = (wrapLength-substring.length()) / 2;
            	} else if (align == 2) {
            		padL = wrapLength-substring.length();
            	}
            	int padR = wrapLength - substring.length() - padL;
           		ServicoExcecao.printChars(output, ' ', padL);
            	output.print(substring);
           		ServicoExcecao.printChars(output, ' ', padR);
            	output.println(rightStr);
            }
        	start = end;
        }
    }
}
