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

import static infra.exception.Assert.Argument;
import static infra.exception.Assert.Attribute;
import static infra.exception.Assert.Invariant;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import infra.exception.RichRuntimeException;
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.ilog.SolverCommand;
import infra.ilog.NoSolutionException;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;
import infra.slf4j.Operation;
import infra.slf4j.OperationFactory;

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
	public final Logger loggerExecution;
	public final Logger loggerData;
//	public final Logger loggerModelo;
	public final Logger loggerSolution;
	public final Logger loggerExternalData;
	public final Logger loggerInternalData;

	/** Configuration that guides the OPL execution. */
	private final ConfiguracaoOPL configuracao;
	/** @return Configuration that guides the OPL execution. */
	protected  ConfiguracaoOPL getConfiguracaoOPL() { return configuracao; }

	/** OPL instance being guided. */
	private final IloOplModel oplModel;
	/** @return OPL instance being guided. */
	protected IloOplModel getOplModel() {  return oplModel; }

	/** Solver command that guides the execution of the CPLEX or CP instance. */
	private final SolverCommand solverCommand;
	/** @return Solver command that guides the execution of the CPLEX or CP instance. */
	protected SolverCommand getComandoResolvedor() { return solverCommand; }

	/** Cria o comando executor a partir de uma instância CPLEX existente. */
	public ComandoOPL(IloOplModel oplModel, ConfiguracaoOPL configuracao, SolverCommand comandoResolvedor) {
		super();
		Argument.notNull(oplModel, configuracao, comandoResolvedor);

		this.oplModel = oplModel;
		this.configuracao = new ConfiguracaoOPL(configuracao);
		Invariant.check(this.configuracao.equals(configuracao));
		this.solverCommand = comandoResolvedor;

		this.logger = LoggerFactory.getLogger(LoggerFactory.getLogger("ilog.opl"), configuracao.getNome());
		this.loggerMeter = LoggerFactory.getLogger(this.logger, "perf");
		this.loggerExecution = LoggerFactory.getLogger(this.logger, "exec");
		this.loggerData = LoggerFactory.getLogger(this.logger, "data");
//		this.loggerModelo = LoggerFactory.getLogger(this.loggerDados, "modelo");
		this.loggerSolution = LoggerFactory.getLogger(this.loggerData, "solution");
		this.loggerExternalData = LoggerFactory.getLogger(this.loggerData, "external");
		this.loggerInternalData = LoggerFactory.getLogger(this.loggerData, "internal");
	}

	private final Operation ExecuteOpl = OperationFactory.getOperation("executeOpl", "Execute OPL");

	/** Executa o resolvedor OPL.
	 * @throws NoSolutionException */
	public void executar() throws NoSolutionException {
		Attribute.check(this.configuracao != null);
		Attribute.check(this.solverCommand != null);
		Attribute.check(this.oplModel != null);

		Attribute.check(oplModel.hasCplex());
		Attribute.check(oplModel.isGenerated());

		Meter op = MeterFactory.getMeter(loggerMeter, ExecuteOpl).start();
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
			solverCommand.execute();

			/*
			 * Reportar estado final do OPL.
			 */
			logSolutionData();
			if (this.configuracao.temCaminhoSolucao()) {
				salvarSolucao(this.configuracao.getCaminhoAbsolutoDadosSolucao());
			}
			op.ok();
		} catch (NoSolutionException e) {
			op.fail(e);
			throw e;
		} catch (RuntimeException e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e, ExecuteOpl);
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
		Argument.notNull(caminho);
		Argument.check(caminho.isAbsolute());
		Attribute.check(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printExternalData(os);
			os.close();
			loggerExecution.info("Cópia dos dados externos salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecution.warn("Falha ao salvar cópia dos dados externos em {}.", caminho.getAbsolutePath(), e);
		}
	}

	/**
	 * Registra no log uma cópia dos dados da solução.
	 * Este arquivo pode ser comparado com a solução reproduzida do modelo no CPLEX Studio.
	 */
	protected void salvarSolucao(File caminho) {
		Argument.notNull(caminho);
		Argument.check(caminho.isAbsolute());
		Attribute.check(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printSolution(os);
			os.close();
			loggerExecution.info("Cópia da solução salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecution.warn("Falha ao salvar cópia da solução em {}.", caminho.getAbsolutePath(), e);
		}
	}

	/**
	 * Escreve no arquivo uma cópia dos dados 'internos', que são dados calculados pelo modelo OPL usado dados 'externos' ou dados da solução.
	 * Este arquivo pode ser comparado com os dados obtidos com a reprodução do modelo no CPLEX Studio.
	 */
	protected void salvarDadosInternos(File caminho) {
		Argument.notNull(caminho);
		Argument.check(caminho.isAbsolute());
		Attribute.check(this.oplModel != null);

		try {
			ComandoOPL.assureDiretoryForFile(caminho);
			OutputStream os = new FileOutputStream(caminho);
			this.oplModel.printInternalData(os);
			os.close();
			loggerExecution.info("Cópia dos dados internos salva em {}.", caminho.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecution.warn("Falha ao salvar cópia dos dados internos em {}.", caminho.getAbsolutePath(), e);
		}
	}

	protected static final String strPropertyPrintPattern = "  - %s = %s%n";

	/** Registra no log propriedades do modelo OPL. */
	protected void logPropriedades() {
		Attribute.check(this.oplModel != null);
		IloOplModelDefinition definition = oplModel.getModelDefinition();
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
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
		if(! loggerSolution.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerSolution);
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
