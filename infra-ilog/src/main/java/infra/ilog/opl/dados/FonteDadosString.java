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

import ilog.opl.IloOplDataSource;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;

import java.io.IOException;

/**
 * Utiliza um texto como fonte de dados para o OPL.
 */
public class FonteDadosString extends AbstractFonteDados {
	private final CharSequence dados;

	/**
	 * Cria o DataSource associado a um texto.
	 * @param nome
	 * @param caminhoArquivo
	 */
	public FonteDadosString(String nome, String dados) {
		super(nome);
		this.dados = dados;
	}

	@Override
	public void importar(IloOplModel oplModel) throws IOException {
		IloOplFactory oplFactory = IloOplFactory.getOplFactoryFrom(oplModel);
		IloOplDataSource oplDataSource = oplFactory.createOplDataSourceFromString(dados.toString(), nome);
		oplModel.addDataSource(oplDataSource);
	}
}
