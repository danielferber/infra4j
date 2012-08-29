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
package infra.ilog.cplex;

import static infra.exception.Assert.Argument;
import static infra.exception.Assert.Attribute;
import infra.exception.controlstate.design.UnsupportedMethodException;

import java.io.File;


/**
 * Parâmetros que controlam a execução da uma instância de {@link CommandCplex}.
 *
 * TODO: Criar subclasses especícias para cada um dos algoritmos de programação linear.
 *
 * @author Daniel Felix Ferber
 */
public class ConfigurationCplex {
	/**
	 * Cria uma configuração relativa a um caminho base.
	 * @param caminho O caminho base da configuração. Todos os demais caminhos da configuração são relativos a ele. Deve ser absoluto.
	 */
	public ConfigurationCplex(String nome, File caminho) {
		Argument.notNull(nome, caminho);
		Argument.check(caminho.isAbsolute());

		this.caminhoBase = caminho;
		this.nome = nome;
	}

	/**
	 * Cria uma configuração baseada em uma configuração existente.
	 * @param configuracao A configuração da qual serão copiados os atributos.
	 */
	public ConfigurationCplex(ConfigurationCplex configuracao) {
		Argument.notNull(configuracao);
		Argument.notNull(configuracao.nome);
		Argument.notNull(configuracao.caminhoBase);
		Argument.check(configuracao.caminhoBase.isAbsolute());

		this.nome = configuracao.nome;
		this.caminhoBase = configuracao.caminhoBase;
		this.caminhoModeloExportado = configuracao.caminhoModeloExportado;
		this.caminhoParametrosExportado = configuracao.caminhoParametrosExportado;
		this.caminhoSolucaoExportado = configuracao.caminhoSolucaoExportado;
		this.numeroPassosEntreProgresso = configuracao.numeroPassosEntreProgresso;
		this.delegate = configuracao.delegate;
		this.simplexLimiteDeIteracoes = configuracao.simplexLimiteDeIteracoes;
		this.simplexLimiteDeTempo = configuracao.simplexLimiteDeTempo;
	}

	private String nome;
	public String getNome() { return nome; }
	public void setNome(String nome) {
		Argument.notNull(nome);
		this.nome = nome;
	}

	/** O caminho base da configuração. Todos os caminhos são relativos a ele. */
	private File caminhoBase = new File(System.getProperty("user.dir"));
	/** @return O caminho base da configuração. Todos os caminhos são relativos a ele. */
	public File getCaminhoBase() { return caminhoBase; }
	/** @param caminho O caminho base da configuração. Todos os caminhos são relativos a ele. */
	public ConfigurationCplex setCaminhoBase(File caminho) {
		Argument.notNull(caminhoBase);
		Argument.check(caminhoBase.isAbsolute());
		this.caminhoBase = caminho;
		return this;
	}

	/** O caminho do arquivo no qual será salvo o modelo utilizado pelo Cplex (ou null para não salvar). */
	private File caminhoModeloExportado = null;
	/** @return O caminho do arquivo no qual será salvo o modelo utilizado pelo Cplex (ou null para não salvar). */
	public File getCaminhoModeloExportado() { return this.caminhoModeloExportado; }
	/** @param caminho O caminho do arquivo no qual salvo o modelo utilizado pelo Cplex (ou null para não salvar). */
	public ConfigurationCplex setCaminhoModeloExportado(File caminho) { this.caminhoModeloExportado = caminho; return this; }
	/** @return Se foi atribuido um caminho para o arquivo no qual será salvo o modelo utilizado pelo Cplex. */
	public boolean temCaminhoModeloExportado() { return this.caminhoModeloExportado != null; }
	/** @return Caminho do absoluto do arquivo no qual será salvo o modelo utilizado para executar o Cplex. */
	public File getCaminhoAbsolutoModeloExportado() {
		if (caminhoModeloExportado.isAbsolute()) return caminhoModeloExportado;
		Attribute.notNull(this.caminhoBase);
		Attribute.check(this.caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoModeloExportado.getPath());
	}

