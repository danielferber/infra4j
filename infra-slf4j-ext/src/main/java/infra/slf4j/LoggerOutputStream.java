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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.slf4j.Logger;


/**
 * Outputstream que escreve seu conteúdo para um logger. O conteúdo é escrito no logger ao chamar os métodos
 * {@link #close()} ou {@link #flush()}. Para obter instâncias deste stream, utilize um dos métodos
 * {@link #getDebugOutputStream(Logger)}, {@link #getErrorOutputStream(Logger)}, {@link #getInfoOutputStream(Logger)},
 * {@link #getTraceOutputStream(Logger)} ou {@link #getWarnOutputStream(Logger)} pois não existe construtor público.
 * <p>
 * Deve ser utilizado apenas para escrever conteúdo com tamanho moderado, pois todo conteúdo permanece temporariamente
 * em buffer.
 * <p>
 * Para escrever mensagens formatadas, pode-se encapsular este {@link OutputStream} como {@link PrintStream} ou
 * {@link PrintWriter}.
 * <p>
 * <b>Detalhe de implementação:</b> Não é possível criar instâncias desta classe, pois
 *
 * @author Daniel Felix Ferber
 */
public abstract class LoggerOutputStream extends OutputStream {
	/** Logger para onde será escrito o conteúdo. */
	protected final Logger logger;
	/** Buffer que acumula temporariamente o conteúdo. */
	private final ByteArrayOutputStream os = new ByteArrayOutputStream(0x3FFF);

	/**
	 * Construtor padrão. As instâncias de fato devem implementar {@link #writeToLogger()} de acordo com a prioridade do
	 * logger. Infelizmente, o {@link Logger} não permite representar a prioridade de um logger, de forma que é
	 * necessário criar uma instância específica para a prioridade desejada.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 */
	protected LoggerOutputStream(Logger logger) {
		super();
		this.logger = logger;
	}

	@Override
	public void close() throws IOException {
		os.close();
		writeToLogger();
		super.close();
	}

	@Override
	public void flush() throws IOException {
//		os.flush();
//		writeToLogger();
//		os.reset();
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		os.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	/** Escreve o conteúdo para o logger de acordo com a prioridade desejada. */
	protected abstract void writeToLogger();

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade <code>ERROR</code>.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	//	public static OutputStream getErrorOutputStream(Logger logger) {
	//		if (!logger.isErrorEnabled())
	//			return new NullOutputStream();
	//		return new LoggerOutputStream(logger) {
	//			@Override
	//			protected void writeToLogger() {
	//				logger.error(extractString());
	//			}
	//		};
	//	}

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade <code>INFO</code>.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	//	public static OutputStream getInfoOutputStream(Logger logger) {
	//		if (!logger.isInfoEnabled())
	//			return new NullOutputStream();
	//		return new LoggerOutputStream(logger) {
	//			@Override
	//			protected void writeToLogger() {
	//				logger.info(extractString());
	//			}
	//		};
	//	}

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade <code>DEBUG</code>.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	//	public static OutputStream getDebugOutputStream(Logger logger) {
	//		if (!logger.isDebugEnabled())
	//			return new NullOutputStream();
	//		return new LoggerOutputStream(logger) {
	//			@Override
	//			protected void writeToLogger() {
	//				logger.debug(extractString());
	//			}
	//		};
	//	}

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade <code>TRACE</code>.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	//	public static OutputStream getTraceOutputStream(Logger logger) {
	//		if (!logger.isTraceEnabled())
	//			return new NullOutputStream();
	//		return new LoggerOutputStream(logger) {
	//			@Override
	//			protected void writeToLogger() {
	//				logger.trace(extractString());
	//			}
	//		};
	//	}

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade <code>WARN</code>.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	//	public static OutputStream getWarnOutputStream(Logger logger) {
	//		if (!logger.isWarnEnabled())
	//			return new NullOutputStream();
	//		return new LoggerOutputStream(logger) {
	//			@Override
	//			protected void writeToLogger() {
	//				logger.warn(extractString());
	//			}
	//		};
	//	}

	/**
	 * Obtém uma instância do {@link OutputStream} que escreve o conteúdo no logger com prioridade determinada.
	 *
	 * @param logger
	 *            Logger que receberá o conteúdo.
	 * @return OutputStream que encapsula o logger.
	 */
	public static OutputStream getOutputStream(Logger logger, Level level) {
		switch (level) {
		case DEBUG:
			if (!logger.isDebugEnabled())
				return new NullOutputStream();
			return new LoggerOutputStream(logger) {
				@Override
				protected void writeToLogger() {
					logger.debug(extractString());
				}
			};
		case ERROR:
			if (!logger.isErrorEnabled())
				return new NullOutputStream();
			return new LoggerOutputStream(logger) {
				@Override
				protected void writeToLogger() {
					logger.error(extractString());
				}
			};
		case INFO:
			if (!logger.isInfoEnabled())
				return new NullOutputStream();
			return new LoggerOutputStream(logger) {
				@Override
				protected void writeToLogger() {
					logger.info(extractString());
				}
			};
		case TRACE:
			if (!logger.isTraceEnabled())
				return new NullOutputStream();
			return new LoggerOutputStream(logger) {
				@Override
				protected void writeToLogger() {
					logger.trace(extractString());
				}
			};
		case WARN:
			if (!logger.isWarnEnabled())
				return new NullOutputStream();
			return new LoggerOutputStream(logger) {
				@Override
				protected void writeToLogger() {
					logger.warn(extractString());
				}
			};
		default:
			throw new IllegalArgumentException();
		}

	}

	/** @return O conteúdo acumulado pelo OutputStream. */
	protected String extractString() {
		return os.toString();
	}

	@Override
	public String toString() {
		return os.toString();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object obj) {
		throw new UnsupportedOperationException();
	}
}
