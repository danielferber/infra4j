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

import infra.exception.assertions.controlstate.bug.ImpossibleException;
import infra.exception.assertions.datastate.IllegalArgumentDataException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.ilog.opl.ProvedorModelo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;



/**
 * Um provedor de modelo bem simples que simplesmente lê o modelo de um arquivo.
 * @author X7WS
 *
 */
public class ProvedorModeloClasspath extends AbstractProvedorModelo {
	private final URL caminhoArquivo;

	public ProvedorModeloClasspath(String nome, String caminhoArquivo) {
		super(nome);
		NullArgumentException.apply(caminhoArquivo);
		this.caminhoArquivo = this.getClass().getResource(caminhoArquivo);
		IllegalArgumentDataException.apply(this.caminhoArquivo != null);
	}

	@Override
	public String getArquivo(int linhaReportada, int colunaReportada) {
		return caminhoArquivo.getPath();
	}

	@Override
	public String getConteudo() throws IOException {
		InputStream is = caminhoArquivo.openStream();
		if (is == null) throw new FileNotFoundException(caminhoArquivo.toString());
		try {
			String s = IOUtils.toString(is);
			return s;
		} finally {
			is.close();
		}
	}

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		try {
			URL url = new URL(caminhoArquivo, caminhoRelativo);
			return new ProvedorModeloURL(nome+":"+caminhoRelativo, url);
		} catch (MalformedURLException e) {
			throw new ImpossibleException(e);
		}
	}
}
