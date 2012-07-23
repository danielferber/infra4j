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
import static infra.exception.Assert.Poscondition;
import static infra.exception.Assert.Precondition;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Algorithm;
import ilog.cplex.IloCplex.Status;
import infra.exception.RichRuntimeException;
import infra.exception.assertions.controlstate.bug.ImpossibleConditionException;
import infra.exception.assertions.controlstate.bug.ImpossibleException;
import infra.exception.assertions.controlstate.design.UnsupportedException;
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.controlstate.unimplemented.UnhandledException;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedConditionException;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.ilog.NoSolutionException;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;
import infra.slf4j.Operation;
import infra.slf4j.OperationFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.slf4j.Logger;

/**
 * Solver specific implementation for CPLEX of the {@link ComandoSolver}
 * ointerface. Its mais purpose is to gffer a uniform and intuitive
 * understanding of the CPLEX solver.
 * <p>
 * Performs a typical execution fo the CPLEX solver. This execution may be
 * configured through the settings that are passed to the constructor.
 *
 * This implementation does not create the model nor load data. It is assumed
 * that both are already available. The method {@link #executar()} is typically
 * called only once. However, one may call {@link #executar()} again to continue
 * executing a model that was interrupted or that did run out of time.
 *
 *   The responsibilities of the class involves:
 * <ul>
 * <li>Adequately address, log and report the possible execution failures.
 * <li>Write execution progress and solver status to the log.
 * <li>Optionally, write solver configuration properties, model, input data and
 * solution to the log for future investigation with the IBM ILOG CPLEX Studio.
 * </ul>
 *
 * TODO: Callback support to allow customizing the behavior of the typical implementation.
 * TODO: Set solver algorithm according to the class that stores the settings (one settings class per algorithm).
 *
 * @author Daniel Felix Ferber
 */
public class CommandCplex implements ComandoSolver {
	public final Logger logger;
	public final Logger loggerExecucao;
	public final Logger loggerMeter;
	public final Logger loggerData;

	/** Configuration that guides the CPLEX execution. */
	private final ConfiguracaoCplex configuracao;
	/** @return Configuration that guides the CPLEX execution. */
	protected ConfiguracaoCplex getConfiguracao() { return configuracao; }

	/** CPLEX instance being guided. */
	private final IloCplex cplex;
	/** @return CPLEX instance being guided. */
	public IloCplex getCplex() { return cplex; }

	/** Delegate implementation that decided if CPLEX shall continue to run. */
	private final Delegate delegate; /* TODO: really necessary to hold a local copy from the settings? */

	/** Creates the command pattern that guids the CPLEX instance. */
	public CommandCplex(IloCplex cplex, ConfiguracaoCplex configuracao) {
		super();

		Argument.notNull(cplex);
		Argument.notNull(configuracao);
		Argument.notNull(configuracao.getNome());

		this.configuracao = new ConfiguracaoCplex(configuracao); /* for thread safety and execution previsibility, stores a copy of configuration. */
		this.cplex = cplex;
		this.delegate = configuracao.getDelegate();

		this.logger = LoggerFactory.getLogger(LoggerFactory.getLogger("ilog.cplex"), configuracao.getNome());
		this.loggerExecucao = LoggerFactory.getLogger(logger, "exec");
		this.loggerMeter = LoggerFactory.getLogger(logger, "perf");
		this.loggerData = LoggerFactory.getLogger(logger, "data");
	}

	private final Operation ExecutarCplex = OperationFactory.getOperation("executarCplex", "Executar CPLEX");
	private final Operation InteracaoCplex = OperationFactory.getOperation("interacaoCplex", "Interação CPLEX");

