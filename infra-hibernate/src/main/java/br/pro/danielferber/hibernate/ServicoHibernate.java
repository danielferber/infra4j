package br.pro.danielferber.hibernate;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.pro.danielferber.infra.exception.assertions.controlstate.bug.ImpossibleException;
import br.pro.danielferber.infra.exception.assertions.controlstate.design.TemporalDependencyException;
import br.pro.danielferber.infra.exception.assertions.datastate.IllegalEnvironmentDataException;
import br.pro.danielferber.infra.exception.motivo.Motivo;
import br.pro.danielferber.infra.exception.motivo.MotivoException;

/**
 * Gerencia a conexão com o banco de dados, através do Hibernate.
 * <p>
 * A configuração é obtida através do mecanismo padrão do hibernate (arquivos 'hibernate.cfg.xml' na raiz do classpath e
 * '*.hbm.xml' no mesmo diretório das respectivas classes).
 * <p>
 * Para aplicações mais complexas, oferece a possibilidade de obter a configuração do subdiretório 'hibernate' em relação ao diretório de execução.
 * Este diretório conterá o arquivo 'hibernate.cfg.xml' com a configuração da conexão e arquivos .hbd.xml com o mapeamento das classes.
 * <p>
 * Se a configuração for realiazada com sucesso, então mantém uma instância singleton no {@link SessionFactory}, para
 * criar novas sessões.
 * 
 * @author Daniel Felix Ferber
 * 
 */
public class ServicoHibernate {
	private static final Logger logger = LoggerFactory.getLogger(ServicoHibernate.class);

	private final static String nomeDir = "config";
	private final static String nomeArq = "hibernate.cfg.xml";

	/** {@link SessionFactory} gerenciada pelo serviço. */
	private SessionFactory sessionFactory;
	/** Lock que sincroniza alteração de estado do serviço. */
	private Lock lock = new ReentrantLock();
	/** Singleton. */
	private static ServicoHibernate instance = new ServicoHibernate();
	
	protected ServicoHibernate() {
		logger.debug("Singleton criado.");
	}
	
	public static ServicoHibernate getInstance() { return instance; }

	public static enum MotivoConfiguracaoHibernate implements Motivo {
		CLASSPATH("Falha utilizar configuração do classpath."),
		ARQUIVO_CONFIG("Falha ao abrir arquivo de configuração."), 
		CONFIGURACAO("Falha ao interpretar configuração."), ;
		private String mensagem;

		private MotivoConfiguracaoHibernate(String mensagem) { this.mensagem = mensagem; }
		@Override public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Ler configuração de Hibernate."; }
	}
	
	public static enum MotivoHibernateStart implements Motivo {
		CONEXAO("Falha ao conectar com o servidor de banco de dados."),
		SESSAO("Falha ao criar sessao."), 
		;
		private String mensagem;
		MotivoHibernateStart(String mensagem) { this.mensagem = mensagem; }
		@Override public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Conexão com banco de dados."; }
	}

	public static enum MotivoConstrucaoTabelas implements Motivo {
		CONSTRUCAO("Falha ao construir automaticamente as tabelas do banco."),
		;
		private String mensagem;
		MotivoConstrucaoTabelas(String mensagem) { this.mensagem = mensagem; }
		@Override	public String getMensagem() { return mensagem; }
		@Override public String getOperacao() { return "Construção de tabelas."; }
	}

	/** Se a instalação utiliza uma configuração do classpath. */
	private static boolean usandoConfiguracaoClasspath = false;
	/** Se a instalação encontrou e leu um arquivo externo ao invés do padrão logback. */
	private static boolean usandoConfiguracaoEspecifica = false;
	
	public static boolean isUsandoConfiguracaoEspecifica() {
		return usandoConfiguracaoEspecifica;
	}
	
	public static boolean isUsandoConfiguracaoClasspath() {
		return usandoConfiguracaoClasspath;
	}
	
