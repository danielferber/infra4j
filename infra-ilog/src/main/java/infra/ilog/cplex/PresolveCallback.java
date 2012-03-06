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
import infra.exception.assertions.controlstate.design.UnsupportedMethodException;
import infra.exception.assertions.datastate.NullArgumentException;

import org.slf4j.Logger;


/**
 * Callback que registra o progresso da fase de presolve.
 * @author "Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional"
 */
class PresolveCallback extends ilog.cplex.IloCplex.PresolveCallback {
	private final Logger logger;
	private final int numeroPassosEntreLogs;
	private int contadorPassos = 0;

	public PresolveCallback(Logger logger, int numeroPassosEntreLogs) {
		super();
		assert NullArgumentException.apply(logger);

		this.logger = logger;
		this.numeroPassosEntreLogs = numeroPassosEntreLogs;
	}

	@Override
	protected void main() throws IloException {
		if (! logger.isInfoEnabled()) return;
		if (contadorPassos > 0) {
			contadorPassos--;
			return;
		}
		contadorPassos = numeroPassosEntreLogs;
		this.logger.info(
				String.format("Presolve: nAggr=%d; nmodCoef=%d; nRemCol=%d; nremRow=%d; nCol=%d; nRow=%d; nQC=%d",
						getNaggregations(),
						getNmodifiedCoeffs(),
						getNremovedCols(),
						getNremovedRows(),
						getNcols(),
						getNrows(),
						getNQCs())
		);
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }

}
