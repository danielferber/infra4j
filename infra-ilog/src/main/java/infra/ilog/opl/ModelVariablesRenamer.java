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

import ilog.concert.IloException;
import ilog.concert.IloIntVarMap;
import ilog.concert.IloNumVarMap;
import ilog.concert.IloTuple;
import ilog.concert.IloTupleSchema;
import ilog.opl.IloOplArrayDefinition;
import ilog.opl.IloOplElement;
import ilog.opl.IloOplElementDefinition;
import ilog.opl.IloOplElementType.Type;
import ilog.opl.IloOplModel;
import ilog.opl.IloOplModelDefinition;
import infra.exception.controlstate.bug.ImpossibleException;
import infra.exception.controlstate.design.UnsupportedConditionException;
import infra.exception.controlstate.design.UnsupportedDataException;
import infra.exception.controlstate.unimplemented.UnimplementedMethodException;

import java.util.Iterator;


public class ModelVariablesRenamer {
	private final IloOplModel model;
	private final IloOplModelDefinition modelDefinition;
	private String prefix;

	public ModelVariablesRenamer(IloOplModel model) {
		super();
		this.model = model;
		this.modelDefinition = model.getModelDefinition();
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void run() {
		@SuppressWarnings("unchecked")
		Iterator<IloOplElement> iterator = model.getElementIterator();
		while (iterator.hasNext()) {
			IloOplElement element = iterator.next();
			if (element.isDecisionVariable()) {
				@SuppressWarnings("unused")
				IloOplElementDefinition definition = modelDefinition.getElementDefinition(element.getName());
				/*
				 * As variáveis de decisão suportadas podem ser variáveis numéricas ou arrays
				 * de variáveis numéricas.
				 */
				if (element.getElementType().equals(Type.NUM)) {
					renameNumVariable(element);
				} else if (element.getElementType().equals(Type.MAP_NUM)) {
					renameNumArrayVariable(element);
				} else if (element.getElementType().equals(Type.INT)) {
					renameIntVariable(element);
				} else if (element.getElementType().equals(Type.MAP_INT)) {
					renameIntArrayVariable(element.asIntVarMap(), element.getName());
				} else {
					throw new UnsupportedConditionException();
				}
			}
		}
	}

	private void renameNumVariable(IloOplElement element) {
		String newName = fixedName(element);
		element.asNumVar().setName(newName);
	}

	private void renameIntVariable(IloOplElement element) {
		String newName = fixedName(element);
		element.asIntVar().setName(newName);
	}

	private void renameIntArrayVariable(IloIntVarMap asIntVarMap, String name) {
		throw new UnimplementedMethodException();
	}

	private void renameNumArrayVariable(IloOplElement element) {
		String newName = fixedName(element);
		IloNumVarMap map = element.asNumVarMap();
		IloOplArrayDefinition mapDefinition = modelDefinition.getElementDefinition(element.getName()).asArray();
		recurseIntoNumArray(newName, map, mapDefinition, 0);
	}

	private void recurseIntoNumArray(String prefixoNome, IloNumVarMap mapElement, IloOplArrayDefinition mapDefinition, int currentIndexer) {
		try {
			IloOplElementDefinition indexer = mapDefinition.getIndexer(currentIndexer);
			if (indexer.getName() == null) throw new UnsupportedDataException(prefixoNome);
			IloOplElement indexElement = model.getElement(indexer.getName());

			if (indexElement.getElementType().equals(Type.SET_TUPLE)) {
				@SuppressWarnings("unchecked")
				Iterator<IloTuple> iterator = indexElement.asTupleSet().iterator();
				if (currentIndexer < mapDefinition.getDimensions()-1) {
					while (iterator.hasNext()) {
						IloTuple tuple = iterator.next();
						recurseIntoNumArray(prefixoNome+tupleToStr(tuple), mapElement.getSub(tuple), mapDefinition, currentIndexer+1);
					}
				} else {
					while (iterator.hasNext()) {
						IloTuple tuple = iterator.next();
						mapElement.get(tuple).setName(prefixoNome+tupleToStr(tuple));
					}
				}
			}
			else if (indexElement.getElementType().equals(Type.RANGE_INT)) {
				@SuppressWarnings("unchecked")
				Iterator<Integer> iterator = indexElement.asIntRange().iterator();
				if (currentIndexer < mapDefinition.getDimensions()-1) {
					while (iterator.hasNext()) {
						Integer index = iterator.next();
						recurseIntoNumArray(prefixoNome+intToStr(index), mapElement.getSub(index), mapDefinition, currentIndexer+1);
					}
				} else {
					while (iterator.hasNext()) {
						Integer index = iterator.next();
						mapElement.get(index).setName(prefixoNome+intToStr(index));
					}
				}
			} else {
				throw new UnsupportedConditionException();
			}
		} catch (IloException e) {
			throw new ImpossibleException(e);
		}
	}

	private String intToStr(Integer index) {
		return "("+index.toString()+")";
	}

	private String tupleToStr(IloTuple tuple) {
		IloTupleSchema schema = tuple.getSchema();
		StringBuilder result = new StringBuilder();
		result.append("(");
		for (int i = 0; i < schema.getSize(); i++) {
			if (i != 0) result.append(',');
			if (schema.isInt(i)) result.append(Integer.toString(tuple.getIntValue(i)));
			else if (schema.isSymbol(i)) result.append(tuple.getStringValue(i));
			else throw new UnsupportedConditionException();
		}
		result.append(")");
		return result.toString();
	}

	private String fixedName(IloOplElement element) {
		String nome = element.getName();
		String newName = null;
		if (prefix != null && nome.startsWith(prefix)) newName = nome.substring(prefix.length());
		else newName = nome.toLowerCase();
		return newName;
	}
}