	/** O caminho do arquivo no qual serão salvos os parâmetros utilizados para executar o Cplex (ou null para não salvar). */
	private File caminhoParametrosExportado = null;
	/** @return O caminho do arquivo no qual serão salvos os parâmetros utilizados para executar o Cplex (ou null para não salvar). */
	public File getCaminhoParametrosExportados() { return this.caminhoParametrosExportado; }
	/** @param caminho O caminho do arquivo no qual serão salvos os parâmetros utilizados para executar o Cplex (ou null para não salvar). */
	public ConfigurationCplex setCaminhoParametrosExportados(File caminho) { this.caminhoParametrosExportado = caminho; return this; }
	/** @return Se foi atribuido um caminho para o arquivo no qual serão salvos os parâmetros utilizados para executar o Cplex. */
	public boolean temCaminhoParametrosExportados() { return this.caminhoParametrosExportado != null; }
	/** @return Caminho do absoluto do arquivo no qual será serão salvos os parâmetros utilizados para executar o Cplex. */
	public File getCaminhoAbsolutoParametrosExportados() {
		if (caminhoParametrosExportado.isAbsolute()) return caminhoParametrosExportado;
		Attribute.notNull(this.caminhoBase);
		Attribute.check(this.caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoParametrosExportado.getPath());
	}

	/** O caminho do arquivo no qual será salva a solução gerada pelo Cplex (ou null para não salvar). */
	private File caminhoSolucaoExportado = null;
	/** @return O caminho do arquivo no qual será salva a solução gerada pelo Cplex (ou null para não salvar). */
	public File getCaminhoSolucaoExportada() { return this.caminhoSolucaoExportado; }
	/** @param caminho O caminho do arquivo no qual será salva a solução gerada pelo Cplex (ou null para não salvar). */
	public ConfigurationCplex setCaminhoSolucaoExportada(File caminho) { this.caminhoSolucaoExportado = caminho; return this; }
	/** @return Se foi atribuido um caminho para o arquivo no qual será salva a solução gerada pelo Cplex. */
	public boolean temCaminhoSolucaoExportada() { return this.caminhoSolucaoExportado != null; }
	/** @return Caminho do absoluto do arquivo no qual será salva a solução gerada pelo Cplex. */
	public File getCaminhoAbsolutoSolucaoExportada() {
		if (caminhoSolucaoExportado.isAbsolute()) return caminhoSolucaoExportado;
		Attribute.notNull(this.caminhoBase);
		Attribute.check(this.caminhoBase.isAbsolute());
		return new File (caminhoBase, caminhoSolucaoExportado.getPath());
	}

	/** Número de interações executadas para reportar progresso. */
	private int numeroPassosEntreProgresso = 10;
	/** @param numero Número de interações executadas para reportar progresso. */
	public void setNumeroPassosEntreProgresso(int numero) {
		Argument.positive(numero);
		this.numeroPassosEntreProgresso = numero;
	}
	/** @return Número de interações executadas para reportar progresso.  */
	public int getNumeroPassosEntreProgresso() { return numeroPassosEntreProgresso; }

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }

	private Integer simplexLimiteDeIteracoes = null;
	public void setSimplexLimiteDeIteracoes(Integer simplexLimiteDeIteracoes) { Argument.positive(simplexLimiteDeIteracoes); this.simplexLimiteDeIteracoes = simplexLimiteDeIteracoes; }
	public Integer getSimplexLimiteDeIteracoes() { return simplexLimiteDeIteracoes; }

	private Double simplexLimiteDeTempo = null;
	public void setSimplexLimiteDeTempo(Double simplexLimiteDeTempo) { Argument.positive(simplexLimiteDeTempo); this.simplexLimiteDeTempo = simplexLimiteDeTempo; }
	public Double getSimplexLimiteDeTempo() { return simplexLimiteDeTempo; }

	private Delegate delegate;
	public void setDelegate(Delegate delegate) { this.delegate = delegate; }
	public Delegate getDelegate() { return delegate; }
}
