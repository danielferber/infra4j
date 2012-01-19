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
package ilog.opl.modelo;

import ilog.opl.ProvedorModelo;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedMethodException;
import infra.exception.assertions.datastate.NullArgumentException;

public abstract class AbstractProvedorModelo implements ProvedorModelo {

	protected final String nome;

	public AbstractProvedorModelo(String nome) {
		super();
		NullArgumentException.apply(nome);
		this.nome = nome;
	}

	@Override
	public int getLinha(int linhaReportada, int colunaReportada) { return linhaReportada; }

	@Override
	public int getColuna(int linhaReportada, int colunaReportada) { return colunaReportada; }

	@Override
	public String getNome() { return nome; }

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		throw new UnimplementedMethodException();
	}
}
