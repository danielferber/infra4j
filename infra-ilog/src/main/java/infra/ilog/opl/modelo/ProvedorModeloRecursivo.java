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

import infra.ilog.opl.ProvedorModelo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class ProvedorModeloRecursivo extends AbstractProvedorModelo {
	private final ProvedorModelo provedorRaiz;

	public ProvedorModeloRecursivo(ProvedorModelo provedorRaiz) {
		super(provedorRaiz.getNome());
		this.provedorRaiz = provedorRaiz;
	}

	@Override
	public String getConteudo() throws IOException {
		String linhas = obterLinhasRecursivo(provedorRaiz);
		return linhas;
	}

	@Override
	public int getLinha(int linhaReportada, int colunaReportada) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getColuna(int linhaReportada, int colunaReportada) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getArquivo(int linhaReportada, int colunaReportada) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProvedorModelo getProvedor(String caminhoRelativo) {
		// TODO Auto-generated method stub
		return null;
	}

	protected String obterLinhasRecursivo(ProvedorModelo provedorRaiz) throws IOException {
		String conteudo = provedorRaiz.getConteudo();
		boolean comentario = false;
		int contadorLinha = 0;
		StringBuilder resultado = new StringBuilder();
		Scanner scanner = new Scanner(conteudo);
		while (scanner.hasNextLine()) {
			String linha = scanner.nextLine();
			contadorLinha++;

			/* Se é uma de comentário ou de código diferente de "include". */
			String linha2 = linha.trim();
			if (! linha2.startsWith("include")) {
				if (comentario) {
					resultado.append(String.format(" *%s:%3d* %s", provedorRaiz.getNome(), contadorLinha, linha));
					resultado.append('\n');
				} else {
					resultado.append(String.format("/*%s:%3d*/%s", provedorRaiz.getNome(), contadorLinha, linha));
					resultado.append('\n');
				}
				comentario = alternaComentario(comentario, linha);
				continue;
			}
			/* Se é uma linha comentada com "include", então  continua coma próxima. */
			if (comentario) {
				comentario = alternaComentario(comentario, linha);
				continue;
			}
			/* É uma linha que não está comentada e começa com "include". */
			int start = linha2.indexOf('"');
			if (start == -1) throw new IOException(String.format("Include inválido, %s, linha %d", provedorRaiz.getArquivo(contadorLinha, start), provedorRaiz.getLinha(contadorLinha, start)));
			start++;
			int end = linha2.indexOf('"', start);
			if (end == -1) throw new IOException(String.format("Include inválido, %s, linha %d", provedorRaiz.getArquivo(contadorLinha, start), provedorRaiz.getLinha(contadorLinha, start)));
			String caminhoRelativo = linha2.substring(start, end);
			ProvedorModelo provedorRecursivo = provedorRaiz.getProvedor(caminhoRelativo);
			String linhas3 = obterLinhasRecursivo(provedorRecursivo);
			resultado.append(linhas3);
			resultado.append('\n');
		}
		return resultado.toString();
	}

	protected List<String> readLines(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		List<String> list = new ArrayList<String>();
		String line = reader.readLine();
		while (line != null) {
			list.add(line);
			line = reader.readLine();
		}
		return list;
	}

	protected boolean alternaComentario(boolean comentario, String linha) {
		int p = 0;
		int l = linha.length();
		while (p < l) {
			if (comentario) {
				int pos = linha.indexOf("*/", p);
				if (pos == -1) {
					break;
				}
				p = pos+2;
				comentario = false;
			} else {
				int pos = linha.indexOf("/*", p);
				if (pos == -1) {
					break;
				}
				p = pos+2;
				comentario = true;
			}
		}
		return comentario;
	}

}
