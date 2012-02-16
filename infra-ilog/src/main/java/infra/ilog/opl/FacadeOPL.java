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

import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.opl.IloOplElementDefinition;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import ilog.opl.IloOplModelSource;
import ilog.opl.IloOplSettings;
import infra.exception.assertions.controlstate.unimplemented.UnhandledException;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedConditionException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.ilog.cplex.ComandoCplex;
import infra.ilog.cplex.ConfiguracaoCplex;
import infra.ilog.opl.CustomErrorHandler.ErroModeloException;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;
import infra.slf4j.Operation;
import infra.slf4j.OperationFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;


/**
 * Executa o OPL conforme as configurações.
 * Isto envolve esconder a maior quantidade possível dos passos necessários segundo a API do OPL para criar o modelo CPLEX.
 *
 * @author Daniel Felix Ferber - X7WS
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
	private final ConfiguracaoCplex configuracaoCplex;
	protected ConfiguracaoCplex getConfiguracaoCplex() { 	return configuracaoCplex; }

	/** Objetos OPL controlados. */
	private IloOplFactory oplFactory;
	private IloOplModel oplModel;

	/** Acesso ao modelo. */
	private final ProvedorModelo modeloProvider;
	private final Collection<FonteDados> dataSources;
	private final Collection<ConsumidorDados> dataSinks;

	/*
	 * TODO Para a configuração e a obtenção do modelo, criar motivos específicos, pois eles
	 * podem carregar informação detalhada das falhas reportadas através do error listener.
	 */
	private final Operation ExecuteFacade = OperationFactory.getOperation("loadLibrary", "Execute facade.");
	private final Operation AcessarBiblioteca = OperationFactory.getOperation("loadLibrary", "Load OPL dynamic library.");
	private final Operation CreateSettings = OperationFactory.getOperation("createSettings", "Create OPL settings.");
	private final Operation LoadModel = OperationFactory.getOperation("loadModel", "Load OPL model.");
	private final Operation CreateCplex = OperationFactory.getOperation("createCplex", "Create CPLEX solver.");
	private final Operation LoadDataSource = OperationFactory.getOperation("loadDataSource", "Load data sources.");
	private final Operation LoadDataSink = OperationFactory.getOperation("loadDataSink", "Load data sinks.");
	private final Operation RealizeModel = OperationFactory.getOperation("realizeModel", "Realize model.");
	private final Operation ExecuteSolver = OperationFactory.getOperation("executeSolver", "Execute solver.");
	private final Operation ExecutePosProcessing = OperationFactory.getOperation("executePosProcessing", "Execute pós-processing.");

	public FacadeOPL(ConfiguracaoOPL configuracaoOpl, ConfiguracaoCplex configuracaoCplex, ProvedorModelo modeloProvider, Collection<FonteDados> dataSources, Collection<ConsumidorDados> dataSinks) {
		super();
		NullArgumentException.apply(configuracaoOpl);
		NullArgumentException.apply(modeloProvider);
		NullArgumentException.apply(configuracaoCplex);

		this.configuracaoOpl = new ConfiguracaoOPL(configuracaoOpl);
		this.configuracaoCplex = new ConfiguracaoCplex(configuracaoCplex);
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

	public void executar() /*throws MotivoException*/ {
		Meter task = null;
		IloOplModelDefinition oplModelDefinition = null;
		CustomErrorHandler customOplErrorHandler = null;
		IloOplSettings oplSettings = null;

		Meter taskGeral = MeterFactory.getMeter(loggerMeter, ExecuteFacade).start();
		try {
			/*
			 * INICIALIZAÇÃO.
			 *
			 * Acessa a classe da OplFactory e a configura para mostrar erros de inicialização de acordo com a configuração.
			 * Isto também força o Java a carregar as DLLs do ILOG, caso isto não tenha ocorrido até então.
			 * Se uma DLL não puder ser encontrada, então ocorrerá um ExceptionInInitializerError que será reportado como um MotivoException.
			 *
			 * A documentação do OPL não deixa claro qual o comportamento esperado em caso de erro de licença.
			 * Este caso será tratado de forma genérica, igual ao erro de não encontar DLL.
			 */
			/*
			 * TODO Imprimir o library path e colocar uma mensagem explicando como definir o library path na linha de comando.
			 *
			 * TODO Se algo der errado, verificar se a DLL ou SO existe nos paths de interesse.
			 *
			 * TODO Ser algo der errado, ver cabeçalho da dll/os para ver se ela é 32 ou 64 bits. Avisar se for diferente da arquitetura da JVM.
			 */
			task = MeterFactory.getMeter(loggerMeter, "acessarBiblioteca").setMessage("Acessar bibliotecas dinâmicas.").start();
			try {
				IloOplFactory.getVersion();
				IloOplFactory.setDebugMode(configuracaoOpl.getModoDebug());
				IloOplFactory.setDebugModeWarning(configuracaoOpl.getModoDebug());
				this.oplFactory = new IloOplFactory();
				task.ok();
			} catch (ExceptionInInitializerError e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.ACESSAR_BIBLIOTECA);
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.ACESSAR_BIBLIOTECA);
			}

			/*
			 * CONFIGURAÇÃO.
			 *
			 * Cria o objeto de configuração que determina o comportamento da execução do OPL.
			 * A API do IloOplSettings requer que o error handler seja criado junto com a configuração.
			 * Muitas inconsistências são reportadas através de mensagens pelo error handler ao invés de exceções.
			 */
			task = MeterFactory.getMeter(loggerMeter, "configurar").setMessage("Configurar OPL.").start();
			try {
				/* Error handler. */
				customOplErrorHandler = new CustomErrorHandler(oplFactory, loggerExecucao);
				oplSettings = oplFactory.createOplSettings(customOplErrorHandler);

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
				if (configuracaoOpl.temCaminhoTmp()) {
					File caminho = configuracaoOpl.getCaminhoAbsolutoTmp();
					FacadeOPL.assureDiretory(caminho);
					oplSettings.setTmpDir(caminho.getAbsolutePath());
					loggerExecucao.warn("Usar diretório temporário de execução: {}.", caminho.getAbsolutePath());
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

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (IOException e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.CRIAR_SETTINGS);
			} catch (ErroModeloException e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.CRIAR_SETTINGS);
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}

			/*
			 * DEFINIÇÃO DE MODELO.
			 *
			 * Obter a definição de modelo do provedor.
			 * Verifica se a formulação está sintaticamente correta.
			 * Para isto tenta acessar qualquer uma das definições para forçar a compilação do modelo.
			 */
			task = MeterFactory.getMeter(loggerMeter, "definirModelo").setMessage("Obter definições do modelo.").start();
			try {
				/* Aciona o ModeloProvider para recuperar o código fonte do modelo. */
				String textoModelo = null;
				try {
					textoModelo = modeloProvider.getConteudo();
				} catch (IOException e) {
					throw new MotivoException(e, MotivoExecutarOpl.OBTER_MODELO);
				}
				logModelo(textoModelo);

				IloOplModelSource oplModelSource = this.oplFactory.createOplModelSourceFromString(textoModelo, modeloProvider.getNome());

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				oplModelDefinition = this.oplFactory.createOplModelDefinition(oplModelSource, oplSettings);

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/*
				 * TODO Investiver o tipo de exceção que é lançada quando o elemento não existe!
				 */
				/*
				 * Acessa uma definição abritrária, neste caso, a primeira.
				 * Foi uma forma encontrada para forçar o OPL compilar totalmente a definição antes
				 * de gerar o modelo a partir dos dados.
				 * Se uma das definições estiver errada, então o OPL poderá lançar uma exceção ou registar no errorHandler.
				 */
				try {
					@SuppressWarnings("unchecked")
					Iterator<IloOplElementDefinition> iterator = oplModelDefinition.getElementDefinitionIterator();
					iterator.next().getName();
				} catch (Exception e) {
					/*
					 * Apesar de nenhum método declarar a exceção IloException, ela pode ocorrer de fato.
					 * É uma idiossincrasia da implementação JNI do OPL, onde o comportamento implementado em C
					 * difere do comportamento declarado na API Java.
					 * Normalmente, o erro também foi reportado par ao custom error handler.
					 * Por vida das dúvidas, é tratada também a possibilidade de haver um outro tipo de erro.
					 */
					if (e instanceof IloException) {
						IloException iloE = (IloException) e;
						customOplErrorHandler.throwExceptionOnError();
					}
					throw e;
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/* Avisa os Data Sources sobre o modelo. */

				task.ok();
			} catch (ErroModeloException e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.OBTER_MODELO);
			} catch (MotivoException e) {
				task.fail(e);
				throw e;
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}

			/*
			 * MODELO.
			 *
			 * Criar o modelo associado com o solucionador (CP ou CPLEX).
			 * Por enquanto, está implementado somente para CPLEX.
			 */
			ComandoSolver comandoSolver = null;
			 /*
			 * Traduzir modelo OPL para modelo do respectivo solver (CPLEX).
			 */
			if (configuracaoCplex != null) {
				task = MeterFactory.getMeter(loggerMeter, "criarCplex").setMessage("Criar CPLEX.").start();
				try {
						IloCplex cplex = this.oplFactory.createCplex();
						comandoSolver = new ComandoCplex(cplex, configuracaoCplex);
						this.oplModel = this.oplFactory.createOplModel(oplModelDefinition, cplex);

						/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
						customOplErrorHandler.throwExceptionOnError();

						task.ok();
					} catch (Exception e) {
						task.fail(e);
						throw new MotivoException(e, MotivoExecutarOpl.CRIAR_CPLEX);
					}
			} else {
				throw new UnimplementedConditionException();
			}

			/*
			 * Validar DataSource e DataSink.
			 */
			task = MeterFactory.getMeter(loggerMeter, "definirFontes").setMessage("Definir fontes de dados.").start();
			try {
				for (FonteDados fonte : this.dataSources) {
					loggerExecucao.debug("Definir fonte '{}'.", fonte.getNome());
					fonte.definir(this.oplModel);
				}
				task.ok();
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}
			task = MeterFactory.getMeter(loggerMeter, "definirConsumidors").setMessage("Definir consumidores de dados.").start();
			try {
				for (ConsumidorDados consumidor : this.dataSinks) {
					loggerExecucao.debug("Definir consumidor '{}'.", consumidor.getNome());
					consumidor.definir(this.oplModel);
				}
				task.ok();
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}

			/*
			 * IMPORTAR
			 *
			 * Aplicar fontes de dados.
			 * A leitura ocorre no futuro na próxima etapa, ou seja, ao gerar o modelo.
			 */
			task = MeterFactory.getMeter(loggerMeter, "importarDados").setMessage("Importar dados das fontes.").start();
			try {
				for (FonteDados fonte : this.dataSources) {
					loggerExecucao.debug("Preparar fonte '{}'.", fonte.getNome());
					fonte.preparar(this.oplModel);
				}
				for (FonteDados fonte : this.dataSources) {
					loggerExecucao.debug("Importar fonte '{}'.", fonte.getNome());
					fonte.importar(this.oplModel);
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (ErroModeloException e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.DATASOURCE);
//			} catch (MotivoException e) {
//				task.fail(e);
//				throw e;
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}

			/*
			 * GERAR.
			 *
			 * Traduzir modelo OPL para modelo do respectivo solver (CPLEX).
			 */
			task = MeterFactory.getMeter(loggerMeter, "realizar").setMessage("Realizar modelo OPL no solucionador").start();
			try {
				try {
					this.oplModel.generate();
				} catch (Exception e) {
					/*
					 * Apesar de nenhum método declarar a exceção IloException, ela pode ocorrer de fato.
					 * É uma idiossincrasia da implementação JNI do OPL, onde o comportamento implementado em C
					 * difere do comportamento declarado na API Java.
					 * Normalmente, o erro também foi reportado par ao custom error handler.
					 * Por vida das dúvidas, é tratada também a possibilidade de haver um outro tipo de erro.
					 */
					if (e instanceof IloException) {
						IloException iloE = (IloException) e;
						customOplErrorHandler.throwExceptionOnError();
					}
					throw e;
				}
				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/* Fecha os datasources abertos no passo anterior. */
				for (FonteDados fonte : this.dataSources) {
					loggerExecucao.debug("Finalizar fonte '{}'.", fonte.getNome());
					fonte.finalizar(this.oplModel);
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (ErroModeloException e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.REALIZAR_MODELO);
//			} catch (MotivoException e) {
//				task.fail(e);
//				throw e;
			} catch (Exception e) {
				task.fail(e);
				if (e instanceof IloException)  loggerExecucao.warn("JNI has thrown undeclared exception.", e);
				throw new UnhandledException(e);
			}

			/*
			 * EXECUTAR.
			 */
//			task = MeterFactory.getMeter(FacadeOPL.loggerMeter, "executar").setMessage("Executar solucionador").start();
//			try {
				ComandoOPL comandoOpl = new ComandoOPL(this.oplModel, configuracaoOpl, comandoSolver);
				comandoOpl.executar();
//
//				task.ok();
//			} catch (RuntimeException e) {
//				task.fail(e);
//				throw e;
//			} catch (MotivoException e) {
//				task.fail(e);
//				throw e;
//			}

			/*
			 * PÓS PROCESSAR.
			 */
			task = MeterFactory.getMeter(loggerMeter, "posprocessar").setMessage("Realizar pós-processamento do modelo OPL.").start();
			try {
				this.oplModel.postProcess();

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.POS_PROCESSAMENTO);
			}

			/*
			 * DATASINK.
			 */
			task = MeterFactory.getMeter(loggerMeter, "exportarDados").setMessage("Exportar dados para consumidores.").start();
			try {
				for (ConsumidorDados consumidor : this.dataSinks) {
					consumidor.preparar(this.oplModel);
				}
				for (ConsumidorDados consumidor : this.dataSinks) {
					consumidor.exportar(this.oplModel);
				}
				for (ConsumidorDados consumidor : this.dataSinks) {
					consumidor.finalizar(this.oplModel);
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.DATASINK);
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
	}

	/** Registra no log uma cópia do modelo. */
	protected void logModelo(String textoModelo) {
		if(! loggerModelo.isInfoEnabled()) return;
		PrintStream ps = LoggerFactory.getInfoPrintStream(loggerModelo);
		ps.println(textoModelo);
		ps.close();
	}

	public  void dispose() {
		if (this.oplFactory != null) {
			this.oplFactory.end();
			this.oplFactory = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		if (this.oplFactory != null) {
			loggerExecucao.error("Uma instância oplFactory não foi finalizada corretamente!");
			this.oplFactory.end();
//			não é considerado uma boa prática....
//			this.oplFactory = null;
		}
		super.finalize();
	}
}
