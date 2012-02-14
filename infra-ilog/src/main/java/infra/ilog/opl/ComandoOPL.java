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
import infra.exception.assertions.controlstate.unimplemented.UnhandledException;
import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.IllegalAttributeException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;

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
	public final Logger logger;
	public final Logger loggerMeter;
	public final Logger loggerExecucao;
	public final Logger loggerDados;
//	public final Logger loggerModelo;
	public final Logger loggerSolucao;
	public final Logger loggerExternalData;
	public final Logger loggerInternalData;

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

		this.logger = LoggerFactory.getLogger(LoggerFactory.getLogger("ilog.opl"), configuracao.getNome());
		this.loggerMeter = LoggerFactory.getLogger(this.logger, "meter");
		this.loggerExecucao = LoggerFactory.getLogger(this.logger, "execucao");
		this.loggerDados = LoggerFactory.getLogger(this.logger, "dados");
//		this.loggerModelo = LoggerFactory.getLogger(this.loggerDados, "modelo");
		this.loggerSolucao = LoggerFactory.getLogger(this.loggerDados, "solucao");
		this.loggerExternalData = LoggerFactory.getLogger(this.loggerDados, "externo");
		this.loggerInternalData = LoggerFactory.getLogger(this.loggerDados, "internos");
	}

	/** Executa o resolvedor OPL. */
	public void executar() throws MotivoException {
		assert IllegalAttributeException.apply(this.configuracao != null);
		assert IllegalAttributeException.apply(this.comandoResolvedor != null);
		assert IllegalAttributeException.apply(this.oplModel != null);

		IllegalAttributeException.apply(oplModel.hasCplex());
		IllegalAttributeException.apply(oplModel.isGenerated());

		Meter op = MeterFactory.getMeter(loggerMeter, "executarOpl").setMessage("Executar OPL").start();
		try {

			/*
			 * TODO Só faz sentido executar se não existe um main no modelo opl?
			 * O comando OPL poderia ignorar o main e executar o modelo.
			 * Ou chamar diretamente o main.
			 */

			/*
			 * TODO Aparentemente, não é possível obter o modelo a partir do IloModel ou IloModelDefinition.
			 * Se for possível, então é interessante jogar no log também, para manter simetria com os
			 * loggers de solução, dados internos e dados externos.
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
			op.ok();
		} catch (MotivoException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			op.fail(e);
			throw new UnhandledException(e);
		}
	}

	protected static void assureDiretoryForFile(File file) throws IOException {
		if (! file.getParentFile().exists()) {
			if (! file.getParentFile().mkdirs()) {
				throw new IOException(String.format("Failed to create directory '%s'.", file.getParentFile().getAbsolutePath()));
			}
		}
	}

	/**
	 * Registra no log uma cópia dos dados 'externos', que são os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java.
	 * Este arquivo pode ser usado pelo OPL Studio como arquivo .DAT para reproduzir a execução do modelo.
	 */
	protected void salvarDadosExternos(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalArgumentException.apply(caminho.isAbsolute());
		assert IllegalAttributeException.apply(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printExternalData(os);
			os.close();
			loggerExecucao.info("Cópia dos dados externos salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia dos dados externos em {}.", caminho.getAbsolutePath(), e);
		}
	}

	/**
	 * Registra no log uma cópia dos dados da solução.
	 * Este arquivo pode ser comparado com a solução reproduzida do modelo no CPLEX Studio.
	 */
	protected void salvarSolucao(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalArgumentException.apply(caminho.isAbsolute());
		assert IllegalAttributeException.apply(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printSolution(os);
			os.close();
			loggerExecucao.info("Cópia da solução salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia da solução em {}.", caminho.getAbsolutePath(), e);
		}
	}

	/**
	 * Escreve no arquivo uma cópia dos dados 'internos', que são dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.
	 * Este arquivo pode ser comparado com os dados obtidos com a reprodução do modelo no CPLEX Studio.
	 */
	protected void salvarDadosInternos(File caminho) {
		assert NullArgumentException.apply(caminho);
		assert IllegalArgumentException.apply(caminho.isAbsolute());
		assert IllegalAttributeException.apply(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printInternalData(os);
			os.close();
			loggerExecucao.info("Cópia dos dados internos salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia dos dados internos em {}.", caminho.getAbsolutePath(), e);
		}
	}

	protected static final String strPropertyPrintPattern = "  - %s = %s%n";

	/** Registra no log propriedades do modelo OPL. */
	protected void logPropriedades() {
		assert IllegalAttributeException.apply(this.oplModel != null);
		IloOplModelDefinition definition = oplModel.getModelDefinition();
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		out.println("Propriedades do modelo OPL:");
		out.format(ComandoOPL.strPropertyPrintPattern, "id", Integer.toString(oplModel.getModelID()));
		out.format(ComandoOPL.strPropertyPrintPattern, "name", oplModel.getName());
		out.format(ComandoOPL.strPropertyPrintPattern, "hasMain", Boolean.toString(definition.hasMain()));
		out.format(ComandoOPL.strPropertyPrintPattern, "hasObjective", Boolean.toString(definition.hasObjective()));
		out.format(ComandoOPL.strPropertyPrintPattern, "isNonLinear", Boolean.toString(definition.isNonLinear()));
		out.format(ComandoOPL.strPropertyPrintPattern, "isNull", Boolean.toString(definition.isNull()));
		out.format(ComandoOPL.strPropertyPrintPattern, "isSimpleObjective", Boolean.toString(definition.isSimpleObjective()));
		out.format(ComandoOPL.strPropertyPrintPattern, "isUsingCP", Boolean.toString(definition.isUsingCP()));
		out.format(ComandoOPL.strPropertyPrintPattern, "isUsingCplex", Boolean.toString(definition.isUsingCplex()));
		out.close();
	}

	/** Registra no log uma cópia dos dados da solução. */
	protected void logSolutionData() {
		if(! loggerSolucao.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerSolucao);
		ps.println();
		this.oplModel.printSolution(ps);
		ps.close();
	}

	/** Registra no log uma cópia dos dados 'internos', que são dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução. */
	protected void logInternalData() {
		if(! loggerExternalData.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerInternalData);
		this.oplModel.printInternalData(ps);
		ps.close();
	}

	/** Registra no log uma cópia dos dados 'externos', que são os dados passados como entrada para o modelo OPL, tipicamente por um arquivo .DAT ou pelos datasources Java. */
	protected void logExternalData() {
		if(! loggerInternalData.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerExternalData);
		this.oplModel.printExternalData(ps);
		ps.close();
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }
}
