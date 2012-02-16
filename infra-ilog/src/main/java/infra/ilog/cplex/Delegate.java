package infra.ilog.cplex;

import ilog.cplex.IloCplex;
import infra.ilog.ComandoSolver;

/**
 * O {@link ComandoSolver} delega para esta classe algumas decisões sobre como
 * executar o CPLEX. Veja 'delegate' design pattern.
 */
public interface Delegate {
	/**
	 * Permite decidir se deve realizar mais uma interação do solucionador.
	 * @param cplex instância do CPLEX
	 * @param numeroIteracao número da iteração da execução do CPLEX
	 * @param configuracao
	 * @return true se deve realizar mais uma execução, false se deve interromper
	 * TODO noremear para autorizarIteracao()
	 */
	boolean antesExecucao(IloCplex cplex, int numeroIteracao, ConfiguracaoCplex configuracao);
	/**
	 * Permite decidir se deve realizar mais uma interação do solucionador.
	 * @param cplex instância do CPLEX
	 * @param numeroIteracao número da iteração da execução do CPLEX
	 * @param configuracao
	 * @return true se deve realizar mais uma execução, false se deve interromper
	 * TODO renomear para autorizarProximaIteracao()
	 */
	boolean depoisExecucao(IloCplex cplex, int numeroIteracao, ConfiguracaoCplex configuracao);
}