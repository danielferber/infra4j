package br.pro.danielferber.bootstrap;

/**
 * Responsável por executar as tarefas para iniciar todos os componentes do
 * servidor durante a inicialização e por finalizar os componentes durante o
 * término do servidor.
 * 
 * @author Daniel Felix Ferber
 * 
 */
public interface BootstrapHandler {
	/**
	 * Executa a inicialização do servidor.
	 * @throws Exception Erro durante a inicialização do servidor.
	 */
	void startServer() throws Exception;
	/**
	 * Executa a finalização do servidor.
	 * @throws Exception Erro durante a finalização do servidor.
	 */
	void stopServer() throws Exception;
}
