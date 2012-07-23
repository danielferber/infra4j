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
package infra.ilog.opl.dados;

import static infra.exception.Assert.Argument;
import ilog.opl.IloOplModel;
import infra.ilog.opl.FonteDados;

import java.io.IOException;


public abstract class AbstractFonteDados implements FonteDados {

	protected final String nome;

	public AbstractFonteDados(String nome) {
		super();
		Argument.notNull(nome);

		this.nome = nome;
	}

	@Override
	public String getNome() { return nome; }

	@Override
	public void preparar(IloOplModel oplModel) throws IOException {
		Argument.notNull(oplModel);
	}

	@Override
	public void finalizar(IloOplModel oplModel) throws IOException {
		Argument.notNull(oplModel);
	}

	@Override
	public void definir(IloOplModel oplModel) {
		Argument.notNull(oplModel);
	}
}
