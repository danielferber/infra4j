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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.regex.Pattern;


/**
 * Utiliza um arquivo como consumidor de dados para o OPL.
 * Gerencia o ciclo de vida do arquivo, ou seja, garante que ele seja criado e depois fechado após seu uso.
 * O arquivo será aberto somente quando o OPL precisar escrever dados.
 * Ocorrerá uma {@link FileNotFoundException} caso o diretório do arquivo não puder ser criado.
 * O construtor não verifica se o arquivo existe, pois assume que ele poderá ser criado no futuro.
 */
public class ConsumidorDadosArquivo extends AbstractConsumidorDadosStream {
	private final File caminhoArquivo;

	public ConsumidorDadosArquivo(String nome, File caminhoArquivo, Collection<Pattern> includePattern, Collection<Pattern> excludePattern, EnumSet<Filtro> filtro) {
		super(nome, includePattern, excludePattern, filtro);
		Argument.notNull(caminhoArquivo);
		this.caminhoArquivo = caminhoArquivo;
	}

	public ConsumidorDadosArquivo(String nome, File caminhoArquivo) {
		this(nome, caminhoArquivo, null, null, null);
	}

	@Override
	public void exportar(IloOplModel oplModel) throws IOException {
		File parentDir = caminhoArquivo.getParentFile();
		if (! parentDir.exists()) {
			/* Verifica se o diretório que hospedará o arquivo realmente foi criado. */
			if (! parentDir.mkdirs() || ! parentDir.exists()) throw new FileNotFoundException(parentDir.getAbsolutePath());
		}
		FileOutputStream os = new FileOutputStream(caminhoArquivo);
		try {
			super.exportarStream(oplModel, os);
		} finally {
			os.close();
		}
	}
}
