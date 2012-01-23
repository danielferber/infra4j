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

import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.IllegalAttributeException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;


/**
 * Executa o OPL conforme as configurações.
 * <p>
 * Não é responsabilidade desta classe a geração do modelo, apenas a execução.
 * <p>
 * Isto envolve:
 * <ul>
 * <li>Registrar propriedades e status do modelo OPL em log.
 * <li>Opcionalmente, salvar modelo, dados internos e dados externos em arquivo para depurar com CPLEX Studio.
 * <li>Opcionalmente, salvar a solução em arquivo para revisão manual.
 * </ul>
 * A fazer
 * <ul>
 * <li>Estrutura de callback para personalizar chamada do solucionador (TODO).
 * </ul>
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class ComandoOPL {
	protected static final Logger logger = LoggerFactory.getLogger("ilog.opl");
	protected static final Logger loggerExecucao = LoggerFactory.getLogger(ComandoOPL.logger, "execucao");
	protected static final Logger loggerDados = LoggerFactory.getLogger(ComandoOPL.logger, "dados");
	protected static final Logger loggerModelo = LoggerFactory.getLogger(ComandoOPL.loggerDados, "modelo");
	protected static final Logger loggerSolucao = LoggerFactory.getLogger(ComandoOPL.loggerDados, "solucao");
	protected static final Logger loggerExternalData = LoggerFactory.getLogger(ComandoOPL.loggerDados, "externos");
	protected static final Logger loggerInternalData = LoggerFactory.getLogger(ComandoOPL.loggerDados, "internos");

	/** Configurações de execução para esta instância. */
	private final ConfiguracaoOPL configuracao;
	protected  ConfiguracaoOPL getConfiguracaoOPL() { return configuracao; }

	/** Instância de modelo OPL gerenciada. */
	private final IloOplModel oplModel;
	protected IloOplModel getOplModel() {  return oplModel; }

	/** Instância do comando que executa o resolvedor. */
	private final ComandoSolver comandoResolvedor;
	protected ComandoSolver getComandoResolvedor() { return comandoResolvedor; }

	/** Cria o comando executor a partir de uma instância CPLEX existente. */
	public ComandoOPL(IloOplModel oplModel, ConfiguracaoOPL configuracao, ComandoSolver comandoResolvedor) {
		super();
		assert NullArgumentException.apply(oplModel, configuracao, comandoResolvedor);
		this.oplModel = oplModel;
		this.configuracao = new ConfiguracaoOPL(configuracao);
		this.comandoResolvedor = comandoResolvedor;
	}

	/** Executa o resolvedor OPL. */
	public void executar() throws MotivoException {
		assert IllegalAttributeException.apply(this.configuracao != null);
		assert IllegalAttributeException.apply(this.comandoResolvedor != null);
		assert IllegalAttributeException.apply(this.oplModel != null);
		IllegalAttributeException.apply(oplModel.hasCplex());
		IllegalAttributeException.apply(oplModel.isGenerated());

		/*
		 * TODO Só faz sentido executar se não existe um main no modelo opl?
		 * O comando OPL poderia ignorar o main e executar o modelo.
		 * Ou chamar diretamente o main.
		 */

		/*
		 * Reportar dados gerados ao realizar o modelo.
		 */
		logExternalData();
		logInternalData();
		if (this.configuracao.temCaminhoDadosExternos()) {
			salvarDadosExternos(this.configuracao.getCaminhoDadosExternos());
		}
		if (this.configuracao.temCaminhoDadosInternos()) {
			salvarDadosInternos(this.configuracao.getCaminhoDadosInternos());
		}
		logPropriedades();

		/*
		 * Executar CPLEX.
		 */
		comandoResolvedor.executar();

		/*
		 * Reportar estado final do OPL.
		 */
		logSolutionData();
		if (this.configuracao.temCaminhoSolucao()) {
			salvarSolucao(this.configuracao.getCaminhoAbsolutoDadosSolucao());
		}
	}

	/**
	 * Registra no log uma cópia dos dados 'externos', que são os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java.
	 * Este arquivo pode ser usado pelo OPL Studio como arquivo .DAT para reproduzir a execução do modelo.
	 */
	protected void salvarDadosExternos(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalAttributeException.apply(this.oplModel != null);
		try {
			assert IllegalArgumentException.apply(caminho.isAbsolute());
			if (! caminho.getParentFile().exists()) {
				caminho.getParentFile().mkdirs();
			}
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printExternalData(os);
			os.close();
		} catch (IOException e) {
			ComandoOPL.loggerExecucao.warn("Falha ao salvar dados externos.", e);
		}
	}

	/**
	 * Registra no log uma cópia dos dados da solução.
	 * Este arquivo pode ser comparado com a solução reproduzida do modelo no CPLEX Studio.
	 */
	protected void salvarSolucao(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalAttributeException.apply(this.oplModel != null);
		try {
			assert IllegalArgumentException.apply(caminho.isAbsolute());
			if (! caminho.getParentFile().exists()) {
				caminho.getParentFile().mkdirs();
			}
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printSolution(os);
			os.close();
		} catch (IOException e) {
			ComandoOPL.loggerExecucao.warn("Falha ao salvar dados da solução.", e);
		}
	}

	/**
	 * Escreve no arquivo uma cópia dos dados 'internos', que são dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.
	 * Este arquivo pode ser comparado com os dados obtidos com a reprodução do modelo no CPLEX Studio.
	 */
	protected void salvarDadosInternos(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalAttributeException.apply(this.oplModel != null);
		try {
			assert IllegalArgumentException.apply(caminho.isAbsolute());
			if (! caminho.getParentFile().exists()) {
				caminho.getParentFile().mkdirs();
			}
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printInternalData(os);
			os.close();
		} catch (IOException e) {
			ComandoOPL.loggerExecucao.warn("Falha ao salvar dados internos.", e);
		}
	}

	protected static final String propertyPrintPattern = "  - %s = %s\n";

	/** Registra no log propriedades do modelo OPL. */
	protected void logPropriedades() {
		assert IllegalAttributeException.apply(this.oplModel != null);
		IloOplModelDefinition definition = oplModel.getModelDefinition();
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoOPL.loggerExecucao);
		out.println("Propriedades do modelo OPL:");
		out.format(ComandoOPL.propertyPrintPattern, "id", oplModel.getModelID());
		out.format(ComandoOPL.propertyPrintPattern, "name", oplModel.getName());
		out.format(ComandoOPL.propertyPrintPattern, "hasMain", definition.hasMain());
		out.format(ComandoOPL.propertyPrintPattern, "hasObjective", definition.hasObjective());
		out.format(ComandoOPL.propertyPrintPattern, "isNonLinear", definition.isNonLinear());
		out.format(ComandoOPL.propertyPrintPattern, "isNull", definition.isNull());
		out.format(ComandoOPL.propertyPrintPattern, "isSimpleObjective", definition.isSimpleObjective());
		out.format(ComandoOPL.propertyPrintPattern, "isUsingCP", definition.isUsingCP());
		out.format(ComandoOPL.propertyPrintPattern, "isUsingCplex", definition.isUsingCplex());
		out.close();
	}

	/** Registra no log uma cópia dos dados da solução. */
	protected void logSolutionData() {
		if(! ComandoOPL.loggerSolucao.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(ComandoOPL.loggerSolucao);
		ps.println();
		this.oplModel.printSolution(ps);
		ps.close();
	}

	/** Registra no log uma cópia dos dados 'internos', que são dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução. */
	protected void logInternalData() {
		if(! ComandoOPL.loggerExternalData.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(ComandoOPL.loggerInternalData);
		this.oplModel.printInternalData(ps);
		ps.close();
	}

	/** Registra no log uma cópia dos dados 'externos', que são os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java. */
	protected void logExternalData() {
		if(! ComandoOPL.loggerInternalData.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(ComandoOPL.loggerExternalData);
		this.oplModel.printExternalData(ps);
		ps.close();
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }
}
