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
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.opl.IloOplElementDefinition;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import ilog.opl.IloOplModelSource;
import ilog.opl.IloOplSettings;
import infra.exception.RichRuntimeException;
import infra.exception.controlstate.unimplemented.UnhandledException;
import infra.exception.controlstate.unimplemented.UnimplementedConditionException;
import infra.ilog.NoSolutionException;
import infra.ilog.SolverCommand;
import infra.ilog.cplex.CommandCplex;
import infra.ilog.cplex.ConfigurationCplex;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;
import infra.slf4j.Operation;
import infra.slf4j.OperationFactory;
import infra.slf4j.OperationWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;


/**
 * Executa o OPL conforme as configurações.
 * Isto envolve esconder a maior quantidade possível dos passos necessários segundo a API do OPL para criar o modelo CPLEX.
 *
 * @author Daniel Felix Ferber
 */
public class FacadeOPL {
	public final Logger logger;
	public final Logger loggerMeter;
	public final Logger loggerExecucao;
	public final Logger loggerDados;
	public final Logger loggerModelo;

	/** Container com as configurações para esta instância. */
	private final ConfiguracaoOPL configuracaoOpl;
	protected ConfiguracaoOPL getConfiguracaoOpl() { return configuracaoOpl; }

	/*
	 * TODO Estudar se é realmente necessário ter sempre uma referência da configuração Cplex/CP, ou se ela
	 * pode ser passada diretamente no método executar.
	 */
	/** Container com as configurações para esta instância. */
	private final ConfigurationCplex configuracaoCplex;
	protected ConfigurationCplex getConfiguracaoCplex() { 	return configuracaoCplex; }

	/** Acesso ao modelo. */
	private final ProvedorModelo modeloProvider;
	private final Collection<FonteDados> dataSources;
	private final Collection<ConsumidorDados> dataSinks;

	private static final Operation ExecuteFacade = OperationFactory.getOperation("loadLibrary", "Execute facade.");
	private static final Operation CreateSettings = OperationFactory.getOperation("createSettings", "Create OPL settings.");
	private static final OperationWithMessage LoadModel = (OperationWithMessage) OperationFactory.getOperation("loadModel", "Load OPL model.");
	private static final Operation ParseModel = OperationFactory.getOperation("parseModel", "Parse OPL model.");
	private static final Operation CreateCplex = OperationFactory.getOperation("createCplex", "Create CPLEX solver.");
	private static final Operation CreateModelCplex = OperationFactory.getOperation("createModelCplex", "Create model on CPLEX solver.");
	private static final Operation DefineDataSources = OperationFactory.getOperation("defineDataSources", "Define data sources.");
	private static final Operation DefineDataSinks = OperationFactory.getOperation("defineDataSinks", "Define data sinks.");
	private static final OperationWithMessage RegisterDataSources = (OperationWithMessage) OperationFactory.getOperation("registerDataSources", "Register data sources.");
	private static final OperationWithMessage ExportDataSinks = (OperationWithMessage) OperationFactory.getOperation("exportDataSinks", "Export data sinks.");
	private static final Operation RealizeModelOnCplex = OperationFactory.getOperation("realizeModelCCplex", "Realize model on CPLEX.");
	private static final Operation ExecuteSolver = OperationFactory.getOperation("executeSolver", "Execute solver.");
	private static final Operation ExecutePosProcessing = OperationFactory.getOperation("executePosProcessing", "Execute pós-processing.");

	/* TODO trocar por um design mais adequado, sem usar motivos. */
	public static enum MotivosExecucao {
		RegisterDataSources(FacadeOPL.RegisterDataSources.getName(), FacadeOPL.RegisterDataSources.getMessage()),
		ExportDataSinks(FacadeOPL.ExportDataSinks.getName(), FacadeOPL.ExportDataSinks.getMessage()),
		LoadModel(FacadeOPL.LoadModel.getName(), FacadeOPL.LoadModel.getMessage());
		String operacao;
		String mensagem;

		private MotivosExecucao(String operacao, String mensagem) {
			this.operacao = operacao;
			this.mensagem = mensagem;
		}
		public String getMensagem() { return mensagem; }
		public String getOperacao() { return operacao; 	}
	}

