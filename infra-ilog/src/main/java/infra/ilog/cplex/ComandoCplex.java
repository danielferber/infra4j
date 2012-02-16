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

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Algorithm;
import ilog.cplex.IloCplex.Status;
import infra.exception.assertions.controlstate.bug.ImpossibleConditionException;
import infra.exception.assertions.controlstate.bug.ImpossibleException;
import infra.exception.assertions.controlstate.design.UnsupportedException;
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.controlstate.unimplemented.UnhandledException;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedConditionException;
import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.IllegalAttributeException;
import infra.exception.assertions.datastate.NullArgumentException;
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
 * Implementação padrão específica para o CPLEX para o 'command' design pattern.
 * Executa o CPLEX conforme as configurações. A classe que implementa as configurações
 * especifica qual algoritmo de programação linear será utilizado.
 * <p>
 * Não é responsabilidade desta classe a geração do modelo, apenas a execução.
 * A classe permite apenas uma execução do CPLEX.
 * <p>
 * As responsabilidades da classe envolvem:
 * <ul>
 * <li>Tratar adequadamente as possíveis falhas de execução.
 * <li>Registrar progresso da execução em log.
 * <li>Registrar propriedades e status do modelo PL em log.
 * <li>Opcionalmente, salvar modelo e parâmetros em arquivo para depurar com CPLEX Studio.
 * <li>Opcionalmente, salvar a solução em arquivo para revisão manual.
 * </ul>
 * A fazer
 * <ul>
 * <li>Estrutura de callback para personalizar chamada do solucionador (TODO).
 * </ul>
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class ComandoCplex implements ComandoSolver {
	public final Logger logger;
	public final Logger loggerExecucao;
	public final Logger loggerMeter;
	public final Logger loggerDados;

	/** Configurações de execução da instância. */
	private final ConfiguracaoCplex configuracao;
	protected ConfiguracaoCplex getConfiguracao() { return configuracao; }

	/** Instância CPLEX gerenciada. */
	private final IloCplex cplex;
	protected IloCplex getCplex() { return cplex; }

	/** Delegate que auxilia nas decisões. */
	private final Delegate delegate;

	/** Cria o comando executor a partir de uma instância CPLEX existente. */
	public ComandoCplex(IloCplex cplex, ConfiguracaoCplex configuracao) {
		super();

		NullArgumentException.apply(cplex, configuracao);

		this.configuracao = new ConfiguracaoCplex(configuracao); /* guarda uma cópia da configuração. */
		this.cplex = cplex;
		this.delegate = configuracao.getDelegate();

		this.logger = LoggerFactory.getLogger(LoggerFactory.getLogger("ilog.cplex"), configuracao.getNome());
		this.loggerExecucao = LoggerFactory.getLogger(logger, "execucao");
		this.loggerMeter = LoggerFactory.getLogger(logger, "meter");
		this.loggerDados = LoggerFactory.getLogger(logger, "dados");
	}

	private final Operation ExecutarCplex = OperationFactory.getOperation("executarCplex", "Executar CPLEX");
	private final Operation InteracaoCplex = OperationFactory.getOperation("interacaoCplex", "Interação CPLEX");

	/** Executa o resolvedor CPLEX. */
	@Override
	public void executar() throws NoSolutionException {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert IllegalAttributeException.apply(this.configuracao != null);
		try {
			IllegalAttributeException.apply(this.cplex.getStatus() == Status.Unknown); /* não pode ter rodado o cplex ainda. */
		} catch (IloException e) {
			/* IloCplex.getStatus() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		}

		Meter op = MeterFactory.getMeter(loggerMeter, ExecutarCplex).start();
		try {
			/*
			 * Reportar propriedades do solucionador.
			 */
			logPropriedadesSolucionadorCplex();

			/*
			 * Reportar modelo e parâmetros em arquivo para depuração com CPLEX Studio, se assim configurado.
			 */
			if (this.configuracao.temCaminhoModeloExportado()) {
				salvarModelo(this.configuracao.getCaminhoAbsolutoModeloExportado());
			}
			/*
			 * TODO Estudar se os parâmetros não deveria ser escritos a cada iteração, uma vez que o Delegate pode
			 * alterar os parâmetros a cada iteração.
			 */
			if (this.configuracao.temCaminhoParametrosExportados()) {
				salvarSettings(this.configuracao.getCaminhoAbsolutoParametrosExportados());
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

					ComandoCplex.validarEstadoInicialCplex(cplex, loggerExecucao);

					/*
					 * Se a thread recebeu sinal de terminar, então também cancela a execução.
					 */
					if (Thread.interrupted()) {
						loggerExecucao.debug("Solver thread interrpted. Cancel execution.");
						ComandoCplex.validarEstadoFinalCplex(cplex, loggerExecucao);
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

			ComandoCplex.validarEstadoFinalCplex(this.cplex, loggerExecucao);

			op.ok();
		} catch (NoSolutionException e) {
			op.put("reason", e.reason.toString()).fail(e);
			throw e;
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		}
	}

	protected void executarIteracao(int numeroIteracao) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert IllegalAttributeException.apply(this.configuracao != null);

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
		logPropriedadesModelo();

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
		logPropriedadesResultado(solucaoEncontrada);

		/*
		 * Se existe uma (melhor) solução, então reporta ela no arquivo e no log.
		 */
		if (solucaoEncontrada) {
			loggerExecucao.info("Existe uma solução (ou solução parcial).");
			logPropriedadesSolucao();

			/* Reportar solução em arquivo, se assim configurado. */
			if (this.configuracao.temCaminhoSolucaoExportada()) {
				salvarSolucao(this.configuracao.getCaminhoAbsolutoSolucaoExportada());
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
		assert NullArgumentException.apply(cplex);
		assert NullArgumentException.apply(logger);

		String statusString = null;
		NoSolutionException exception = null;

		try {
			statusString = cplex.getStatus().toString();
			if (IloCplex.Status.Error.equals(cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();

			} else if (IloCplex.Status.Infeasible.equals(cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.INVIAVEL);

			} else if (IloCplex.Status.Unbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.ILIMITADO);

			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				exception = new NoSolutionException(NoSolutionException.Reason.ILIMITADO_INVIAVEL);

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
		assert NullArgumentException.apply(cplex);
		assert NullArgumentException.apply(logger);
		String statusString = null;
		NoSolutionException exception = null;

		try {
			statusString = cplex.getStatus().toString();
			if (IloCplex.Status.Error.equals(cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();

			} else if (IloCplex.Status.Infeasible.equals(cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.INVIAVEL);

			} else if (IloCplex.Status.Unbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.ILIMITADO);

			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais). */
				exception = new NoSolutionException(NoSolutionException.Reason.ILIMITADO_INVIAVEL);

			} else if (IloCplex.Status.Unknown.equals(cplex.getStatus())) {
				/* Ainda não achou solução, nem sabe se o modelo é viável ou não. */
				exception = new NoSolutionException(NoSolutionException.Reason.INCOMPLETO);
			} else if (IloCplex.Status.Bounded.equals(cplex.getStatus())) {
				/* Ainda não achou solução, aparentemente o modelo é viável. */
				exception = new NoSolutionException(NoSolutionException.Reason.INCOMPLETO);
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

	protected void logPropriedadesSolucionadorCplex() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		try {
			out.println("Características do CPLEX:");
			out.format(ComandoCplex.strPropertyPrintPattern, "version", cplex.getVersion());
		} catch (IloException e) {
			/* IloCplex.get<*>() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesExecucao() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		try {
			out.println("Características da execução:");
			out.format(ComandoCplex.strPropertyPrintPattern, "algorithm", ComandoCplex.algorithmName(cplex.getAlgorithm()));
			out.format(ComandoCplex.strPropertyPrintPattern, "subAlgorithm", ComandoCplex.algorithmName(cplex.getSubAlgorithm()));
		} catch (IloException e) {
			/* IloCplex.get<*>() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesModelo() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		out.println("Características da matriz:");
		out.format(ComandoCplex.strPropertyPrintPattern, "colunas", Integer.toString(cplex.getNcols()));
		out.format(ComandoCplex.strPropertyPrintPattern, "linhas", Integer.toString(cplex.getNrows()));
		out.format(ComandoCplex.strPropertyPrintPattern, "variáveis binárias", Integer.toString(cplex.getNbinVars()));
		out.format(ComandoCplex.strPropertyPrintPattern, "variáveis inteiras", Integer.toString(cplex.getNintVars()));
		out.format(ComandoCplex.strPropertyPrintPattern, "variáveis semi contínuas", Integer.toString(cplex.getNsemiContVars()));
		out.format(ComandoCplex.strPropertyPrintPattern, "variáveis semi contínuas", Integer.toString(cplex.getNsemiIntVars()));
		out.format(ComandoCplex.strPropertyPrintPattern, "elementos não zeros", Integer.toString(cplex.getNNZs()));
		out.format(ComandoCplex.strPropertyPrintPattern, "restrições quadráticas", Integer.toString(cplex.getNQCs()));
		out.format(ComandoCplex.strPropertyPrintPattern, "special ordered sets", Integer.toString(cplex.getNSOSs()));
		out.println("Características do problema:");
		out.format(ComandoCplex.strPropertyPrintPattern, "mixed integer program", Boolean.toString(cplex.isMIP()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic program", Boolean.toString(cplex.isQP()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic constrains", Boolean.toString(cplex.isQC()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic objective", Boolean.toString(cplex.isQO()));
		out.close();
	}

	protected void logPropriedadesResultado(boolean solucaoEncontrada) {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		try {
			out.println("Características da solução:");
			out.format(ComandoCplex.strPropertyPrintPattern, "status cplex", cplex.getCplexStatus().toString());
			out.format(ComandoCplex.strPropertyPrintPattern, "sub status cplex", cplex.getCplexSubStatus().toString());
			out.println();
			out.format(ComandoCplex.strPropertyPrintPattern, "existe solução", Boolean.toString(solucaoEncontrada));
			out.format(ComandoCplex.strPropertyPrintPattern, "existe solução primal", Boolean.toString(cplex.isPrimalFeasible()));
			out.format(ComandoCplex.strPropertyPrintPattern, "existe solução dual", Boolean.toString(cplex.isDualFeasible()));
			out.format(ComandoCplex.strPropertyPrintPattern, "status da solução", cplex.getStatus().toString());
			out.println();
			out.format(ComandoCplex.strPropertyPrintPattern, "nós analisados", Integer.toString(cplex.getNnodes()));
			out.format(ComandoCplex.strPropertyPrintPattern, "nós restantes", Integer.toString(cplex.getNnodesLeft()));
			out.println();
			out.format(ComandoCplex.strPropertyPrintPattern, "interações", Integer.toString(cplex.getNiterations()));
			out.format(ComandoCplex.strPropertyPrintPattern, "interações simplex fase 1", Integer.toString(cplex.getNphaseOneIterations()));
			out.format(ComandoCplex.strPropertyPrintPattern, "interações sifting fase 1", Integer.toString(cplex.getNsiftingPhaseOneIterations()));
			out.format(ComandoCplex.strPropertyPrintPattern, "interações sifting", Integer.toString(cplex.getNsiftingIterations()));
			out.format(ComandoCplex.strPropertyPrintPattern, "interações barreira", Integer.toString(cplex.getNbarrierIterations()));
		} catch (IloException e) {
			/* IloCplex.get<*>() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesSolucao() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		try {
			out.format(ComandoCplex.strPropertyPrintPattern, "número de soluções", Integer.toString(cplex.getSolnPoolNsolns()));
			out.format(ComandoCplex.strPropertyPrintPattern, "função objetivo da última solução", Double.toString(cplex.getObjValue()));
			out.format(ComandoCplex.strPropertyPrintPattern, "função objetivo da melhor solução", Double.toString(cplex.getBestObjValue()));
			out.close();
		} catch (IloException e) {
			/* IloCplex.get<*>() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		} finally {
			out.close();
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
		if (! file.getParentFile().exists()) {
			if (! file.getParentFile().mkdirs()) {
				throw new IOException(String.format("Failed to create directory '%s'.", file.getParentFile().getAbsolutePath()));
			}
		}
	}

	protected void salvarModelo(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		assert IllegalArgumentException.apply(file.isAbsolute());

		try {
			ComandoCplex.assureDiretoryForFile(file);
			this.cplex.exportModel(file.getAbsolutePath());
			loggerExecucao.info("Cópia do modelo salva em {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia do modelo em {}.", file.getAbsolutePath(), e);
		}
	}


	protected void salvarSettings(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		assert IllegalArgumentException.apply(file.isAbsolute());

		try {
			ComandoCplex.assureDiretoryForFile(file);
			this.cplex.writeParam(file.getAbsolutePath());
			loggerExecucao.info("Cópia da configuração salva em {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia da configuração em {}.", file.getAbsolutePath(), e);
		}
	}

	protected void salvarSolucao(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		assert IllegalArgumentException.apply(file.isAbsolute());

		try {
			ComandoCplex.assureDiretoryForFile(file);
			this.cplex.writeSolution(file.getAbsolutePath());
			loggerExecucao.info("Cópia da solução salva em {}.", file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar cópia da solução em {}.", file.getAbsolutePath(), e);
		}
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }
}
