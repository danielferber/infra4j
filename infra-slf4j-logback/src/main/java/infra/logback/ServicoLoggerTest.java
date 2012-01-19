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
package infra.logback;

import infra.exception.motivo.MotivoException;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServicoLoggerTest {
	public static void main(String[] args) {
		try {
			ServicoLogback.instalar();
			ServicoLogback.reconfigurar(new File("config/logback.cfg.xml"));
		} catch (MotivoException e) {
			e.printStackTrace();
			return;
		}
		if (ServicoLogback.isUsandoConfiguracaoClasspath()) {
			System.out.println("Usando configuração do classpath.");
		} else if (ServicoLogback.isUsandoConfiguracaoEspecifica()) {
			System.out.println("Usando configuração do diretório de config.");
		} else {
			System.out.println("Usando configuração padrão.");
		}
		Logger logger = LoggerFactory.getLogger("teste");
		logger.debug("oi");
		logger.info("ola");
	}
}
