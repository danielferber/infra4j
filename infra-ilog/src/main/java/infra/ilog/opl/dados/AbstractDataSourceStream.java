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
package infra.ilog.opl.dados;

import static infra.exception.Assert.Argument;
import ilog.opl.IloOplDataSource;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import infra.ilog.opl.DataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Implementação abstrata para {@link DataSource} que obtém dados de recursos representados
 * como streams.
 *
 * @author Daniel Felix Ferber
 */
public abstract class AbstractDataSourceStream extends AbstractDataSource {
	final Map<IloOplModel, InputStream> streamsAtivos = new WeakHashMap<IloOplModel, InputStream>();

	public AbstractDataSourceStream(String nome) {
		super(nome);
	}

	protected void agendarFechamentoStream(IloOplModel oplModel, InputStream is) {
		Argument.notNull(oplModel, is);
		streamsAtivos.put(oplModel, is);
	}

	protected void agendarStream(IloOplModel oplModel, InputStream is) {
		IloOplFactory oplFactory = IloOplFactory.getOplFactoryFrom(oplModel);
		IloOplDataSource oplDataSource = oplFactory.createOplDataSourceFromStream(is, nome);
		oplModel.addDataSource(oplDataSource);
	}

	@Override
	public void finish(IloOplModel oplModel) throws IOException {
		super.finish(oplModel);
		InputStream is = streamsAtivos.get(oplModel);
		if (is != null) {
			is.close();
			streamsAtivos.remove(oplModel);
		}
	}
}
