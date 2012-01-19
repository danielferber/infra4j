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
package ilog.opl.dados;

import ilog.opl.IloOplModel;
import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.NullArgumentException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


/**
 * Utiliza um arquivo do classpath como fonte de dados para o OPL.
 * Gerencia o ciclo de vida do arquivo, ou seja, garante que seja fechado após seu uso.
 * O arquivo será aberto somente quando o OPL precisar de dados.
 * O construtor assume que o arquivo existe previamente, pois o classpath é
 * um repositório imutável e não tem como um arquivo ser adicionado no futuro.
 */
public class FonteDadosClasspath extends AbstractFonteDadosStream {
	private final URL caminhoArquivo;

	/**
	 * Cria o DataSource associado a um arquivo do classpath.
	 * O arquivo precisa existir, caso contrário é considerado uma inconsistência.
	 * @param nome
	 * @param caminhoArquivo
	 */
	public FonteDadosClasspath(String nome, String caminhoArquivo) {
		super(nome);
		NullArgumentException.apply(caminhoArquivo);
		this.caminhoArquivo = this.getClass().getResource(caminhoArquivo);
		IllegalArgumentException.apply(this.caminhoArquivo != null);
	}

	@Override
	public void importar(IloOplModel oplModel) throws IOException {
		InputStream is = caminhoArquivo.openStream();
		super.agendarStream(oplModel, is);
		super.agendarFechamentoStream(oplModel, is);
	}
}
