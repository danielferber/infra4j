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

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Constitui uma forma padrão de reportar exceções.
 * <p/>
 * O método {@link #reportException(PrintStream, Throwable)} imprime uma exceção de forma mais legível.
 * <p>
 * O método {@link #install()} aplica um handler padrão de exceção para toda aplicação. Os métodos {@link #setUncaughtExceptionHandler()} e
 * {@link #setUncaughtExceptionHandler(Thread)} aplicam um handler para uma thread específica.
 * É interessante chamar este método para todas as novas threads criadas pela aplicação.
 * <p>
 * O handler padrão escreve a exceção em {@link System#err} e também em um log específico de exceções não tratadas.
 *
 * @author Daniel Felix Ferber
 *
 */
public class ExceptionService {
	public static final Logger logger = LoggerFactory.getLogger(ExceptionService.class);

	/**
	 * O handler padão de exceções. Imprime imprime em {@link System#err} uma descrição da exceção utilizando {@link #reportException(PrintStream, Throwable)}
	 * . Também registra a exceção em um log específico de exceções não tratadas..
	 */
	private static final Thread.UncaughtExceptionHandler defaultExceptionHandler = new Thread.UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			ExceptionService.reportException(System.err, e);
		}
	};

	/**
	 * Aplica um hander de exceção padronizado para toda a  JVM..
	 */
	public static void install() {
		Thread.setDefaultUncaughtExceptionHandler(ExceptionService.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado a uma thread específica. */
	public static void setUncaughtExceptionHandler(Thread thread) {
		thread.setUncaughtExceptionHandler(ExceptionService.defaultExceptionHandler);
	}

	/** Aplica um hander de exceção padronizado à thread atual.  */
	public static void setUncaughtExceptionHandler() {
		ExceptionService.setUncaughtExceptionHandler(Thread.currentThread());
	}

	/**
	 * Imprime uma descrição da exceção. De preferência, o erro deve ser do tipo {@link MotivoException} ou {@link MotivoRuntimeException}, pois desta forma é
	 * possível apresentar informações mais objetivas ao usuário.
	 *
	 * @param throwable
	 *            Exceção para ser reportada.
	 */
	public static void reportException(PrintStream output, Throwable throwable) {
		ExceptionService.logger.error("Falha durante a execução.", throwable);

		/* For robustness, do not fail if there is a bug within the report. */
		try {
			output.println();
			ExceptionService.writeTitle(output, "FALHA DE EXECUÇÃO", 80);
			output.println();

			ExceptionService.writeLine(output, 80, '/', '-', '\\');
			Throwable t = throwable;
			while (t != null) {
				if (t.getLocalizedMessage() != null) {
					ExceptionService.writeBox(output, t.getLocalizedMessage(), 80, 1);
					ExceptionService.writeLine(output, 80, '+', ' ', '+');
				} else if (t.getMessage() != null) {
					ExceptionService.writeBox(output, t.getMessage(), 80, 1);
					ExceptionService.writeLine(output, 80, '+', ' ', '+');
				}
				if (RichException.class.isInstance(t)) {
					Iterator<Object> reasons = ((RichException)t).getReasons().iterator();
					if (reasons.hasNext()) {
						ExceptionService.writeBox(output, "Reason: "+ExceptionService.iteratorToString(reasons), 80);
					}
					Iterator<Object> operations = ((RichException)t).getOperations().iterator();
					if (operations.hasNext()) {
						ExceptionService.writeBox(output, "Operation: "+ExceptionService.iteratorToString(operations), 80);
					}
					Iterator<Entry<String, Object>> dataItr = ((RichException)t).getData().entrySet().iterator();
					while (dataItr.hasNext()) {
						Entry<String, Object> data = dataItr.next();
						ExceptionService.writeBox(output, data.getKey()+": "+data.getValue(), 80);
					}
				} else if (RichRuntimeException.class.isInstance(t)) {
					Iterator<Object> reasons = ((RichRuntimeException)t).getReasons().iterator();
					if (reasons.hasNext()) {
						ExceptionService.writeBox(output, "Reason: "+ExceptionService.iteratorToString(reasons), 80);
					}
					Iterator<Object> operations = ((RichRuntimeException)t).getOperations().iterator();
					if (operations.hasNext()) {
						ExceptionService.writeBox(output, "Operation: "+ExceptionService.iteratorToString(operations), 80);
					}
					Iterator<Entry<String, Object>> dataItr = ((RichRuntimeException)t).getData().entrySet().iterator();
					while (dataItr.hasNext()) {
						Entry<String, Object> data = dataItr.next();
						ExceptionService.writeBox(output, data.getKey()+": "+data.getValue(), 80);
					}
//				} else if (IllegalControlStateException.class.isInstance(t)) {
//					ExceptionService.caixa(output, "Violação de integridade da execução.", 80);
//				} else if (IllegalDataStateException.class.isInstance(t)) {
//					ExceptionService.caixa(output, "Violação de integridade de dados.", 80);
				} else if (RuntimeException.class.isInstance(t)) {
					ExceptionService.writeBox(output, "Erro de execução. "+t.getClass().getName(), 80);
				} else {
					ExceptionService.writeBox(output, "Tipo: "+t.getClass().getName(), 80, 0);
				}
				t = t.getCause();
				if (t != null) {
					ExceptionService.writeLine(output, 80, '+', '-', '+');
				}
			}
			ExceptionService.writeLine(output, 80, '\\', '-', '/');

			output.println();
			output.println("Rota até a falha: ");
			throwable.printStackTrace(output);
			output.println();
			output.flush();
		} catch (Exception e) {
			ExceptionService.logger.error("Falha ao imprimir relatório de erro.", e);
		}
	}

	private static String iteratorToString(Iterator<Object> objects) {
		StringBuilder sb = new StringBuilder();
		while (objects.hasNext()) {
			Object object = objects.next();
			sb.append(object);
			if (objects.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	private static void writeChars(PrintStream output, char c, int count) {
		for (int i = 0; i < count; i++) {
			output.print(c);
		}
	}

	private static void writeLine(PrintStream output, int width, char l, char c, char r) {
		output.print(l);
		ExceptionService.writeChars(output, c, width-2);
		output.print(r);
		output.println();
	}

	private static void writeTitle(PrintStream output, String mensagem, int width) {
		ExceptionService.writeChars(output, '*', width);
		output.println();
		ExceptionService.writeChars(output, '*', 3);
		int paddingL = (width-3-3-mensagem.length()) / 2;
		int paddingR = width-3-3-paddingL-mensagem.length();
		ExceptionService.writeChars(output, ' ', paddingL);
		output.print(mensagem);
		ExceptionService.writeChars(output, ' ', paddingR);
		ExceptionService.writeChars(output, '*', 3);
		output.println();
		ExceptionService.writeChars(output, '*', width);
		output.println();
	}

	private static void writeBox(PrintStream output, String str, int width) {
		ExceptionService.writeBox(output, str, width, 0);
	}

	private static void writeBox(PrintStream output, String str, int width, int align) {
		String leftStr = "|>";
		String rightStr = "<|";
		int len = str.length();
		int start = 0;
		int wrapLength = width - leftStr.length() - rightStr.length();

		/*
		 * start: Position of the first (non white space) char of the line, if any.
		 * end: Position one after the last (non white space) char of the line. This position might be one after the last char of the string.
		 */
		while (start < len) {
			if (Character.isWhitespace(str.charAt(start))) {
				start++;
				continue;
			}
			/* Here, start parks over the first char of the line. Trailing spaces were skipped before. */

			int end = start + wrapLength;
			if (end > len) {
				end = len;
			} else if (end < len) {
				while (end > start && ! Character.isWhitespace(str.charAt(end))) end--;
			}
			/* If the remaining line is shorter then the width, the end parks one after the remaining width.
			 * If the remaining line equals the width, the end parks one after the remaining width.
			 * If there is a space within the line, then end parks at the first white char from right to left.
			 * This first white char might be one after the width, if the word happens to terminate exatcly at the availble width. */

			if (end == start) {
				/* Line longer than width. */
				end = start + wrapLength;
				while (end < len && ! Character.isWhitespace(str.charAt(end))) end++;
				/* Here, end stops after the last char of the long line or after the end of the string. */
				String substring = str.substring(start, end);
				output.print(leftStr);
				output.println(substring);
			} else {
				/* Line shorter than width. */
				while (Character.isWhitespace(str.charAt(end-1))) end--;
				/* Here, end stops after the last char of the shortline or after the end of the string. */
				String substring = str.substring(start, end);
				output.print(leftStr);
				int padL = 0;
				if (align == 1) {
					padL = (wrapLength-substring.length()) / 2;
				} else if (align == 2) {
					padL = wrapLength-substring.length();
				}
				int padR = wrapLength - substring.length() - padL;
				ExceptionService.writeChars(output, ' ', padL);
				output.print(substring);
				ExceptionService.writeChars(output, ' ', padR);
				output.println(rightStr);
			}
			start = end;
		}
	}

	/* Some test cases. */
	public static void main(String[] args) {
		ExceptionService.writeBox(System.out, "aaaaa", 25);
		ExceptionService.writeBox(System.out, "   bbbbb", 25);
		ExceptionService.writeBox(System.out, "ccccc   ", 25);
		ExceptionService.writeBox(System.out, "   eeeee   ", 25);
		for (int i = 10; i < 40; i++) {
			System.out.println(i);
			ExceptionService.writeBox(System.out, "aaaaa bbbbb ccccc ddddd eeeee", i+4);
		}
		for (int i = 10; i < 40; i++) {
			System.out.println(i);
			ExceptionService.writeBox(System.out, "aaaaa bbbbb     ccccc ddddd eeeee", i+4);
		}
		for (int i = 10; i < 40; i++) {
			System.out.println(i);
			ExceptionService.writeBox(System.out, "aaaaa bbbbb  cccccccccccccc ddddd eeeee", i+4);
		}
		for (int i = 10; i < 50; i++) {
			System.out.println(i);
			ExceptionService.writeBox(System.out, "aaaaaaaaaaaaaa bbbbb  ccccc ddddd eeeee", i+4);
		}
		for (int i = 10; i < 50; i++) {
			System.out.println(i);
			ExceptionService.writeBox(System.out, "aaaaa bbbbb  cccccccccccccc ddddd eeeeeeeeeeeeee", i+4);
		}
	}
}
