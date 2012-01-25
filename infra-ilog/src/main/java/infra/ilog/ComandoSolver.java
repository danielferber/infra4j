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
package infra.ilog;

import infra.exception.motivo.Motivo;
import infra.exception.motivo.MotivoException;

/**
 * Interface para solucionadores apresentados através de 'commando' design
 * pattern. O objeto deve encapsular no método {@link #executar()} todos os
 * passos necessários para executar o solucionador para modelo e dados
 * previamente carregados.
 * <p>
 * A execução pode lançar uma {@link MotivoException} usando um motivo {@link MotivoExecutarSolver}
 * para descrever o motivo do solucionador não ser capaz de obter uma solução.
 * <p>
 * Opcionalmente, a implementação do comando pode adotar políticas específicas para
 * alterar o comportamento do solucionador.
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 * @author Tiago de Morais Montanher (a7go) - Grupo de Pesquisa Operacional
 */
public interface ComandoSolver {

	/**
	 * Executa o solucionador sobre a instância.
	 * @throws MotivoException
	 */
	public abstract void executar() throws MotivoException;

	public static enum MotivoExecutarSolver implements Motivo {
		ILIMITADO("Problema ilimitado."),
		INVIAVEL("Problema inviável."),
		ILIMITADO_INVIAVEL("Problema inviável ou ilimitado."),
		INCOMPLETO("Resolvedor não finalizou."),
		INTERROMPIDO("Resolvedor interrompido."),
		;

		public final String message;
		private MotivoExecutarSolver(String message) { this.message = message;	}
		@Override
		public String getMensagem() { return this.message; }
		@Override
		public String getOperacao() { return "Erro ao executar solver."; }
	}
}