	/** Executa o resolvedor CPLEX. */
	@Override
	public void executar() throws NoSolutionException {
		Attribute.notNull(this.cplex);
		Attribute.notNull(this.configuracao);

		/* TODO: evitar chamadas reentrantes se o modelo já estiver executando. */
		try {
			/* TODO: o estado também poderia ser unbounded ou feasible, se aceitarmos novas chamadas no método. */
			Precondition.equal(this.cplex.getStatus(), Status.Unknown); /* não pode ter rodado o cplex ainda. */
		} catch (IloException e) {
			/* IloCplex.getStatus() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		}

		Meter op = MeterFactory.getMeter(loggerMeter, ExecutarCplex).start();
		try {
			/*
			 * Reportar propriedades do solucionador.
			 */
			logSolverProperties();

			/*
			 * Reportar modelo e parâmetros em arquivo para depuração com CPLEX Studio, se assim configurado.
			 */
			if (this.configuracao.temCaminhoModeloExportado()) {
				saveModel(this.configuracao.getCaminhoAbsolutoModeloExportado());
			}
			/*
			 * TODO Estudar se os parâmetros não deveriam ser escritos a cada iteração, uma vez que o Delegate pode
			 * alterar os parâmetros a cada iteração.
			 */
			if (this.configuracao.temCaminhoParametrosExportados()) {
				saveSettings(this.configuracao.getCaminhoAbsolutoParametrosExportados());
			}

			int numeroIteracao = 0;
			while (true) {
				numeroIteracao++;
				Meter opI = MeterFactory.getMeter(loggerMeter, InteracaoCplex).put("n", Integer.toString(numeroIteracao))	.start();

				try {

					/*
					 * O delegate pode decidir por interromper a execução do CPLEX.
					 */
					if (delegate != null) {
						loggerExecucao.debug("Call Delegate.before(iteration={})...", numeroIteracao);
						boolean continuar = delegate.antesExecucao(cplex, numeroIteracao, configuracao);
						loggerExecucao.debug("Returned Delegate.before(iteration={}): run={}.", numeroIteracao, continuar);
						if (! continuar) break;
					} else {
						/* Por padrão, se não existe delegate, realiza a execução. */
					}

					CommandCplex.validarEstadoInicialCplex(cplex, loggerExecucao);

					/*
					 * Se a thread recebeu sinal de terminar, então também cancela a execução.
					 */
					if (Thread.interrupted()) {
						loggerExecucao.debug("Solver thread interrpted. Cancel execution.");
						CommandCplex.validarEstadoFinalCplex(cplex, loggerExecucao);
						break;
					}

					executarIteracao(numeroIteracao);

					/*
					 * O delegate pode decidir por continuar ou parar a busca por soluções.
					 */
					if (delegate != null) {
						loggerExecucao.debug("Call Delegate.after(iteration={})...", numeroIteracao);
						boolean continuar = delegate.depoisExecucao(cplex, numeroIteracao, configuracao);
						loggerExecucao.debug("Returned Delegate.after(iteration={}): repeat={}.", numeroIteracao, continuar);
						if (! continuar) break;
					} else {
						/* Por padrão, se não existe delegate, interrompe a execução. */
						break;
					}

					opI.ok();
				} catch (NoSolutionException e) {
					/* Não haver solução não é considerado uma falha durante a iteração. */
					opI.put("reason", e.reason.toString()).ok();
					throw e;
				} catch (RuntimeException e) {
					opI.fail(e);
					throw e;
				}
			}

			CommandCplex.validarEstadoFinalCplex(this.cplex, loggerExecucao);

			op.ok();
		} catch (NoSolutionException e) {
			op.put("reason", e.reason.toString()).fail(e);
			throw e.operation(ExecutarCplex);
		} catch (RuntimeException e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e, ExecutarCplex);
		}
	}

	protected void executarIteracao(int numeroIteracao) {
		Attribute.notNull(this.cplex);
		Attribute.notNull(this.configuracao);

		/*
		 * TODO Aplicar configurações ao CPLEX de acordo com a subclasse da configuração.
		 */
		try {
			if (configuracao.getSimplexLimiteDeIteracoes() != null) {
				cplex.setParam(IloCplex.IntParam.ItLim, configuracao.getSimplexLimiteDeIteracoes());
			}
			if (configuracao.getSimplexLimiteDeTempo() != null) {
				cplex.setParam(IloCplex.DoubleParam.TiLim, configuracao.getSimplexLimiteDeTempo());
			}
		} catch (IloException e) {
			/* IloCplex.setParam() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		}

		/*
		 * Reportar propriedades do modelo.
		 */
		logModelProperties();

		/*
		 * Ativar callbacks que direcionam o progresso do CPLEX no log.
		 */
		try {
			/*
			 * TODO O uso do callback precisa ser melhor estudado para não impedir multi-threading.
			 */
			cplex.use(new PresolveCallback(loggerExecucao, configuracao.getNumeroPassosEntreProgresso()));
			cplex.use(new ContinuousCallback(loggerExecucao, configuracao.getNumeroPassosEntreProgresso()));
		} catch (IloException e) {
			/* IloCplex.use() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		}


		boolean solucaoEncontrada = false;
		try {
			/*
			 * TODO Implementar outputstream para capturar saida do cplex.
			 * Filtrar mensagens de erro vindas do cplex e jogar no log.
			 * cplex.setOut(s);
			 */

			/*
			 * Resolver!
			 */
			loggerExecucao.debug("Call Cplex.solfe(iteration={})", numeroIteracao);
			solucaoEncontrada = this.cplex.solve();
			loggerExecucao.debug("Cplex.solve(iteration={}): solutionFound={}.", numeroIteracao, solucaoEncontrada);
		} catch (IloException e) {
			/*
			 * TODO Estudar melhor quais exceções fazem sentido. Se não houver alguma, mudar para UnsupportedException.
			 */
			throw new UnhandledException(e);
		}

		/*
		 * Reportar o tipo de resultado obtido.
		 */
		logSolutionProperties(solucaoEncontrada);

		/*
		 * Se existe uma (melhor) solução, então reporta ela no arquivo e no log.
		 */
		if (solucaoEncontrada) {
			loggerExecucao.info("Existe uma solução (ou solução parcial).");
			logSolutionPoolProperties();

			/* Reportar solução em arquivo, se assim configurado. */
			if (this.configuracao.temCaminhoSolucaoExportada()) {
				saveSolution(this.configuracao.getCaminhoAbsolutoSolucaoExportada());
			};
		} else {
			loggerExecucao.info("Não existe solução.");
		}
	}

	/**
	 * Lança {@link MotivoException} se o CPLEX está num estado que proibe nova execução.
	 * @param logger
	 */
	protected static void validarEstadoInicialCplex(IloCplex cplex, Logger logger) throws NoSolutionException {
		Argument.notNull(cplex);
		Argument.notNull(logger);

		String statusString = null;
		NoSolutionException exception = null;

		try {
			statusString = cplex.getStatus().toString();
			if (IloCplex.Status.Error.equals(cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();

			} else if (IloCplex.Status.Infeasible.equals(cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.INFEASIBLE);

			} else if (IloCplex.Status.Unbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.UNBOUNDED);

			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.UNBOUNDED_INFEASIBLE);

			} else if (IloCplex.Status.Unknown.equals(cplex.getStatus())) {
				/* Ainda não achou solução, nem sabe se o modelo é viável ou não, precisa continuar tentando. */

			} else if (IloCplex.Status.Bounded.equals(cplex.getStatus())) {
				/* Ainda não achou solução, aparentemente o modelo é viável, precisa continuar tentando. */

			} else if (IloCplex.Status.Optimal.equals(cplex.getStatus())) {
				/* Encontrou uma solução ótima. */

			} else if (IloCplex.Status.Feasible.equals(cplex.getStatus())) {
				/* Encontrou uma solução e pode continuar tentando. */

			} else {
				throw new ImpossibleConditionException();
			}
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}
		if (exception != null) {
			logger.debug("validarEstadoCplex(status={}): invalid, reason={}", exception.reason);
			throw exception;
		}
		logger.debug("validarEstadoCplex(status={}): ok", statusString);
	}

	/**
	 * Lança MotivoException se a busca do CPLEX termina em um estado considerado insucesso.
	 * Ou seja, um estado que não permite ler a solução, se ela parcial ou ótima.
	 */
	protected static void validarEstadoFinalCplex(IloCplex cplex, Logger logger) throws NoSolutionException {
		Argument.notNull(cplex);
		Argument.notNull(logger);

		String statusString = null;
		NoSolutionException exception = null;

		try {
			statusString = cplex.getStatus().toString();
			if (IloCplex.Status.Error.equals(cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();

			} else if (IloCplex.Status.Infeasible.equals(cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.INFEASIBLE);

			} else if (IloCplex.Status.Unbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.UNBOUNDED);

			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.UNBOUNDED_INFEASIBLE);

			} else if (IloCplex.Status.Unknown.equals(cplex.getStatus())) {
				/* Ainda não achou solução, nem sabe se o modelo é viável ou não. */
				exception = new NoSolutionException(NoSolutionException.Reason.INCOMPLETE);
			} else if (IloCplex.Status.Bounded.equals(cplex.getStatus())) {
				/* Ainda não achou solução, aparentemente o modelo é viável. */
				exception = new NoSolutionException(NoSolutionException.Reason.INCOMPLETE);
			} else if (IloCplex.Status.Optimal.equals(cplex.getStatus())) {
				/* Encontrou uma solução ótima. */
			} else if (IloCplex.Status.Feasible.equals(cplex.getStatus())) {
				/* Encontrou uma solução que não é ótima, mas serve. */
			} else {
				throw new ImpossibleConditionException();
			}
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}
		if (exception != null) {
			logger.debug("validarEstadoFinalCplex(status={}): invalid, reason={}", exception.reason);
			throw exception;
		}
		logger.debug("validarEstadoFinalCplex(status={}): ok", statusString);
	}

	protected static final String strPropertyPrintPattern = "  - %s = %s%n";

	protected void logSolverProperties() {
		Attribute.notNull(cplex);

		try {
			PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
			try {
				out.println("Solver properties:");
				out.format(CommandCplex.strPropertyPrintPattern, "solver", cplex.getClass().getName());
				out.format(CommandCplex.strPropertyPrintPattern, "version", cplex.getVersion());
			} catch (IloException e) {
				/* IloCplex.get<*>() is not known to actually throw IloException. */
				throw new UnsupportedException(e);
			} finally {
				out.close();
			}
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to log model properties.", e);
		}
	}

	protected void logExecutionProperties() {
		Attribute.notNull(cplex);

		try {
			PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
			try {
				out.println("Execution properties:");
				out.format(CommandCplex.strPropertyPrintPattern, "algorithm", CommandCplex.algorithmName(cplex.getAlgorithm()));
				out.format(CommandCplex.strPropertyPrintPattern, "sub algorithm", CommandCplex.algorithmName(cplex.getSubAlgorithm()));
			} catch (IloException e) {
				/* IloCplex.get<*>() is not known to actually throw IloException. */
				throw new UnsupportedException(e);
			} finally {
				out.close();
			}
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to log model properties.", e);
		}
	}

	protected void logModelProperties() {
		Attribute.notNull(cplex);

		try {
			PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
			try {
				out.println("Model matrix properties:");
				out.format(CommandCplex.strPropertyPrintPattern, "colunas", Integer.toString(cplex.getNcols()));
				out.format(CommandCplex.strPropertyPrintPattern, "linhas", Integer.toString(cplex.getNrows()));
				out.format(CommandCplex.strPropertyPrintPattern, "variáveis binárias", Integer.toString(cplex.getNbinVars()));
				out.format(CommandCplex.strPropertyPrintPattern, "variáveis inteiras", Integer.toString(cplex.getNintVars()));
				out.format(CommandCplex.strPropertyPrintPattern, "variáveis semi contínuas", Integer.toString(cplex.getNsemiContVars()));
				out.format(CommandCplex.strPropertyPrintPattern, "variáveis semi contínuas", Integer.toString(cplex.getNsemiIntVars()));
				out.format(CommandCplex.strPropertyPrintPattern, "elementos não zeros", Integer.toString(cplex.getNNZs()));
				out.format(CommandCplex.strPropertyPrintPattern, "restrições quadráticas", Integer.toString(cplex.getNQCs()));
				out.format(CommandCplex.strPropertyPrintPattern, "special ordered sets", Integer.toString(cplex.getNSOSs()));
				out.println();
				out.println("Model properties:");
				out.format(CommandCplex.strPropertyPrintPattern, "mixed integer program", Boolean.toString(cplex.isMIP()));
				out.format(CommandCplex.strPropertyPrintPattern, "quadratic program", Boolean.toString(cplex.isQP()));
				out.format(CommandCplex.strPropertyPrintPattern, "quadratic constrains", Boolean.toString(cplex.isQC()));
				out.format(CommandCplex.strPropertyPrintPattern, "quadratic objective", Boolean.toString(cplex.isQO()));
			} finally {
				out.close();
			}
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to log model properties.", e);
		}
	}

	protected void logSolutionProperties(boolean solutionFound) {
		Attribute.notNull(cplex);

		try {
			PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
			try {
				out.println("Solution:");
				out.format(CommandCplex.strPropertyPrintPattern, "cplex - status", cplex.getCplexStatus().toString());
				out.format(CommandCplex.strPropertyPrintPattern, "cplex - sub status", cplex.getCplexSubStatus().toString());
				out.println();
				out.format(CommandCplex.strPropertyPrintPattern, "solution - found", Boolean.toString(solutionFound));
				out.format(CommandCplex.strPropertyPrintPattern, "solution - is primal", Boolean.toString(cplex.isPrimalFeasible()));
				out.format(CommandCplex.strPropertyPrintPattern, "solution - is dual", Boolean.toString(cplex.isDualFeasible()));
				out.format(CommandCplex.strPropertyPrintPattern, "solution - status ", cplex.getStatus().toString());
				out.println();
				out.format(CommandCplex.strPropertyPrintPattern, "nodes - analysed", Integer.toString(cplex.getNnodes()));
				out.format(CommandCplex.strPropertyPrintPattern, "nodes - remaining", Integer.toString(cplex.getNnodesLeft()));
				out.println();
				out.format(CommandCplex.strPropertyPrintPattern, "interations", Integer.toString(cplex.getNiterations()));
				out.format(CommandCplex.strPropertyPrintPattern, "interations - simplex fase 1", Integer.toString(cplex.getNphaseOneIterations()));
				out.format(CommandCplex.strPropertyPrintPattern, "interations - sifting fase 1", Integer.toString(cplex.getNsiftingPhaseOneIterations()));
				out.format(CommandCplex.strPropertyPrintPattern, "interations - sifting", Integer.toString(cplex.getNsiftingIterations()));
				out.format(CommandCplex.strPropertyPrintPattern, "interations - barrier", Integer.toString(cplex.getNbarrierIterations()));
			} catch (IloException e) {
				/* IloCplex.get<*>() is not known to actually throw IloException. */
				throw new UnsupportedException(e);
			} finally {
				out.close();
			}
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to log solution properties.", e);
		}
	}

	protected void logSolutionPoolProperties() {
		Attribute.notNull(cplex);

		try {
			PrintStream out = LoggerFactory.getInfoPrintStream(loggerData);
			try {
				out.println("Solution pool:");
				out.format(CommandCplex.strPropertyPrintPattern, "number of solution", Integer.toString(cplex.getSolnPoolNsolns()));
				out.format(CommandCplex.strPropertyPrintPattern, "objective function (last solution)", Double.toString(cplex.getObjValue()));
				out.format(CommandCplex.strPropertyPrintPattern, "objective function (best solution)", Double.toString(cplex.getBestObjValue()));
				out.close();
			} catch (IloException e) {
				/* IloCplex.get<*>() is not known to actually throw IloException. */
				throw new UnsupportedException(e);
			} finally {
				out.close();
			}
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to log solution pool properties.", e);
		}
	}

	protected static String algorithmName(int algorithm) {
		if (algorithm == Algorithm.Auto) {
			return "Auto";
		} else if (algorithm == Algorithm.Barrier) {
			return "Barrier";
		} else if (algorithm == Algorithm.Concurrent) {
			return "Concurrent";
		} else if (algorithm == Algorithm.Dual) {
			return "Dual";
		} else if (algorithm == Algorithm.Network) {
			return "Network";
		} else if (algorithm == Algorithm.None) {
			return "None";
		} else if (algorithm == Algorithm.Primal) {
			return "Primal";
		} else if (algorithm == Algorithm.Sifting) {
			return "Sifting";
		} else {
			return "Unknown: #" + Integer.toString(algorithm);
		}
	}

	protected static void assureDiretoryForFile(File file) throws IOException {
		File parentDir = file.getParentFile();
		if (! parentDir.exists()) {
			if (! parentDir.mkdirs()) {
				throw new IOException(String.format("Failed to create directory '%s'.", parentDir.getAbsolutePath()));
			}
		}
		Poscondition.check(parentDir.exists());
	}

	protected void saveModel(File file) {
		Attribute.notNull(cplex);
		Argument.notNull(file);
		Argument.check(file.isAbsolute());

		try {
			CommandCplex.assureDiretoryForFile(file);
			this.cplex.exportModel(file.getAbsolutePath());
			Poscondition.check(file.exists());
			loggerExecucao.info("A copy of the model was saved to file {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to save a copy of the model to file {}.", file.getAbsolutePath(), e);
		}
	}


	protected void saveSettings(File file) {
		Attribute.notNull(cplex);
		Argument.notNull(file);
		Argument.check(file.isAbsolute());

		try {
			CommandCplex.assureDiretoryForFile(file);
			this.cplex.writeParam(file.getAbsolutePath());
			Poscondition.check(file.exists());
			loggerExecucao.info("A copy of the configuration was saved to file {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to save a copy of the configuration to file {}.", file.getAbsolutePath(), e);
		}
	}

	protected void saveSolution(File file) {
		Attribute.notNull(cplex);
		Argument.notNull(file);
		Argument.check(file.isAbsolute());

		try {
			CommandCplex.assureDiretoryForFile(file);
			this.cplex.writeSolution(file.getAbsolutePath());
			Poscondition.check(file.exists());
			loggerExecucao.info("A copy of the solution was saved to file {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Failed to save a copy of the solution to file {}.", file.getAbsolutePath(), e);
		}
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }
}
