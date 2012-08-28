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

/**
 * Interface to the ILOG solvers.
 *
 * It is assumed that the model has been built previously and that the data has
 * been loaded.
 *
 * The solver is presented through the 'command' design pattern. The method
 * {@link #execute()} performs all steps required for a <u>typical</u>
 * execution of the solver. Such typical execution may be configured, depending
 * on the settings made available the implementation of this interface.
 *
 * @author Daniel Felix Ferber
 * @author Tiago de Morais Montanher
 */
public interface ComandoSolver {
	/**
	 * Runs the solver over the model and the data that were previously loaded.
	 *
	 * @throws NoSolutionException
	 *             the solver failed to obtain a solution.
	 */
	public abstract void execute() throws NoSolutionException;
}
