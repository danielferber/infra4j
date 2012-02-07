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

import ilog.cplex.IloCplex;
import ilog.opl.IloOplElementDefinition;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import ilog.opl.IloOplModelSource;
import ilog.opl.IloOplSettings;
import infra.exception.assertions.controlstate.unimplemented.UnimplementedConditionException;
import infra.exception.assertions.datastate.NullArgumentException;
import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;
import infra.ilog.ComandoSolver;
import infra.ilog.cplex.ComandoCplex;
import infra.ilog.cplex.ConfiguracaoCplex;
import infra.slf4j.LoggerFactory;
import infra.slf4j.Meter;
import infra.slf4j.MeterFactory;

import java.io.File;
import java.io.FileNotFoundException;
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
	protected static final Logger logger = LoggerFactory.getLogger("ilog.opl");
	protected static final Logger loggerMeter = LoggerFactory.getLogger(FacadeOPL.logger, "meter");
	protected static final Logger loggerExecucao = LoggerFactory.getLogger(FacadeOPL.logger, "execucao");
	protected static final Logger loggerModelo = LoggerFactory.getLogger(FacadeOPL.logger, "dados.modelo");

	/** Container com as configurações para esta instância. */
	private final ConfiguracaoOPL configuracaoOpl;
	protected ConfiguracaoOPL getConfiguracaoOpl() { return configuracaoOpl; }

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

	public static enum MotivoExecutarOpl implements Motivo {
		ACESSAR_BIBLIOTECA("Falha ao acessar biblioteca do solucionador OPL."),
		CRIAR_SETTINGS("Falha ao criar configuração do solucionador OPL."),
		OBTER_MODELO("Falha ao obter modelo do solucionador OPL."),
		CRIAR_CPLEX("Falha ao criar solucionador CPLEX."),
		DATASOURCE("Falha ao obter dados do datasource."),
		DATASINK("Falha ao obter dados do datasink."),
		REALIZAR_MODELO("Falha ao realizar modelo OPL no solucionador."),
		POS_PROCESSAMENTO("Falha ao realizar pós-processamento do modelo OPL."),
		OBTER_SOLUCAO("Falha ao obter solução."),
		DEFINIR_DATASOURCE("Falha ao definir fonte de dados."),
		DEFINIR_DATASINK("Falha ao definir consumidor de dados."),
		;

		public final String message;
		private MotivoExecutarOpl(String message) { this.message = message;	}
		@Override
		public String getMensagem() { return this.message; }
		@Override
		public String getOperacao() { return "Erro ao iniciar solucionador CPLEX."; }
	}

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
	}

	public void executar() throws MotivoException {
		Meter taskGeral = MeterFactory.getMeter(FacadeOPL.logger, "facadeOpl").setMessage("Executar FacadeOPL.").start();
		Meter task = null;
		IloOplModelDefinition oplModelDefinition = null;
		CustomErrorHandler customOplErrorHandler = null;
		IloOplSettings oplSettings = null;

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
			task = MeterFactory.getMeter(taskGeral, "acessarBiblioteca").setMessage("Acessar bibliotecas dinâmicas.").start();
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
			task = MeterFactory.getMeter(taskGeral, "configurar").setMessage("Configurar OPL.").start();
			try {
				/* Error handler. */
				customOplErrorHandler = new CustomErrorHandler(oplFactory, FacadeOPL.loggerExecucao);
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
					FacadeOPL.loggerExecucao.warn("OPL configurado em modo DEBUG.");
				}
				oplSettings.setKeepTmpFiles(configuracaoOpl.getModoDebug());
				oplSettings.setWithWarnings(configuracaoOpl.getModoDebug());
				oplSettings.setSkipWarnNeverUsedElements(! configuracaoOpl.getModoDebug());

				/*
				 * Atribui diretório temporário para execução e garante que ele exista.
				 */
				if (configuracaoOpl.temCaminhoTmp()) {
					File caminho = configuracaoOpl.getCaminhoAbsolutoTmp();
					if (! caminho.exists()) {
						if (! caminho.mkdirs()) {
							FacadeOPL.loggerExecucao.warn("Impossível criar diretório{}.", caminho.getAbsolutePath());
						}
					}
					if (! caminho.exists()) {
						throw new FileNotFoundException(caminho.getAbsolutePath());
					}
					oplSettings.setTmpDir(caminho.getAbsolutePath());
					FacadeOPL.loggerExecucao.warn("Usar diretório temporário de execução: {}.", caminho.getAbsolutePath());
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
					FacadeOPL.loggerExecucao.debug("Preservar nomes entre modelos.");
				}

				boolean usarValidacao = configuracaoOpl.getUsarValidacao();
				oplSettings.setSkipAssert(! usarValidacao);
				if (usarValidacao) {
					FacadeOPL.loggerExecucao.debug("Validar modelo com asserts.");
				} else {
					FacadeOPL.loggerExecucao.debug("Não validar modelo com asserts.");
				}

				// TODO: Entender para que serve este parâmetro, que ainda não foi aproveitado.
				// oplSettings.setUndefinedDataError

				// TODO: Incorporar o profiler de uma forma melhor pensada.
//				oplSettings.setProfiler(oplFactory.createOplProfiler());

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.CRIAR_SETTINGS);
			}

			/*
			 * DEFINIÇÃO DE MODELO.
			 *
			 * Obter a definição de modelo do provedor.
			 * Verifica se a formulação está sintaticamente correta.
			 * Para isto tenta acessar qualquer uma das definições para forçar a compilação do modelo.
			 */
			task = MeterFactory.getMeter(taskGeral, "definirModelo").setMessage("Obter definições do modelo.").start();
			try {
				/* Aciona o ModeloProvider para recuperar o código fonte do modelo. */
				String textoModelo = modeloProvider.getConteudo();
				PrintStream ps = LoggerFactory.getInfoPrintStream(FacadeOPL.loggerModelo);
				ps.println(textoModelo);
				ps.close();

				IloOplModelSource oplModelSource = this.oplFactory.createOplModelSourceFromString(textoModelo, modeloProvider.getNome());

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				oplModelDefinition = this.oplFactory.createOplModelDefinition(oplModelSource, oplSettings);

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/*
				 * Acessa uma definição abritrária, neste caso, a primeira.
				 * Foi uma forma encontrada para forçar o OPL compilar totalmente a definição antes
				 * de gerar o modelo a partir dos dados.
				 * Se uma das definições estiver errada, então o OPL poderá lançar uma exceção ou registar no errorHandler.
				 */
				@SuppressWarnings("unchecked")
				Iterator<IloOplElementDefinition> iterator = oplModelDefinition.getElementDefinitionIterator();
				iterator.next().getName();

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/* Avisa os Data Sources sobre o modelo. */

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.OBTER_MODELO);
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
				task = MeterFactory.getMeter(FacadeOPL.loggerMeter, "criarCplex").setMessage("Criar CPLEX.").start();
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
			task = MeterFactory.getMeter(taskGeral, "definirFontes").setMessage("Definir fontes de dados.").start();
			try {
				for (FonteDados fonte : this.dataSources) {
					FacadeOPL.loggerExecucao.debug("Definir fonte '{}'.", fonte.getNome());
					fonte.definir(this.oplModel);
				}
				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.DEFINIR_DATASOURCE);
			}
			task = MeterFactory.getMeter(taskGeral, "definirConsumidors").setMessage("Definir consumidores de dados.").start();
			try {
				for (ConsumidorDados consumidor : this.dataSinks) {
					FacadeOPL.loggerExecucao.debug("Definir consumidor '{}'.", consumidor.getNome());
					consumidor.definir(this.oplModel);
				}
				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.DEFINIR_DATASINK);
			}

			/*
			 * IMPORTAR
			 *
			 * Aplicar fontes de dados.
			 * A leitura ocorre no futuro na próxima etapa, ou seja, ao gerar o modelo.
			 */
			task = MeterFactory.getMeter(taskGeral, "importarDados").setMessage("Importar dados das fontes.").start();
			try {
				for (FonteDados fonte : this.dataSources) {
					FacadeOPL.loggerExecucao.debug("Preparar fonte '{}'.", fonte.getNome());
					fonte.preparar(this.oplModel);
				}
				for (FonteDados fonte : this.dataSources) {
					FacadeOPL.loggerExecucao.debug("Importar fonte '{}'.", fonte.getNome());
					fonte.importar(this.oplModel);
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.DATASOURCE);
			}

			/*
			 * GERAR.
			 *
			 * Traduzir modelo OPL para modelo do respectivo solver (CPLEX).
			 */
			task = MeterFactory.getMeter(taskGeral, "realizar").setMessage("Realizar modelo OPL no solucionador").start();
			try {
				this.oplModel.generate();

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				/* Fecha os datasources abertos no passo anterior. */
				for (FonteDados fonte : this.dataSources) {
					FacadeOPL.loggerExecucao.debug("Finalizar fonte '{}'.", fonte.getNome());
					fonte.finalizar(this.oplModel);
				}

				/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
				customOplErrorHandler.throwExceptionOnError();

				task.ok();
			} catch (Exception e) {
				task.fail(e);
				throw new MotivoException(e, MotivoExecutarOpl.REALIZAR_MODELO);
			}

			/*
			 * EXECUTAR.
			 */
			task = MeterFactory.getMeter(FacadeOPL.loggerMeter, "executar").setMessage("Executar solucionador").start();
			try {
				ComandoOPL comandoOpl = new ComandoOPL(this.oplModel, configuracaoOpl, comandoSolver);
				comandoOpl.executar();

				task.ok();
			} catch (RuntimeException e) {
				task.fail(e);
				throw e;
			} catch (MotivoException e) {
				task.fail(e);
				throw e;
			}

			/*
			 * PÓS PROCESSAR.
			 */
			task = MeterFactory.getMeter(taskGeral, "posprocessar").setMessage("Realizar pós-processamento do modelo OPL.").start();
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
			task = MeterFactory.getMeter(taskGeral, "exportarDados").setMessage("Exportar dados para consumidores.").start();
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
			/*
			 * TODO Ainda está chamando o handler a moda antiga.
			 */
	//		try {
	//			modelHandler.lerSolucao(this.oplModel);
	//
	//			/* Consulta o errorHandler por erros, que devem ser reportados por Exceptions. */
	//			errorHandler.throwExceptionOnError();
	//
	//			op.success();
	//		} catch (Exception e) {
	//			op.failure(e);
	//			throw new MotivoException(e, MotivoExecutarOpl.OBTER_SOLUCAO);
	//		}

			taskGeral.ok();
		} catch (RuntimeException e) {
			taskGeral.fail(e);
			throw e;
		} catch (MotivoException e) {
			taskGeral.fail(e);
			throw e;
		}
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
			FacadeOPL.loggerExecucao.error("Uma instância oplFactory não foi finalizada corretamente!");
			this.oplFactory.end();
//			não é considerado uma boa prática....
//			this.oplFactory = null;
		}
		super.finalize();
	}
}