	public FacadeOPL(ConfiguracaoOPL configuracaoOpl, ConfigurationCplex configuracaoCplex, ProvedorModelo modeloProvider, Collection<FonteDados> dataSources, Collection<ConsumidorDados> dataSinks) {
		super();
		Argument.notNull(configuracaoOpl);
		Argument.notNull(modeloProvider);
		Argument.notNull(configuracaoCplex);

		this.configuracaoOpl = new ConfiguracaoOPL(configuracaoOpl);
		this.configuracaoCplex = new ConfigurationCplex(configuracaoCplex);
		this.modeloProvider = modeloProvider;
		if (dataSources != null) {
			this.dataSources = Collections.unmodifiableCollection(dataSources);
		} else {
			this.dataSources = Collections.emptySet();
		}
		if (dataSinks != null) {
			this.dataSinks = Collections.unmodifiableCollection(dataSinks);
		} else {
			this.dataSinks = Collections.emptySet();
		}

		this.logger = LoggerFactory.getLogger(LoggerFactory.getLogger("ilog.opl"), configuracaoOpl.getNome());
		this.loggerMeter = LoggerFactory.getLogger(this.logger, "meter");
		this.loggerExecucao = LoggerFactory.getLogger(this.logger, "execucao");
		this.loggerDados = LoggerFactory.getLogger(this.logger, "dados");
		this.loggerModelo = LoggerFactory.getLogger(this.loggerDados, "modelo");
	}

	protected static void assureDiretory(File file) throws IOException {
		if (! file.exists()) {
			if (! file.mkdirs()) {
				throw new IOException(String.format("Failed to create directory '%s'.", file.getAbsolutePath()));
			}
		}
	}

	/* TODO run garbage collector after data source and data sink. */
	public void executar() throws NoSolutionException, OplSettingsException, OplModelException {
		Meter op = null;

		Meter allOp = MeterFactory.getMeter(loggerMeter, FacadeOPL.ExecuteFacade).start();
		IloOplFactory oplFactory = null;
		try {
			oplFactory = loadLibrary();
			CustomErrorHandler errorHandler = new CustomErrorHandler(oplFactory, loggerExecucao);
			IloOplSettings 	oplSettings = createSettings(oplFactory, errorHandler);
			IloOplModelSource oplModelSource = loadModel(oplFactory, errorHandler);
			IloOplModelDefinition oplModelDefinition = parseModel(oplFactory, errorHandler, oplSettings, oplModelSource);
			oplSettings = null;
			SolverCommand comandoSolver = createSolver(oplFactory, errorHandler, oplModelDefinition);
			IloOplModel oplModel;
			if (comandoSolver instanceof CommandCplex) {
				CommandCplex comandoCplex = (CommandCplex) comandoSolver;
				oplModel = createModelOnSolver(oplFactory, errorHandler, oplModelDefinition, comandoCplex);
			} else {
				throw new UnimplementedConditionException();
			}
			oplModelDefinition = null;
			defineDataSources(oplModel);
			defineDataSinks(oplModel);
			registerDataSources(oplModel);
			if (comandoSolver instanceof CommandCplex) {
				CommandCplex comandoCplex = (CommandCplex) comandoSolver;
				realizeModelOnSolver(oplFactory, errorHandler, oplModel, comandoCplex);
			} else {
				throw new UnimplementedConditionException();
			}
			executeSolver(oplModel, comandoSolver);
			executePosProcessing(oplModel, errorHandler);
			exportDataSinks(oplModel);
			allOp.ok();
		} catch (NoSolutionException e) {
			allOp.fail(e);
			throw e;
		} catch (RuntimeException e) {
			allOp.fail(e);
			throw RichRuntimeException.enrich(e, FacadeOPL.ExecuteFacade);
		} finally {
			if (oplFactory != null) {
				oplFactory.end();
			}
		}
	}

