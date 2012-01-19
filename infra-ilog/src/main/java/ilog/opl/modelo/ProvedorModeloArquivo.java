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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;



/**
 * Utiliza um arquivo do file system como fonte de modelo para o OPL.
 * Gerencia o ciclo de vida do arquivo, ou seja, garante que seja fechado após seu uso.
 * O arquivo será aberto somente quando o OPL precisar do modelo.
 * Ocorrerá uma {@link FileNotFoundException} caso o arquivo não exista neste momento.
 * O construtor não verifica se o arquivo existe, pois assume que ele poderá ser criado no futuro.
 */
public class ProvedorModeloArquivo extends AbstractProvedorModelo {
	final File caminhoArquivo;

	public ProvedorModeloArquivo(String nome, File caminhoArquivo) {
		super(nome);
		this.caminhoArquivo = caminhoArquivo;
	}

	@Override
	public String getArquivo(int linhaReportada, int colunaReportada) {
		return caminhoArquivo.getAbsolutePath();
	}

	@Override
	public String getConteudo() throws IOException {
		FileInputStream is = new FileInputStream(caminhoArquivo);
		try {
			String s = IOUtils.toString(is);
			return s;
		} finally {
			is.close();
		}
	}

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		return new ProvedorModeloArquivo(nome+":"+caminhoRelativo, new File(caminhoArquivo.getParentFile(), caminhoRelativo));
	}
}
