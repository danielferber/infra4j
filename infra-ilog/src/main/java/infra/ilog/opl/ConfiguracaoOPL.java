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

import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.datastate.IllegalArgumentDataException;
import infra.exception.assertions.datastate.IllegalDataStateException;
import infra.exception.assertions.datastate.NullArgumentException;

import java.io.File;


/**
 * Parâmetros que controlam o comportamento do {@link FacadeOPL} e como ele realiza a execução do OPL.
 * <p>
 * Detalhes sobre os atributos da configuração:
 * <ul>
 * <li><b>modoDebug</b>: Se o OPL deve gerar informação de debug na tela. A informação será impressa no stdout da JVM.
 * Infelizmente uma limitação do OPL impede de redicionar informação para o log ou para um arquivo. Ao ativar esta
 * opção, o OPL não apagará arquivos temporparios para permitir uma validação manual do conteúdo destes arquivos. O OPL
 * realizará uma avaliação mais rigorosa do modelo e imprimirá avisos sobre desvios como, por exemplo, variáveis não
 * utilizadas.
 * <li><b>usarNomes</b>: Se o OPL deve gerar o modelo CPLEX utilizando os mesmos nomes de variáveis utilizados no modelo
 * OPL. Isto é útil durante o desenvolvimento caso seja necessário avaliar manualmente o modelo CPLEX gerado. uando a
 * aplicação estiver em produção fará sentido desligar esta opção para reduzir o tempo de execução do CPLEX e a
 * quantidade de memória utilizada.
 * <li><b>usarValidacao</b>: Se o OPL deve executar as condições marcadas como 'assert' no modelo OPL. Um modelo OPL
 * robusto deve utilizar o comando 'assert' para validar toda os dados de entrada (dos arquivos .DAT ou datasources
 * Java). Isto é importante durante o desenvolvimento para garantir que os dados gerados por uma aplicação são
 * compatíveis com os esperados pelo modelo. Quando a aplicação estiver em produção fará sentido desligar esta opção,
 * assumindo que os dados serão sempre válidos e reduzindo o tempo de carga dos dados pelo OPL.
 * <li><b>caminhoTmp</b>: Se não for <code>null</code>, então o OPL irá criar arquivos temporários neste diretório, ou
 * seja, um lugar previsível. Combinado com a opção <b>modoDebug</b>, os arquivos temporários será preservados neste
 * diretório após a execução para permitir uma validação manual do conteúdo destes arquivos.
 * <li><b>caminhoModelo</b>: Caminho do arquivo no qual será salvo o modelo utilizado pelo OPL (ou <code>null</code>
 * para não salvar). *
 * <li><b>caminhoDadosExternos</b>: Caminho do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL (ou
 * <code>null</code> para não salvar). São os dados passados como entrada para o modelo OPL, tipicamente por um arquivo
 * .DAT ou pelos datasources Java. Este arquivo pode ser usado pelo OPL Studio como arquivo .DAT para reproduzir a
 * execução do modelo.
 * <li><b>caminhoDadosInternos</b>: Caminho do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL (ou
 * <code>null</code> para não salvar). São os dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.
 * Este arquivo pode ser comparado com os dados obtidos com a reprodução do modelo no CPLEX Studio.
 * <li><b>caminhoDadosSolucao</b>: Caminho do arquivo no qual será salva a solução obtida para o modelo pelo OPL (ou
 * <code>null</code> para não salvar). Este arquivo pode ser comparado com a solução reproduzida do modelo no CPLEX
 * Studio.
 * </ul>
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class ConfiguracaoOPL {

	/**
	 * Cria uma configuração relativa a um caminho base.
	 * @param caminho O caminho base da configuração. Todos os demais caminhos da configuração são relativos a ele. Deve ser absoluto.
	 */
	public ConfiguracaoOPL(String nome, File caminho) {
		assert NullArgumentException.apply(nome, caminho);
		assert IllegalArgumentDataException.apply(caminho.isAbsolute());
		this.nome = nome;
		this.caminhoBase = caminho;
	}

	/**
	 * Cria uma configuração baseada em uma configuração existente.
	 * @param configuracao A configuração da qual serão copiados os atributos.
	 */
	public ConfiguracaoOPL(ConfiguracaoOPL configuracao) {
		assert IllegalArgumentDataException.apply(configuracao.nome != null);
		assert IllegalArgumentDataException.apply(configuracao.caminhoBase != null);
		assert IllegalArgumentDataException.apply(configuracao.caminhoBase.isAbsolute());

		this.nome = configuracao.nome;
		this.modoDebug = configuracao.modoDebug;
		this.usarNomes = configuracao.usarNomes;
		this.usarValidacao = configuracao.usarValidacao;
		this.caminhoBase = configuracao.caminhoBase;
		this.caminhoTmp = configuracao.caminhoTmp;
		this.caminhoModeloOpl = configuracao.caminhoModeloOpl;
		this.caminhoDadosExternosOpl = configuracao.caminhoDadosExternosOpl;
		this.caminhoDadosInternosOpl = configuracao.caminhoDadosInternosOpl;
		this.caminhoDadosSolucaoOpl = configuracao.caminhoDadosSolucaoOpl;
	}

	private String nome;
	public String getNome() { return nome; }
	public void setNome(String nome) { this.nome = nome; }

	/** O caminho base da configuração. Todos os caminhos são relativos a ele. */
	private File caminhoBase = new File(System.getProperty("user.dir"));
	/** @return O caminho base da configuração. Todos os caminhos são relativos a ele. */
	public File getCaminhoBase() { return caminhoBase; }
	/** @param caminho O caminho base da configuração. Todos os caminhos são relativos a ele. */
	public ConfiguracaoOPL setCaminhoBase(File caminho) {
		NullArgumentException.apply(caminho);
		IllegalArgumentDataException.apply(caminho.isAbsolute());

		this.caminhoBase = caminho;
		return this;
	}

	/** Se o OPL deve gerar informação de debug na tela. */
	private boolean modoDebug = true;
	/** @return Se o OPL deve gerar informação de debug na tela. */
	public boolean getModoDebug() { return this.modoDebug; }
	/** @param modoDebug o OPL deve gerar informação de debug na tela. */
	public ConfiguracaoOPL setModoDebug(boolean modoDebug) { this.modoDebug = modoDebug; return this; }

	/** Se o OPL deve gerar o modelo CPLEX utilizando os mesmos nomes de variáveis utilizados no modelo OPL. */
	private boolean usarNomes = true;
	/** @return Se o OPL deve gerar o modelo CPLEX utilizando os mesmos nomes de variáveis utilizados no modelo OPL. */
	public boolean getUsarNomes() { return usarNomes; }
	/** @param usarNomes Se o OPL deve gerar o modelo CPLEX utilizando os mesmos nomes de variáveis utilizados no modelo OPL. */
	public ConfiguracaoOPL setUsarNomes(boolean usarNomes) { this.usarNomes = usarNomes; return this; }

	/** Se o OPL deve executar as condições marcadas como 'assert' no modelo OPL. */
	private boolean usarValidacao = true;
	/** @return Se o OPL deve executar as condições marcadas como 'assert' no modelo OPL. */
	public boolean getUsarValidacao() { return usarValidacao; }
	/** @param usarValidacao Se o OPL deve executar as condições marcadas como 'assert' no modelo OPL. */
	public ConfiguracaoOPL setUsarValidacao(boolean usarValidacao) { this.usarValidacao = usarValidacao; return this; }

	/** Diretório onde o OPL irá criar arquivos temporários (ou <code>null</code> para usar o padrão do OPL). */
	private File caminhoTmp = null;
	/** @return Diretório onde o OPL irá criar arquivos temporários (ou <code>null</code> para usar o padrão do OPL). */
	public File getCaminhoTmp() { return caminhoTmp; }
	/** @param caminhoTmp Diretório onde o OPL irá criar arquivos temporários (ou <code>null</code> para usar o padrão do OPL). */
	public ConfiguracaoOPL setCaminhoTmp(File caminhoTmp) { this.caminhoTmp = caminhoTmp; return this; }
	/** Se foi atribuído o diretório onde o OPL irá criar arquivos temporários. */
	public boolean temCaminhoTmp() { return caminhoTmp != null; }
	/** O caminho absoluto do diretório onde o OPL irá criar arquivos temporários (ou <code>null</code> para usar o padrão do OPL). */
	public File getCaminhoAbsolutoTmp() {
		if (caminhoTmp.isAbsolute()) return caminhoTmp;
		assert IllegalDataStateException.apply(caminhoBase != null, caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoTmp.getPath());
	}

	/** Caminho do arquivo no qual será salvo o modelo utilizado pelo OPL (ou <code>null</code> para não salvar). */
	private File caminhoModeloOpl = null;
	/** @return Caminho do arquivo no qual será salvo o modelo utilizado pelo OPL (ou <code>null</code> para não salvar). */
	public File getCaminhoModeloOpl() { return this.caminhoModeloOpl; }
	/** @param caminho Caminho do arquivo no qual será salvo o modelo utilizado pelo OPL (ou <code>null</code> para não salvar). */
	public ConfiguracaoOPL setCaminhoModeloOpl(File caminho) { this.caminhoModeloOpl = caminho; return this; }
	/** Se foi atribuído o caminho do arquivo no qual será salvo o modelo utilizado pelo OPL. */
	public boolean temCaminhoModeloOpl() { return this.caminhoModeloOpl != null; }
	/** O caminho absoluto do arquivo no qual será salvo o modelo utilizado pelo OPL. */
	public File getCaminhoAbsolutoModeloOpl() {
		if (caminhoModeloOpl.isAbsolute()) return caminhoModeloOpl;
		assert IllegalDataStateException.apply(caminhoBase != null, caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoModeloOpl.getPath());
	}

	/** Caminho do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL (ou code>null</code> para não salvar).
	 * São os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java. */
	private File caminhoDadosExternosOpl = null;
	/** @return Caminho do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL (ou code>null</code> para não salvar).
	 * São os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java. */
	public File getCaminhoDadosExternos() { return this.caminhoDadosExternosOpl; }
	/** @param caminho Caminho do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL (ou code>null</code> para não salvar).
	 * São os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java. */
	public ConfiguracaoOPL setCaminhoDadosExternosOpl(File caminho) { this.caminhoDadosExternosOpl = caminho; return this; }
	/** Se foi atribuído o caminho do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL (ou code>null</code> para não salvar). */
	public boolean temCaminhoDadosExternos() { return this.caminhoDadosExternosOpl != null; }
	/** O caminho absoluto do arquivo no qual serão salvos dados 'externos' do modelo modelo OPL. */
	public File getCaminhoAbsolutoDadosExternosOpl() {
		if (caminhoDadosExternosOpl.isAbsolute()) return caminhoDadosExternosOpl;
		assert IllegalDataStateException.apply(caminhoBase != null, caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoDadosExternosOpl.getPath());
	}

	/** Caminho do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL (ou <code>null</code> para não salvar).
	 * São os dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.*/
	private File caminhoDadosInternosOpl = null;
	/** @return Caminho do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL (ou <code>null</code> para não salvar).
	 * São os dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.*/
	public File getCaminhoDadosInternos() { return this.caminhoDadosInternosOpl; }
	/** @param caminho Caminho do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL (ou <code>null</code> para não salvar).
	 * São os dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.*/
	public ConfiguracaoOPL setCaminhoDadosInternosOpl(File caminho) { this.caminhoDadosInternosOpl = caminho; return this; }
	/** Se foi atribuído o caminho do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL.*/
	public boolean temCaminhoDadosInternos() { return this.caminhoDadosInternosOpl != null; }
	/** Caminho absoluto do arquivo no qual serão salvos dados 'internos' do modelo modelo OPL.*/
	public File getCaminhoAbsolutoDadosInternosOpl() {
		if (caminhoDadosInternosOpl.isAbsolute()) return caminhoDadosInternosOpl;
		assert IllegalDataStateException.apply(caminhoBase != null, caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoDadosInternosOpl.getPath());
	}

	/** Caminho do arquivo no qual será salva a solução obtida para o modelo pelo OPL (ou <code>null</code> para não salvar). */
	private File caminhoDadosSolucaoOpl = null;
	/** @param Caminho do arquivo no qual será salva a solução obtida para o modelo pelo OPL (ou <code>null</code> para não salvar). */
	public File getCaminhoSolucaoOpl() { return this.caminhoDadosSolucaoOpl; }
	/** @param caminho Caminho do arquivo no qual será salva a solução obtida para o modelo pelo OPL (ou <code>null</code> para não salvar). */
	public ConfiguracaoOPL setCaminhoSolucaoOpl(File caminho) { this.caminhoDadosSolucaoOpl = caminho; return this; }
	/** Se foi atribuído o caminho do arquivo no qual será salva a solução obtida para o modelo pelo OPL. */
	public boolean temCaminhoSolucao() { return this.caminhoDadosSolucaoOpl != null; }
	/** O caminho absoluto do arquivo no qual será salva a solução obtida para o modelo pelo OPL. */
	public File getCaminhoAbsolutoDadosSolucao() {
		if (caminhoDadosSolucaoOpl.isAbsolute()) return caminhoDadosSolucaoOpl;
		assert IllegalDataStateException.apply(caminhoBase != null, caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoDadosSolucaoOpl.getPath());
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }

}
