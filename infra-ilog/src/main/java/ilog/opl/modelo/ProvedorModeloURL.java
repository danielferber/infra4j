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
import infra.exception.assertions.controlstate.bug.ImpossibleException;
import infra.exception.assertions.datastate.NullArgumentException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;


public class ProvedorModeloURL extends AbstractProvedorModelo {
	protected final URL url;

	public ProvedorModeloURL(String nome, URL url) {
		super(nome);
		NullArgumentException.apply(url);
		this.url = url;
	}

	@Override
	public String getConteudo() throws IOException {
		InputStream is = url.openStream();
		try {
			String s = IOUtils.toString(is);
			return s;
		} finally {
			is.close();
		}
	}

	@Override
	public String getArquivo(int linhaReportada, int colunaReportada) {
		return url.getPath();
	}

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		try {
			URL url = new URL(this.url, caminhoRelativo);
			return new ProvedorModeloURL(nome+":"+caminhoRelativo, url);
		} catch (MalformedURLException e) {
			throw new ImpossibleException(e);
		}
	}

}
