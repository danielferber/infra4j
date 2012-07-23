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
package infra.ilog.opl.modelo;

import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.ilog.opl.ProvedorModelo;

import java.io.IOException;

import static infra.exception.Assert.Argument;



/**
 * Utiliza um texto como fonte de modelo para o OPL.
 */
public class ProvedorModeloString extends AbstractProvedorModelo {
	final String modelo;

	public ProvedorModeloString(String nome, String modelo) {
		super(nome);
		Argument.notNull(modelo);

		this.modelo = modelo;
	}

	@Override
	public String getArquivo(int linhaReportada, int colunaReportada) {
		return String.class.getName();
	}

	@Override
	public String getConteudo() throws IOException {
		return modelo;
	}

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		throw new UnsupportedMethodException();
	}
}
