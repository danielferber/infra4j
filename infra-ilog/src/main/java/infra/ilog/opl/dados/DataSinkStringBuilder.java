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
import ilog.opl.IloOplModel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.regex.Pattern;

import org.apache.commons.io.output.WriterOutputStream;


/**
 * Utiliza um {@link StringBuilder} como consumidor de dados para o OPL.
 * Esta é a única forma de obter os dados como String: através de um {@link StringBuilder} previamente criado
 * e que será utilizado depois para gerar a String.
 */
public class DataSinkStringBuilder extends AbstractDataSinkStream {
	private final StringBuilder stringBuilder;

	public DataSinkStringBuilder(String nome, StringBuilder sb, Collection<Pattern> includePattern, Collection<Pattern> excludePattern, EnumSet<Filtro> filtro) {
		super(nome, includePattern, excludePattern, filtro);
		Argument.notNull(sb);
		this.stringBuilder = sb;
	}

	public DataSinkStringBuilder(String nome, StringBuilder sb) {
		this(nome, sb, null, null, null);
	}

	@Override
	public void consumeData(IloOplModel oplModel) throws IOException {
		StringWriter sw = new StringWriter();
		OutputStream os = new WriterOutputStream(sw);
		super.exportarStream(oplModel, os);
		stringBuilder.append(sw.getBuffer());
	}
}
