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

import static infra.exception.Assert.Argument;
import static infra.exception.Assert.Attribute;
import ilog.concert.IloException;
import infra.exception.controlstate.design.UnsupportedMethodException;

import org.slf4j.Logger;


/**
 * Callback que registra o progresso do CPLEX no log.
 * @author Daniel Felix Ferber
 */
class ContinuousCallback extends ilog.cplex.IloCplex.ContinuousCallback {
	private final Logger logger;
	private final int numeroPassosEntreLogs;
	private int contadorPassos = 0;

	public ContinuousCallback(Logger logger, int numeroPassosEntreLogs) {
		super();
		Argument.notNull(logger);
		Argument.positive(numeroPassosEntreLogs);

		this.logger = logger;
		this.numeroPassosEntreLogs = numeroPassosEntreLogs;
	}

	@Override
	protected void main() throws IloException {
		Attribute.notNull(logger);

		if (! logger.isInfoEnabled()) return;
		if (contadorPassos-- > 0)  return;
		contadorPassos = numeroPassosEntreLogs;

		contadorPassos  = numeroPassosEntreLogs;
		this.logger.info(
				String.format(
						"Simplex: nIter=%d; inf=%.1f; dualInf=%.1f; objVal=%.1f; nCol=%d; nRow=%d; nQC=%d",
						Integer.valueOf(getNiterations()),
						Double.valueOf(getInfeasibility()),
						Double.valueOf(getDualInfeasibility()),
						Double.valueOf(getObjValue()),
						Integer.valueOf(getNcols()),
						Integer.valueOf(getNrows()),
						Integer.valueOf(getNQCs())
				));
	}

	@Override
	public int hashCode() { throw new UnsupportedMethodException(); }
	@Override
	public boolean equals(Object obj) { throw new UnsupportedMethodException(); }

}
