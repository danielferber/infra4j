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

import ilog.opl.IloOplDataSerializer;
import ilog.opl.IloOplElement;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplSettings;
import infra.ilog.opl.ConsumidorDados;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.regex.Pattern;


/**
 * Implementação abstrata para {@link ConsumidorDados} que escreve dados em recursos
 * representados por streams.
 *
 * @author Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public abstract class AbstractConsumidorDadosStream extends AbstractConsumidorDados {
	private final Collection<Pattern> includePattern;
	private final Collection<Pattern> excludePattern;
	private final EnumSet<Filtro> filtro;

	public static enum Filtro{
		Calculated,
		Data,
		ExternalData,
		InternalData,
		DecisionVariable,
		DecisionExpression,
		PostProcessing
	}

	public AbstractConsumidorDadosStream(String nome, Collection<Pattern>includePattern, Collection<Pattern>excludePattern, EnumSet<Filtro> filtro) {
		super(nome);
		if (includePattern == null) {
			Collection<Pattern> list = new ArrayList<Pattern>(1);
			list.add(Pattern.compile(".*"));
			this.includePattern = Collections.unmodifiableCollection(list);
		} else {
			this.includePattern = includePattern;
		}
		if (excludePattern == null) {
			this.excludePattern = Collections.emptySet();
		} else {
			this.excludePattern = Collections.unmodifiableCollection(excludePattern);
		}
		if (filtro == null) {
			this.filtro = EnumSet.of(Filtro.DecisionVariable, Filtro.DecisionExpression);
		} else {
			this.filtro = filtro.clone();
		}
	}

	protected void exportarStream(IloOplModel oplModel, OutputStream os) {
		IloOplDataSerializer oplDataSerializer;
		IloOplFactory oplFactory = IloOplFactory.getOplFactoryFrom(oplModel);
		IloOplSettings settings = oplModel.getSettings();
		oplDataSerializer = oplFactory.createOplDataSerializer(settings, os);

		Iterator<IloOplElement> itr = oplModel.getElementIterator();
		main: while (itr.hasNext()) {
			IloOplElement oplElement = itr.next();

			/* Verifica se o tipo do elemento que satisfaz o filtro. */
			if (oplElement.isCalculated() && ! filtro.contains(Filtro.Calculated)) continue;
			if (oplElement.isData() && ! filtro.contains(Filtro.Data)) continue;
			if (oplElement.isDecisionExpression() && ! filtro.contains(Filtro.DecisionExpression)) continue;
			if (oplElement.isDecisionVariable() && ! filtro.contains(Filtro.DecisionVariable)) continue;
			if (oplElement.isExternalData() && ! filtro.contains(Filtro.ExternalData)) continue;
			if (oplElement.isInternalData() && ! filtro.contains(Filtro.InternalData)) continue;
			if (oplElement.isPostProcessing() && ! filtro.contains(Filtro.PostProcessing)) continue;

			/* Verifica se o nome do elemento é permitido. */
			String nome = oplElement.getName();
			{
				boolean ok = false;
				for (Pattern pattern : includePattern) {
					if (pattern.matcher(nome).matches()) {
						ok = true;
						break;
					}
				}
				if (!ok) continue main;
			}
			/* Verifica se o nome do elemento é proibido. */
			for (Pattern pattern : excludePattern) {
				if (pattern.matcher(nome).matches()) {
					continue main;
				}
			}

			oplDataSerializer.printElement(oplElement);
		}
	}
}
