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

import java.io.IOException;
import java.io.InputStream;


/**
 * Utiliza um stream como fonte de dados para o OPL.
 * Não gerencia o ciclo de vida do stream, ou seja, mantém ele aberto após o uso.
 */
public class DataSourceInputStream extends AbstractDataSourceStream {
	private final InputStream inputStream;

	public DataSourceInputStream(String nome, InputStream is) {
		super(nome);
		Argument.notNull(is);
		this.inputStream = is;
	}

	@Override
	public void produceData(IloOplModel oplModel) throws IOException {
		super.agendarStream(oplModel, this.inputStream);
	}
}