	/**
	 * Inicia a conexão com banco de dados.
	 * @param classes Uma lista (opcional) de classes, para as quais será verificado se as respectivas tabelas estão acessíveis. 
	 * @throws MotivoException #{@link MotivoHibernateStart} ou #{@link MotivoConfiguracaoHibernate}.
	 */
	public void start() throws MotivoException {		
		try {
			lock.lock();
			logger.info("Iniciando.");

			/* Evita múltiplas chamadas para o método start. */
			if (sessionFactory != null) {
				logger.error("ServicoHibernate já estava iniciado.");
				return;
			}
	
			/* Configuração. Throws MotivoConfiguracaoHibernate. */
			Configuration configuracaoHibernate = obterConfiguracao();
			
			/* Criar factory de sessões. */
			try {
				this.sessionFactory = configuracaoHibernate.buildSessionFactory();
			} catch (HibernateException e) {
				logger.error("Falha ao criar session factory.", e);
				throw new MotivoException(e, MotivoHibernateStart.SESSAO);
			}
	
			/* 
			 * Testar conectividade da sessão. 
			 * Observe que apenas criar a sessão nem sempre funciona dependendo da estratégia de cache. 
			 * Por este motivo, é executado um critério de busca sobre cada uma das tabelas de interesse para forçar conexão SQL 
			 * e também para comprovar a corretude do mapeamento. 
			 */
			Map<String, ClassMetadata> mapa = sessionFactory.getAllClassMetadata();
			Session session = sessionFactory.openSession();
			try {
				Set<String> nomesClasse = mapa.keySet();
				for (String nomeClasse : nomesClasse) {
//					ClassMetadata classMetadata = mapa.get(nomeClasse);
					Class<?> clazz = Class.forName(nomeClasse);
					logger.trace("Verificar entidade {}.", clazz.getSimpleName());
					Transaction transaction = session.beginTransaction();
					try {
						@SuppressWarnings("rawtypes")
						List list = session.createCriteria(clazz).setMaxResults(1).list();
						for (@SuppressWarnings("unused") Object l: list) {
							// nada
						}
					} finally {
						transaction.rollback();
					}
				}
				session.close(); 
			} catch (HibernateException e) {
				logger.error("Falha de conectividade com banco de dados.", e);
				session.close(); 
				this.sessionFactory.close();
				this.sessionFactory = null;
				throw new MotivoException(e, MotivoHibernateStart.CONEXAO);
			} catch (ClassNotFoundException e) {
				throw new ImpossibleException(e);
			}
			logger.info("Executando.");
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Finaliza a conexão com o banco de dados.
	 */
	public void stop() {
		try {
			lock.lock();
			logger.info("Terminando.");
			
			/* Evita múltiplas chamadas para o método start. */
			if (sessionFactory == null) {
				logger.error("ServicoHibernate já estava inativo.");
				return;
			}
			
			/*
			 * Verifica se o current session ainda possui uma transação em aberto.
			 * Existe a possibilidade de alguém ter obtido em algum momento uma sessão através de currentSession(). 
			 * Esta sessão é gerenciada pelo hibernate e é fechada somente quando a transação criada na sessão
			 * for confirmada ou cancelada. Se alguém esqueceu de confirmar ou cancelar esta sessão, então
			 * o gerente gera um alerta no log e cancela a transação. 
			 */
			Session currentSession = null;
			try {
				currentSession = sessionFactory.getCurrentSession();
			} catch (HibernateException e) {
				/* Se não existe current session, uma exceção é lançada.
				 * Esta é a situação correta. */
			}
			
			if (currentSession != null) {
				Transaction transaction = currentSession.getTransaction();
				if (transaction != null && transaction.isActive()) {
					logger.error("CurrentSession ainda possui transação ativa! Alguma thread está esquecendo de finalizar suas transações!");
					transaction.rollback();
				}
			}
			
			/*
			 * Fecha o session factory. Ele mantém o pool de conexões do com o banco de dados.
			 * O programa precisa garantir que todas as sessões criadas manualmente também sejam finalizadas.
			 */
			try {
				sessionFactory.close();
			} catch (HibernateException e) {
				logger.warn("stop: falha ao fechar fábrica de sessão hibernate.", e);
			}
			sessionFactory = null;
			
			/*
			 * Executa o garbage collector. Sabe-se que todas as sessões não fechadas e com transações abertas
			 * mantém uma conexão jdbc. A implementação do session fecha as conexões jdbc se a sessão 
			 * não possui mais referências.
			 */
			System.gc();
			logger.info("Inativo.");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Estratégia para obter a configuração do Hibernate.
	 * @return Configuração para a {@link SessionFactory} do Hibernate.
	 * @throws MotivoException #{@link MotivoConfiguracaoHibernate}.
	 */
	protected Configuration obterConfiguracao() throws MotivoException {
		logger.trace("Obter configuração para hibernate.");

		/*
		 * Primeiro, verifica se existem arquivos de configuração no classpath de acordo com a convenção do hibernate.
		 * Se existir, então não faz nada e deixa o hibernate realizar a configuração padrão de acordo com a convenção. 
		 */
		if (ServicoHibernate.class.getResource("/hibernate.xml") != null) {
			usandoConfiguracaoClasspath = true;
			try {
				return new Configuration().configure();
			} catch (HibernateException e) {
				throw new MotivoException(e, MotivoConfiguracaoHibernate.CLASSPATH);
			}
		}

		/* 
		 * Ler e aplicar configuração do arquivo XML. 
		 */
		String nomeDirAtual = System.getProperty("user.dir"); assert IllegalEnvironmentDataException.notNull(nomeDirAtual);
		File dirConfig = new File(nomeDirAtual, nomeDir);
		File arqConfig = new File(dirConfig, nomeArq);
		if (! arqConfig.exists()) {
			try {
				return new Configuration().configure();
			} catch (HibernateException e) {
				throw new MotivoException(e, MotivoConfiguracaoHibernate.CLASSPATH);
			}
		}

		/*
		 * Criar a configuração do arquivo.
		 */
		Configuration configuration;
		try {
			configuration = new Configuration();
			configuration.configure(arqConfig);
		} catch (MappingException e) {
			logger.error("Erro na configuração do Hibernate.", e);
			throw new MotivoException(e, MotivoConfiguracaoHibernate.CONFIGURACAO);
		} catch (HibernateException e) {
			throw new MotivoException(e, MotivoConfiguracaoHibernate.CONFIGURACAO);
		}

		return configuration;
	}
	
	/**
	 * Cria as tabelas automaticamente baseado na configuração.
	 * @throws MotivoException #{@link MotivoConstrucaoTabelas}
	 */
	public void criarTabelas() throws MotivoException {
		logger.info("Criar tabelas.");		
		if (sessionFactory != null) {
			throw new TemporalDependencyException("Não é possível criar tabelas se já existe conexão com o banco de dados.");
		}
		
		Configuration configHibernate = obterConfiguracao();
		
		/*
		 * Solução descrita em:
		 * http://www.rojotek.com/blog/2008/05/25/fixing-intermittent-table-not-found-errors-with-junit-when-using-hibernate-schemaexport-and-h2/ 
		 */
		SessionFactory sessionFactory = configHibernate.buildSessionFactory();
		try {
			Session session = sessionFactory.openSession();
			try {
				@SuppressWarnings("deprecation")
				Connection connection = session.connection();
				SchemaExport schemaExport = new SchemaExport(configHibernate, connection);
				/*
				 * Boolean flags: 
				 * 1) don’t output the sql to the console, 
				 * 2) do execute on the database, 
				 * 3) don’t execute drop statements, 
				 * 4) and do execute create
				 * statements.
				 */
				schemaExport.execute(false, true, false, true);
		
				if (schemaExport.getExceptions() != null && schemaExport.getExceptions().size() > 0) {
					@SuppressWarnings("rawtypes")
					List es = schemaExport.getExceptions();
					for (Object e : es) {
						logger.error("Erro na criação de tabelas.", e);
					}
					throw new MotivoException((Exception) schemaExport.getExceptions().iterator().next(), MotivoConstrucaoTabelas.CONSTRUCAO);
				}
				logger.info("Tabelas criadas com sucesso.");
			} finally {
				session.close();
			}
		} finally {
			sessionFactory.close();
		}
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
}
