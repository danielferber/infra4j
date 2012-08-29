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

import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;

/**
 * Alternativa ao {@link org.slf4j.LoggerFactory}, com métodos mais específicos e práticos.
 *
 * @author Daniel Felix Ferber
 */
public class LoggerFactory {
	/**
	 * Obtém o logger com hierarquia associada ao nome. Equivalente a {@link org.slf4j.LoggerFactory#getLogger(String)}.
	 * <p>
	 * Usado tipicamente para declarar loggers especiais da aplicação, cujo nome não segue a convenção de nome igual ao
	 * nome 'fully qualified' da classe.
	 *
	 * @param name
	 *            Nome do logger, que é uma hierarquia separada por pontos.
	 * @returns logger Instância do logger.
	 */
	public static Logger getLogger(String name) {
		return org.slf4j.LoggerFactory.getLogger(name);
	}

	/**
	 * Obtém o logger com hierarquia associada com uma determinada classe através do nome 'fully qualified' da classe.
	 * Equivalente a {@link org.slf4j.LoggerFactory#getLogger(Class)}.
	 * <p>
	 * Usado tipicamente para declarar o logger das atividades executadas por uma classe.
	 *
	 * @param name
	 *            Classe.
	 * @returns Instância do logger.
	 */
	public static Logger getLogger(Class<?> clazz) {
		return org.slf4j.LoggerFactory.getLogger(clazz);
	}

	/**
	 * Obtém o logger com hierarquia abaixo da hierarquia associada com uma determinada classe.
	 * <p>
	 * Usado tipicamente para declarar loggers específicos por atividade de uma classe. Desta forma é possível controlar
	 * o log individualmente por atividade. Para cada atividade de interesse é declarado um logger específico dentro da
	 * hierarquia do logger da classe.
	 *
	 * @param name
	 *            Nome da hierarquia abaixo da classe.
	 * @param clazz
	 *            Classe.
	 * @returns Instância do logger.
	 */
	public static Logger getLogger(Class<?> clazz, String name) {
		return org.slf4j.LoggerFactory.getLogger(clazz.getName() + "." + name);
	}

	/**
	 * Obtém o logger com hierarquia abaixo da hierarquia associada com um logger existente.
	 *
	 * @param name
	 *            Nome da hierarquia abaixo da classe.
	 * @param logger
	 *            Logger existente.
	 * @returns Instância do logger.
	 */
	public static Logger getLogger(Logger logger, String name) {
		return org.slf4j.LoggerFactory.getLogger(logger.getName() + "." + name);
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>TRACE</code>.
	 */
	public static PrintStream getTracePrintStream(Logger logger) {
		return new PrintStream(LoggerOutputStream.getOutputStream(logger, Level.TRACE));
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>DEBUG</code>.
	 */
	public static PrintStream getDebugPrintStream(Logger logger) {
		return new PrintStream(LoggerOutputStream.getOutputStream(logger, Level.DEBUG));
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>INFO</code>.
	 */
	public static PrintStream getInfoPrintStream(Logger logger) {
		return new PrintStream(LoggerOutputStream.getOutputStream(logger, Level.INFO));
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>WARN</code>.
	 */
	public static PrintStream getWarnPrintStream(Logger logger) {
		return new PrintStream(LoggerOutputStream.getOutputStream(logger, Level.WARN));
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>ERROR</code>.
	 */
	public static PrintStream getErrorPrintStream(Logger logger) {
		return new PrintStream(LoggerOutputStream.getOutputStream(logger, Level.ERROR));
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>INFO</code>.
	 */
	public static OutputStream getInfoOutputStream(Logger logger) {
		return LoggerOutputStream.getOutputStream(logger, Level.INFO);
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>WARN</code>.
	 */
	public static OutputStream getWarnOutputStream(Logger logger) {
		return LoggerOutputStream.getOutputStream(logger, Level.WARN);
	}

	/**
	 * Obtém um {@link PrintStream} cujo conteúdo será redicionado para um logger, com prioridade <code>ERROR</code>.
	 */
	public static OutputStream getErrorOutputStream(Logger logger) {
		return LoggerOutputStream.getOutputStream(logger, Level.ERROR);
	}

}
