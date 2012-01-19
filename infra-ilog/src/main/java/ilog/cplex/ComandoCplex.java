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
package ilog.cplex;

import ilog.ComandoSolver;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Algorithm;
import ilog.cplex.IloCplex.Status;
import infra.exception.assertions.controlstate.bug.ImpossibleConditionException;
import infra.exception.assertions.controlstate.bug.ImpossibleException;
import infra.exception.assertions.controlstate.design.UnsupportedException;
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedConditionException;
import infra.exception.assertions.datastate.IllegalArgumentException;
import infra.exception.assertions.datastate.IllegalAttributeException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;

import java.io.File;
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
	protected static final Logger logger = LoggerFactory.getLogger("ilog.cplex");
	protected static final Logger loggerExecucao = LoggerFactory.getLogger(ComandoCplex.logger, "execucao");

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

		Meter meterExecutar = MeterFactory.getMeter(ComandoCplex.loggerExecucao, "executar").setMessage("Executar CPLEX");

		try {
			IllegalAttributeException.apply(this.cplex.getStatus() == Status.Unknown); /* não pode ter rodado o cplex ainda. */
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}

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

		meterExecutar.start();
		try {
			int numeroIteracao = 0;
			while (true) {
				numeroIteracao++;
				Meter meterInteracao = MeterFactory.getMeter(ComandoCplex.loggerExecucao, "executar").setMessage("Interação %d", numeroIteracao);
				meterInteracao.start();
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

					meterInteracao.ok();
				} catch (Exception e) {
					meterInteracao.fail(e);
				}
			}

			validarEstadoFinalCplex();

			meterExecutar.ok();
		} catch (MotivoException e) {
			meterExecutar.fail(e);
			throw e;
		} catch (Exception e) {
			meterExecutar.fail(e);
			throw new MotivoException(MotivoExecutarCplex.CPLEX, e);
		}
	}

	protected void executarIteracao() throws MotivoException {
		/*
		 * TODO aplicar configurações ao CPLEX de acordo com a subclasse da configuração.
		 */
		try {
			if (configuracao.getSimplexLimiteDeIteracoes() != null) {
				cplex.setParam(IloCplex.IntParam.ItLim, configuracao.getSimplexLimiteDeIteracoes());
			}
			if (configuracao.getSimplexLimiteDeTempo() != null) {
				cplex.setParam(IloCplex.DoubleParam.TiLim, configuracao.getSimplexLimiteDeTempo());
			}
		} catch (IloException e) {
			throw new UnsupportedException(e);
		}

		/*
		 * Resolver!
		 */
		logPropriedadesModelo();
		boolean solucaoEncontrada = false;
		try {
			/* TODO o uso do callback precisa ser melhor estudado para não impedir multi-threading. */
			//cplex.use(new PresolveCallback(loggerExecucao, configuracao.getNumeroPassosEntreProgresso()));
			//cplex.use(new ContinuousCallback(loggerExecucao, configuracao.getNumeroPassosEntreProgresso()));
			// TODO Implementar outputstream para capturar saida do cplex.
			// Filtrar mensagens de erro vindas do cplex e jogar no log.
			//			cplex.setOut(s);
			solucaoEncontrada = this.cplex.solve();
		} catch (IloException e) {
			throw new MotivoException(e, MotivoExecutarCplex.CPLEX);
		}
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
	private void validarEstadoCplex() throws MotivoException {
		try {
			if (IloCplex.Status.Error.equals(this.cplex.getStatus())) {
				// TODO criar tratamento específico caso o CPLEX apresente um erro interno.
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
	private void validarEstadoFinalCplex() throws MotivoException {
		try {
			if (IloCplex.Status.Error.equals(this.cplex.getStatus())) {
				// TODO criar tratamento específico caso o CPLEX apresente um erro interno.
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

	protected static final String propertyPrintPattern = "  - %s = %s\n";

	protected void logPropriedadesSolucionadorCplex() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoCplex.loggerExecucao);
		try {
			out.println("Características do CPLEX:");
			out.format(ComandoCplex.propertyPrintPattern, "version", cplex.getVersion());
			out.println();
		} catch (IloException e) {
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesExecucao() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoCplex.loggerExecucao);
		try {
			out.println("Características da execução:");
			out.format(ComandoCplex.propertyPrintPattern, "getAlgorithm", algorithmName(cplex.getAlgorithm()));
			out.format(ComandoCplex.propertyPrintPattern, "getSubAlgorithm", algorithmName(cplex.getSubAlgorithm()));
			out.println();
		} catch (IloException e) {
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesResultado(boolean solucaoEncontrada) {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoCplex.loggerExecucao);
		try {
			out.println("Características da solução:");
			out.format(ComandoCplex.propertyPrintPattern, "status cplex", cplex.getCplexStatus().toString());
			out.format(ComandoCplex.propertyPrintPattern, "sub status cplex", cplex.getCplexSubStatus().toString());
			out.println();
			out.format(ComandoCplex.propertyPrintPattern, "existe solução", solucaoEncontrada);
			out.format(ComandoCplex.propertyPrintPattern, "existe solução primal", cplex.isPrimalFeasible());
			out.format(ComandoCplex.propertyPrintPattern, "existe solução dual", cplex.isDualFeasible());
			out.format(ComandoCplex.propertyPrintPattern, "status da solução", cplex.getStatus().toString());
			out.println();
			out.format(ComandoCplex.propertyPrintPattern, "nós analisados", cplex.getNnodes());
			out.format(ComandoCplex.propertyPrintPattern, "nós restantes", cplex.getNnodesLeft());
			out.println();
			out.format(ComandoCplex.propertyPrintPattern, "interações", cplex.getNiterations());
			out.format(ComandoCplex.propertyPrintPattern, "interações simplex fase 1", cplex.getNphaseOneIterations());
			out.format(ComandoCplex.propertyPrintPattern, "interações sifting fase 1", cplex.getNsiftingPhaseOneIterations());
			out.format(ComandoCplex.propertyPrintPattern, "interações sifting", cplex.getNsiftingIterations());
			out.format(ComandoCplex.propertyPrintPattern, "interações barreira", cplex.getNbarrierIterations());
			out.println();
		} catch (IloException e) {
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected void logPropriedadesSolucao() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoCplex.loggerExecucao);
		try {
			out.format(ComandoCplex.propertyPrintPattern, "número de soluções", cplex.getSolnPoolNsolns());
			out.format(ComandoCplex.propertyPrintPattern, "função objetivo da última solução", cplex.getObjValue());
			out.format(ComandoCplex.propertyPrintPattern, "função objetivo da melhor solução", cplex.getBestObjValue());
			out.println();
			out.close();
		} catch (IloException e) {
			throw new UnsupportedException(e);
		} finally {
			out.close();
		}
	}

	protected String algorithmName(int algorithm) {
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

	protected void logPropriedadesModelo() {
		assert IllegalAttributeException.apply(this.cplex != null);
		PrintStream out = LoggerFactory.getInfoPrintStream(ComandoCplex.loggerExecucao);
		out.println("Características da matriz:");
		out.format(ComandoCplex.propertyPrintPattern, "colunas", cplex.getNcols());
		out.format(ComandoCplex.propertyPrintPattern, "linhas", cplex.getNrows());
		out.format(ComandoCplex.propertyPrintPattern, "variáveis binárias", cplex.getNbinVars());
		out.format(ComandoCplex.propertyPrintPattern, "variáveis inteiras", cplex.getNintVars());
		out.format(ComandoCplex.propertyPrintPattern, "variáveis semi contínuas", cplex.getNsemiContVars());
		out.format(ComandoCplex.propertyPrintPattern, "variáveis semi contínuas", cplex.getNsemiIntVars());
		out.format(ComandoCplex.propertyPrintPattern, "elementos não zeros", cplex.getNNZs());
		out.format(ComandoCplex.propertyPrintPattern, "restrições quadráticas", cplex.getNQCs());
		out.format(ComandoCplex.propertyPrintPattern, "special ordered sets", cplex.getNSOSs());
		out.println();
		out.println("Características do problema:");
		out.format(ComandoCplex.propertyPrintPattern, "mixed integer program", cplex.isMIP());
		out.format(ComandoCplex.propertyPrintPattern, "quadratic program", cplex.isQP());
		out.format(ComandoCplex.propertyPrintPattern, "quadratic constrains", cplex.isQC());
		out.format(ComandoCplex.propertyPrintPattern, "quadratic objective", cplex.isQO());
		out.println();
		out.close();
	}

	protected void salvarModelo(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		try {
			assert IllegalArgumentException.apply(file.isAbsolute());
			if (! file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
		this.cplex.exportModel(file.getAbsolutePath());
		} catch (IloException e) {
			ComandoCplex.loggerExecucao.warn("Falha ao salvar modelo.", e);
		}
	}

	protected void salvarSettings(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		try {
			assert IllegalArgumentException.apply(file.isAbsolute());
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			this.cplex.writeParam(file.getAbsolutePath());
		} catch (IloException e) {
			ComandoCplex.loggerExecucao.warn("Falha ao salvar configurações.", e);
		}
	}

	protected void salvarSolucao(File file) {
		assert IllegalAttributeException.apply(this.cplex != null);
		assert NullArgumentException.apply(file);
		try {
			assert IllegalArgumentException.apply(file.isAbsolute());
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			this.cplex.writeSolution(file.getAbsolutePath());
		} catch (IloException e) {
			ComandoCplex.loggerExecucao.warn("Falha ao salvar solução.", e);
		}
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }

}
