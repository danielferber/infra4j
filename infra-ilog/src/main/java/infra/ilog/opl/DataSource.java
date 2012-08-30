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

import ilog.opl.IloOplModel;

import java.io.IOException;

/*
 * TODO Será que lançar mesmo IOExceptions? Não está claro que os métodos sejam
 * de IO físico.
 */
public interface DataSource {
	void define(IloOplModel oplModel);
	void prepare(IloOplModel oplModel) throws IOException;
	void produceData(IloOplModel oplModel) throws IOException;
	void finish(IloOplModel oplModel) throws IOException;
	String getName();
}