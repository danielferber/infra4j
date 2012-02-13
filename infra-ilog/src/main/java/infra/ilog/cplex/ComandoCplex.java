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
import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;

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

	/**
	 * O {@link ComandoSolver} delega para esta classe algumas decisões sobre como
	 * executar o CPLEX. Veja 'delegate' design pattern.
	 */
	public static interface Delegate {
		/**
		 * Permite decidir se deve realizar mais uma interação do solucionador.
		 * @param cplex instância do CPLEX
		 * @param numeroIteracao número da iteração da execução do CPLEX
		 * @param configuracao
		 * @return true se deve realizar mais uma execução, false se deve interromper
		 * @throws MotivoException
		 */
		boolean antesExecucao(IloCplex cplex, int numeroIteracao, ConfiguracaoCplex configuracao) throws MotivoException;
		/**
		 * Permite decidir se deve realizar mais uma interação do solucionador.
		 * @param cplex instância do CPLEX
		 * @param numeroIteracao número da iteração da execução do CPLEX
		 * @param configuracao
		 * @return true se deve realizar mais uma execução, false se deve interromper
		 * @throws MotivoException
		 */
		boolean depoisExecucao(IloCplex cplex, int numeroIteracao, ConfiguracaoCplex configuracao) throws MotivoException;
	}

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

	/** Descreve a possibilidade do CPLEX falhar quando a biblioteca ILOG lança uma exceção. */
	public static enum MotivoExecutarCplex implements Motivo {
		CPLEX("Falha ao executar resolvedor CPLEX."), ;

		public final String message;

		private MotivoExecutarCplex(String message) { this.message = message; }
		@Override public String getMensagem() { return this.message; }
		@Override public String getOperacao() { return "Executar CPLEX."; }
	}

	/** Executa o resolvedor CPLEX. */
	@Override
	public void executar() throws MotivoException {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert IllegalAttributeException.apply(this.configuracao != null);
		try {
			IllegalAttributeException.apply(this.cplex.getStatus() == Status.Unknown); /* não pode ter rodado o cplex ainda. */
		} catch (IloException e) {
			/* IloCplex.getStatus() is not known to actually throw IloException. */
			throw new UnsupportedException(e);
		}

		Meter op = MeterFactory.getMeter(loggerMeter, "executar").setMessage("Executar CPLEX").start();
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
			if (this.configuracao.temCaminhoParametrosExportados()) {
				salvarSettings(this.configuracao.getCaminhoAbsolutoParametrosExportados());
			}

			int numeroIteracao = 0;
			while (true) {
				numeroIteracao++;
				Meter opI = MeterFactory.getMeter(loggerMeter, "interacao")
						.put("n", Integer.toString(numeroIteracao))
						.setMessage("Interação %d", numeroIteracao)
						.start();

				try {

					/*
					 * O delegate pode decidir por interromper a execução do CPLEX.
					 */
					if (delegate != null) {
						boolean continuar = delegate.antesExecucao(cplex, numeroIteracao, configuracao);
						if (! continuar) break;
					} else {
						/* Por padrão, se não existe delegate, realiza a execução. */
					}
					validarEstadoCplex();

					/*
					 * Se a thread recebeu sinal de terminar, então também cancela a execução.
					 */
					if (Thread.interrupted()) {
						throw new MotivoException(MotivoExecutarSolver.INTERROMPIDO);
					}

					executarIteracao();

					/*
					 * O delegate pode decidir por continuar ou parar a busca por soluções.
					 */
					if (delegate != null) {
						boolean continuar = delegate.depoisExecucao(cplex, numeroIteracao, configuracao);
						if (! continuar) break;
					} else {
						/* Por padrão, se não existe delegate, interrompe a execução. */
						break;
					}

					opI.ok();
				} catch (Exception e) {
					opI.fail(e);
					throw e;
				}
			}

			validarEstadoFinalCplex();

			op.ok();
		} catch (MotivoException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			op.fail(e);
			throw new UnhandledException(e);
		}
	}

	protected void executarIteracao() {
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
			solucaoEncontrada = this.cplex.solve();
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
			logPropriedadesSolucao();

			/* Reportar solução em arquivo, se assim configurado. */
			if (this.configuracao.temCaminhoSolucaoExportada()) {
				salvarSolucao(this.configuracao.getCaminhoAbsolutoSolucaoExportada());
			}
		}
	}

	/**
	 * Lança {@link MotivoException} se o CPLEX está num estado que proibe nova execução.
	 */
	protected void validarEstadoCplex() throws MotivoException {
		assert IllegalAttributeException.apply(this.cplex != null);
		try {
			if (IloCplex.Status.Error.equals(this.cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();
			} else if (IloCplex.Status.Infeasible.equals(this.cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.INVIAVEL);
			} else if (IloCplex.Status.Unbounded.equals(this.cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.ILIMITADO);
			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(this.cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais), não adianta continuar tentando. */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.ILIMITADO_INVIAVEL);
			} else if (IloCplex.Status.Unknown.equals(this.cplex.getStatus())) {
				/* Ainda não achou solução, nem sabe se o modelo é viável ou não, precisa continuar tentando. */
			} else if (IloCplex.Status.Bounded.equals(this.cplex.getStatus())) {
				/* Ainda não achou solução, aparentemente o modelo é viável, precisa continuar tentando. */
			} else if (IloCplex.Status.Optimal.equals(this.cplex.getStatus())) {
				/* Encontrou uma solução ótima. */
			} else if (IloCplex.Status.Feasible.equals(this.cplex.getStatus())) {
				/* Encontrou uma solução e pode continuar tentando. */
			} else {
				throw new ImpossibleConditionException();
			}
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}
	}

	/**
	 * Lança MotivoException se o CPLEX está num estado considerado insucesso.
	 */
	protected void validarEstadoFinalCplex() throws MotivoException {
		assert IllegalAttributeException.apply(this.cplex != null);
		try {
			if (IloCplex.Status.Error.equals(this.cplex.getStatus())) {
				/* TODO criar tratamento específico caso o CPLEX apresente um erro interno. */
				throw new UnimplementedConditionException();
			} else if (IloCplex.Status.Infeasible.equals(this.cplex.getStatus())) {
				/* O modelo é inviável (mesmo após eventuais intervenções manuais). */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.INVIAVEL);
			} else if (IloCplex.Status.Unbounded.equals(this.cplex.getStatus())) {
				/* O modelo é ilimitado (mesmo após eventuais intervenções manuais). */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.ILIMITADO);
			} else if (IloCplex.Status.InfeasibleOrUnbounded.equals(this.cplex.getStatus())) {
				/* O modelo é ilimitado/inviável (mesmo após eventuais intervenções manuais). */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.ILIMITADO_INVIAVEL);
			} else if (IloCplex.Status.Unknown.equals(this.cplex.getStatus())) {
				/* Ainda não achou solução, nem sabe se o modelo é viável ou não. */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.INCOMPLETO);
			} else if (IloCplex.Status.Bounded.equals(this.cplex.getStatus())) {
				/* Ainda não achou solução, aparentemente o modelo é viável. */
				throw new MotivoException(ComandoSolver.MotivoExecutarSolver.INCOMPLETO);
			} else if (IloCplex.Status.Optimal.equals(this.cplex.getStatus())) {
				/* Encontrou uma solução ótima. */
			} else if (IloCplex.Status.Feasible.equals(this.cplex.getStatus())) {
				/* Encontrou uma solução que não é ótima, mas serve. */
			} else {
				throw new ImpossibleConditionException();
			}
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}
	}

	protected static final String strPropertyPrintPattern = "  - %s = %s%n";

	protected void logPropriedadesSolucionadorCplex() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(loggerDados);
		try {
			out.println("Características do CPLEX:");
			out.format(ComandoCplex.strPropertyPrintPattern, "version", cplex.getVersion());
			out.println();
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
			out.println();
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
		out.println();
		out.println("Características do problema:");
		out.format(ComandoCplex.strPropertyPrintPattern, "mixed integer program", Boolean.toString(cplex.isMIP()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic program", Boolean.toString(cplex.isQP()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic constrains", Boolean.toString(cplex.isQC()));
		out.format(ComandoCplex.strPropertyPrintPattern, "quadratic objective", Boolean.toString(cplex.isQO()));
		out.println();
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
			out.println();
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
			out.println();
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
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar modelo.", e);
		}
	}


	protected void salvarSettings(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		assert IllegalArgumentException.apply(file.isAbsolute());

		try {
			ComandoCplex.assureDiretoryForFile(file);
			this.cplex.writeParam(file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar configurações.", e);
		}
	}

	protected void salvarSolucao(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		assert IllegalArgumentException.apply(file.isAbsolute());

		try {
			ComandoCplex.assureDiretoryForFile(file);
			this.cplex.writeSolution(file.getAbsolutePath());
		} catch (Exception e) {
			/* Do not interrupt execution. Considered a minor failure. */
			loggerExecucao.warn("Falha ao salvar solução.", e);
		}
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }
}