	/**
	 * SOLVER.
	 *
	 * Criar o modelo associado com o solucionador (CP ou CPLEX).
	 */
	protected SolverCommand createSolver(IloOplFactory oplFactory, CustomErrorHandler errorHandler, IloOplModelDefinition oplModelDefinition) {
		SolverCommand comandoSolver = null;
		if (configuracaoCplex != null) {
			Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.CreateCplex).start();
			try {
					IloCplex cplex = oplFactory.createCplex();
					comandoSolver = new CommandCplex(cplex, configuracaoCplex);
					op.ok();
			} catch (IloException e) {
				/* TODO Investigar quando esta exceção pode ocorrer. Será se faltar a dll do cplex? */
				op.fail(e);
				throw FacadeOPL.wrapPossibleSpuriousIloException(e);
			} catch (RuntimeException e) {
				op.fail(e);
				throw e;
			}
		} else {
			throw new UnimplementedConditionException();
		}
		return comandoSolver;
	}

	/**
	 * CREATE MODEL
	 *
	 * @param oplFactory
	 * @param errorHandler
	 * @param oplModelDefinition
	 * @param comandoSolver
	 * @return
	 */
	protected IloOplModel createModelOnSolver(IloOplFactory oplFactory, CustomErrorHandler errorHandler, IloOplModelDefinition oplModelDefinition, CommandCplex comandoSolver) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.CreateModelCplex).start();
		try {
			IloOplModel oplModel = oplFactory.createOplModel(oplModelDefinition, comandoSolver.getCplex());
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			op.ok();
			return oplModel;
		} catch (OplModelException e) {
			op.fail(e);
			throw e;
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

	/**
	 * Validar DataSource.
	 * @param oplModel
	 */
	protected void defineDataSources(IloOplModel oplModel) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.DefineDataSources).start();
		try {
			for (FonteDados fonte : this.dataSources) {
				loggerExecucao.debug("Definir fonte '{}'.", fonte.getNome());
				fonte.definir(oplModel);
			}
			op.ok();
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		}
	}

	/**
	 * Validar DataSink.
	 * @param oplModel
	 */
	protected void defineDataSinks(IloOplModel oplModel) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.DefineDataSinks).start();
		try {
			for (ConsumidorDados consumidor : this.dataSinks) {
				loggerExecucao.debug("Definir consumidor '{}'.", consumidor.getNome());
				consumidor.definir(oplModel);
			}
			op.ok();
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		}
	}

	/**
	 * REGISTRAR
	 *
	 * Aplicar fontes de dados.
	 * A leitura ocorre no futuro na próxima etapa, ou seja, ao gerar o modelo.
	 */
	protected void registerDataSources(IloOplModel oplModel) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.RegisterDataSources).start();
		try {
			for (FonteDados fonte : this.dataSources) {
				loggerExecucao.debug("Preparar fonte '{}'.", fonte.getNome());
				fonte.preparar(oplModel);
			}
			for (FonteDados fonte : this.dataSources) {
				loggerExecucao.debug("Importar fonte '{}'.", fonte.getNome());
				fonte.importar(oplModel);
			}
			op.ok();
//		} catch (IOException e) {
//			op.fail(e);
//			throw new MotivoRuntimeException(MotivosExecucao.RegisterDataSources);
		} catch (Exception e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e).operation(RegisterDataSources);
		}
	}

	/**
	 * Finalizar data sources
	 */
	protected void finalizarDataSources(IloOplModel oplModel) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.RegisterDataSources).start();
		try {
			for (FonteDados fonte : this.dataSources) {
				loggerExecucao.debug("Finalizar fonte '{}'.", fonte.getNome());
				fonte.finalizar(oplModel);
			}
			op.ok();
//		} catch (IOException e) {
//			op.fail(e);
//			throw new MotivoRuntimeException(MotivosExecucao.RegisterDataSources);
		} catch (Exception e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e).operation(RegisterDataSources);
		}
	}

	/**
	 * EXPORTAR
	 *
	 * Aplicar fontes de dados.
	 * A leitura ocorre no futuro na próxima etapa, ou seja, ao gerar o modelo.
	 */
	protected void exportDataSinks(IloOplModel oplModel) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.ExportDataSinks).start();
		try {
			for (ConsumidorDados consumidor : this.dataSinks) {
				loggerExecucao.debug("Preparar consumidor '{}'.", consumidor.getNome());
				consumidor.preparar(oplModel);
			}
			for (ConsumidorDados consumidor : this.dataSinks) {
				loggerExecucao.debug("Exportar consumidor '{}'.", consumidor.getNome());
				consumidor.exportar(oplModel);
			}
			for (ConsumidorDados consumidor : this.dataSinks) {
				loggerExecucao.debug("Finalizar consumidor '{}'.", consumidor.getNome());
				consumidor.finalizar(oplModel);
			}
			op.ok();
//		} catch (IOException e) {
//			op.fail(e);
//			throw new MotivoRuntimeException(MotivosExecucao.ExportDataSinks);
		} catch (Exception e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e).operation(ExportDataSinks);
		}
	}

	/**
	 * GERAR.
	 *
	 * Traduzir modelo OPL para modelo do respectivo solver (CPLEX).
	 */
	protected void realizeModelOnSolver(IloOplFactory oplFactory, CustomErrorHandler errorHandler, IloOplModel oplModel, CommandCplex comandoSolver) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.RealizeModelOnCplex).start();
		try {
				oplModel.generate();
				FacadeOPL.throwPossibleOplModelException(errorHandler);
				op.ok();
		} catch (OplModelException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

	protected void executeSolver(IloOplModel oplModel, SolverCommand comandoSolver) throws NoSolutionException {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.ExecuteSolver).start();
		try {
			ComandoOPL comandoOpl = new ComandoOPL(oplModel, configuracaoOpl, comandoSolver);
			comandoOpl.executar();
			op.ok();
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		} catch (NoSolutionException e) {
			op.fail(e);
			throw e;
		}
	}

	/**
	 * PÓS PROCESSAR.
	 * @param oplModel
	 * @param errorHandler
	 */
	protected void executePosProcessing(IloOplModel oplModel, CustomErrorHandler errorHandler) {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.ExecutePosProcessing).start();
		try {
			oplModel.postProcess();
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			op.ok();
		} catch (Exception e) {
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

			/*
			 * TODO Chamar profiler da forma certa.
			 */
//			IloOplProfiler profiler = oplSettings.getProfiler();
//			profiler.printReport(System.out);
//			@SuppressWarnings("unchecked")
//			Iterator<IloOplElement> elementItr = oplModel.getElementIterator();
//			while (elementItr.hasNext()) {
//				IloOplElement element = elementItr.next();
//				if (element.isData()) System.out.print("d");
//				if (element.isPostProcessing()) System.out.print("p");
//				if (element.isInternalData()) System.out.print("i");
//				if (element.isExternalData()) System.out.print("e");
//				if (element.isCalculated()) System.out.print("c");
//				if (element.isDecisionVariable()) System.out.print("v");
//				if (element.isDecisionExpression()) System.out.print("s");
//				System.out.print(" : ");
//				System.out.print(element.getName());
//				System.out.print(" = ");
//				System.out.print(element.toStringDisplay());
//				System.out.println(";");
//			}
//			taskGeral.ok();
//		} catch (RuntimeException e) {
//			taskGeral.fail(e);
//			throw e;
//		} catch (MotivoException e) {
//			taskGeral.fail(e);
//			throw e;
//		}

	/**
	 * MODEL DEFINITION.
	 *
	 * Tenta adiantar a validação sintaticamente do modelo.
	 * Para isto tenta acessar qualquer uma das definições para forçar a compilação do modelo.
	 * @throws OplModelException
	 */
	protected IloOplModelDefinition parseModel(IloOplFactory oplFactory, CustomErrorHandler errorHandler, IloOplSettings oplSettings, IloOplModelSource oplModelSource) throws OplModelException {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.ParseModel).start();
		try {
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			IloOplModelDefinition oplModelDefinition = oplFactory.createOplModelDefinition(oplModelSource, oplSettings);
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			/* TODO Investigar o tipo de exceção que é lançada quando o elemento não existe! */
			/*
			 * Acessa uma definição abritrária, neste caso, a primeira.
			 * Foi uma forma encontrada para forçar o OPL compilar totalmente a definição antes
			 * de gerar o modelo a partir dos dados.
			 * Se uma das definições estiver errada, então o OPL poderá lançar uma exceção ou registar no errorHandler.
			 */
			try {
				@SuppressWarnings("unchecked")
				Iterator<IloOplElementDefinition> iterator = oplModelDefinition.getElementDefinitionIterator();
				while (iterator.hasNext()) {
					iterator.next().getName();
				}
				FacadeOPL.throwPossibleOplModelException(errorHandler);
			} catch (Exception e) {
				FacadeOPL.throwPossibleOplModelException(errorHandler);
				throw FacadeOPL.wrapPossibleSpuriousIloException(e);
			}
			op.ok();
			return oplModelDefinition;
		} catch (OplModelException e) {
			op.fail(e);
			throw e;
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			op.fail(e);
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

	/**
	 * Check if there are {@link OplModelParseError}s that were reported to the
	 * {@link CustomErrorHandler} and thrown a {@link OplModelException} if
	 * affirmative.
	 *
	 * @throws OplModelException
	 *            if there are {@link OplModelParseError}s reported to the
	 *            {@link CustomErrorHandler}
	 */
	public static void throwPossibleOplModelException(CustomErrorHandler errorHandler) throws OplModelException {
		if (errorHandler.temErros()) {
			throw new OplModelException(errorHandler.getParseErrors());
		}
	}

	/**
	 * Check if exception is {@link IloException}, report that and wrap the
	 * exception to a {@link UnhandledException}. Many OPL method calls do not
	 * declare to throw any exception, but violate the method signature by
	 * spuriously throwing a {@link IloException} from within JNI.
	 * <p>
	 * This method shall be called from within a <code>catch (Exception e)</code>
	 * clause, that wraps all OPL method calls.
	 *
	 * <pre>
	 * try { ... }
	 * catch (Exception e) {
	 *   throw wrapSpuriousIloException(e);
	 * }
	 * </pre>
	 *
	 * If the model is being handled, then call
	 * {@link #throwPossibleOplModelException(CustomErrorHandler)} before to
	 * ensure that model parse erros that were reported spuriously and
	 * inadequately as IloException will be rethrown as a
	 * {@link OplModelException}.
	 *
	 * @param e
	 *           The exception to be checked and wrapped.
	 * @return The {@link RuntimeException} wrapping the exception.
	 */
	protected static RuntimeException wrapPossibleSpuriousIloException(Exception e) {
		if (e instanceof IloException)  LoggerFactory.getLogger(FacadeOPL.class).warn("JNI has thrown undeclared exception.", e);
		UnhandledException newE = new UnhandledException(e);
		StackTraceElement[] st = newE.getStackTrace();
		st = Arrays.copyOfRange(st, 1, st.length);
		newE.setStackTrace(st);
		return newE;
	}

	/**
	 * LER MODELO.
	 *
	 * Obter a definição de modelo do provedor.
	 * @param errorHandler
	 *
	 * @throws OplModelException
	 */
	protected IloOplModelSource loadModel(IloOplFactory oplFactory, CustomErrorHandler errorHandler) throws OplModelException {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.LoadModel).start();
		try {
			/* Aciona o ModeloProvider para recuperar o código fonte do modelo. */
			String textoModelo = null;
			textoModelo = modeloProvider.getConteudo();
			IloOplModelSource oplModelSource = oplFactory.createOplModelSourceFromString(textoModelo, modeloProvider.getNome());
			logModelo(textoModelo);
			/* No test cases reported errors at this point, check just to ensure. */
			FacadeOPL.throwPossibleOplModelException(errorHandler);
			op.ok();
			return oplModelSource;
//		} catch (IOException e) {
//			op.fail(e);
//			throw new MotivoRuntimeException(e, MotivosExecucao.LoadModel);
		} catch (IOException e) {
			op.fail(e);
			throw RichRuntimeException.enrich(e).operation(LoadModel);
		} catch (Exception e) {
			op.fail(e);
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

	/**
	 * CONFIGURAÇÃO.
	 *
	 * Cria o objeto de configuração que determina o comportamento da execução do OPL.
	 * A API do IloOplSettings requer que o error handler seja criado junto com a configuração.
	 * Muitas inconsistências são reportadas através de mensagens pelo error handler ao invés de exceções.
	 * @throws OplSettingsException
	 */
	protected IloOplSettings createSettings(IloOplFactory oplFactory, CustomErrorHandler errorHandler) throws OplSettingsException {
		Meter op = MeterFactory.getMeter(loggerMeter, FacadeOPL.CreateSettings).start();
		try {
			IloOplSettings oplSettings = oplFactory.createOplSettings(errorHandler);

			/*
			 * Parâmetros que controlam como gerar listagens de dados.
			 * Não é necessariamente a melhor forma de visualização dos dados.
			 * Mas garante que a listagem pode ser utilizada no OPL studio ou para chamar o OPL na linha de comando
			 * com os mesmos dados passados para o FacadeOPL.
			 */
			oplSettings.setDisplayWidth(120);
			oplSettings.setDisplayPrecision(2);
			oplSettings.setDisplayOnePerLine(false); // se fosse true, então listagens ficariam muito longas
			oplSettings.setDisplayWithComponentName(false); // se fosse true, então o opl não consegue ler o próprio arquivo que ele escreve
			oplSettings.setDisplayWithIndex(false); // se fosse true, então o opl não consegue ler o próprio arquivo que ele escreve

			/*
			 * No modo debug, faz verificações adicionais e não descarta informação temporária.
			 * Desta forma é possível inspencionar os arquivos temporários caso o solucionador
			 * apresente um comportamento diferente do esperado.
			 */
			if (configuracaoOpl.getModoDebug()) {
				loggerExecucao.warn("OPL configurado em modo DEBUG.");
			}
			oplSettings.setKeepTmpFiles(configuracaoOpl.getModoDebug());
			oplSettings.setWithWarnings(configuracaoOpl.getModoDebug());
			oplSettings.setSkipWarnNeverUsedElements(! configuracaoOpl.getModoDebug());

			/*
			 * Atribui diretório temporário para execução e garante que ele exista.
			 */
			try {
				if (configuracaoOpl.temCaminhoTmp()) {
					File caminho = configuracaoOpl.getCaminhoAbsolutoTmp();
					FacadeOPL.assureDiretory(caminho);
					oplSettings.setTmpDir(caminho.getAbsolutePath());
					loggerExecucao.warn("Usar diretório temporário de execução: {}.", caminho.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new OplSettingsException(OplSettingsException.Reason.TMP_DIR, e);
			}

			/*
			 * Se desejado, garante que o modelo CPLEX ou CP utilize o mesmo nome de variáveis que o modelo OPL.
			 * Isto facilita o DEBUG se o modelo CPLEX ou CP apresenta comportamento diferente do esperado
			 * de acordo com o modelo OPL
			 */
			boolean usarNomes = configuracaoOpl.getUsarNomes();
			oplSettings.setWithNames(usarNomes);
			oplSettings.setWithLocations(usarNomes);
			oplSettings.setForceElementUsage(usarNomes);
			if (usarNomes) {
				loggerExecucao.debug("Preservar nomes entre modelos.");
			}

			boolean usarValidacao = configuracaoOpl.getUsarValidacao();
			oplSettings.setSkipAssert(! usarValidacao);
			if (usarValidacao) {
				loggerExecucao.debug("Validar modelo com asserts.");
			} else {
				loggerExecucao.debug("Não validar modelo com asserts.");
			}

			// TODO: Entender para que serve este parâmetro, que ainda não foi aproveitado.
			// oplSettings.setUndefinedDataError

			// TODO: Incorporar o profiler de uma forma melhor pensada.
//				oplSettings.setProfiler(oplFactory.createOplProfiler());


			/* TODO achar um caso onde iso sja possível. */
//				throwPossibleOplModelException(errorHandler);

			op.ok();
			return oplSettings;
		} catch (OplSettingsException e) {
			op.fail(e);
			throw e;
		} catch (RuntimeException e) {
			op.fail(e);
			throw e;
		} catch (Exception e) {
			op.fail(e);
			throw FacadeOPL.wrapPossibleSpuriousIloException(e);
		}
	}

	/**
	 * INICIALIZAÇÃO.
	 *
	 * Acessa a classe da OplFactory e a configura para mostrar erros de inicialização de acordo com a configuração.
	 * Isto também força o Java a carregar as DLLs do ILOG, caso isto não tenha ocorrido até então.
	 * Se uma DLL não puder ser encontrada, então ocorrerá um ExceptionInInitializerError que será reportado como um MotivoException.
	 *
	 * A documentação do OPL não deixa claro qual o comportamento esperado em caso de erro de licença.
	 * Este caso será tratado de forma genérica, igual ao erro de não encontar DLL.
	 * TODO Iniciar só na primeira vez que o Facade for chamado.
	 * TODO Imprimir o library path e colocar uma mensagem explicando como definir o library path na linha de comando.
	 * TODO Se algo der errado, verificar se a DLL ou SO existe nos paths de interesse.
	 * TODO Ser algo der errado, ver cabeçalho da dll/os para ver se ela é 32 ou 64 bits. Avisar se for diferente da arquitetura da JVM.
	 * @return
	 *
	 * @throws LoadOplLibraryException
	 */
	protected IloOplFactory loadLibrary() throws LoadOplLibraryException{
		Meter op;
		op = MeterFactory.getMeter(loggerMeter, FacadeOPL.LoadModel).start();
		try {
			IloOplFactory.getVersion();
			IloOplFactory.setDebugMode(configuracaoOpl.getModoDebug());
			IloOplFactory.setDebugModeWarning(configuracaoOpl.getModoDebug());
			IloOplFactory oplFactory = new IloOplFactory();
			op.ok();
			return oplFactory;
		} catch (ExceptionInInitializerError e) {
			op.fail(e);
			throw new LoadOplLibraryException(e);
		} catch (Exception e) {
			op.fail(e);
			throw new LoadOplLibraryException(e);
		}
	}

	/** Registra no log uma cópia do modelo. */
	protected void logModelo(String textoModelo) {
		if(! loggerModelo.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerModelo);
		ps.println(textoModelo);
		ps.close();
	}
}
