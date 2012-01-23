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
package infra.ilog.opl;

import java.io.IOException;

/**
 * Provê um modelo OPL na forma de texto.
 * Se necessário, o provedor poderá criar um modelo 'definitivo' aplicando transformações sobre o(s) modelo(s) 'originaisl'.
 * Mas no caso mais comum, o modelo 'definitivo' será simplesmente igual ao 'original'.
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional;
 */
public interface ProvedorModelo {
	/** O nome do modelo. Apenas para efeitos de log na execução. */
	String getNome();
	/** O texto do modelo 'definitivo'. */
	String getConteudo() throws IOException;

	/** Traduz a linha do modelo 'definitivo' para a linha do modelo 'original' . */
	int getLinha(int linhaReportada, int colunaReportada);
	/** Traduz a coluna do modelo 'definitivo' para a linha do modelo 'original' . */
	int getColuna(int linhaReportada, int colunaReportada);
	/** Traduz a linha/coluna do modelo 'definitivo' para o arquivo que define o modelo 'original' . */
	String getArquivo(int linhaReportada, int colunaReportada);
	ProvedorModelo getProvedor(String caminhoRelativo);
}
