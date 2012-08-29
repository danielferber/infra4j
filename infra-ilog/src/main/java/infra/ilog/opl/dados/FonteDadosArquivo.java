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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Utiliza um arquivo do file system como fonte de dados para o OPL.
 * Gerencia o ciclo de vida do arquivo, ou seja, garante que seja fechado após seu uso.
 * O arquivo será aberto somente quando o OPL precisar de dados.
 * Ocorrerá uma {@link FileNotFoundException} caso o arquivo não exista neste momento.
 * O construtor não verifica se o arquivo existe, pois assume que ele poderá ser criado no futuro.
 */
public class FonteDadosArquivo extends AbstractFonteDadosStream {
	private final File caminhoArquivo;

	/**
	 * Cria o DataSource associado a um arquivo do file system.
	 * O arquivo não precisa existir, poderá ser criado no futuro.
	 * @param nome
	 * @param caminhoArquivo
	 */
	public FonteDadosArquivo(String nome, File caminhoArquivo) {
		super(nome);
		Argument.notNull(caminhoArquivo);
		this.caminhoArquivo = caminhoArquivo;
	}

	/**
	 * Implementação do {@link FonteDados}. Adiciona o arquivo na lista de fontes de dados do OPL.
	 */
	@Override
	public void importar(IloOplModel oplModel) throws IOException {
		FileInputStream is = new FileInputStream(caminhoArquivo);
		super.agendarStream(oplModel, is);
		super.agendarFechamentoStream(oplModel, is);
	}
}
