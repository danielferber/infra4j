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

import ilog.opl.IloCustomOplErrorHandler;
import ilog.opl.IloOplFactory;
import ilog.opl.IloOplLocation;
import ilog.opl.IloOplMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;


/**
 * Callback que registra os erros encontrados ao realizar o modelo OPL.
 * @author "Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class CustomErrorHandler extends IloCustomOplErrorHandler {
	private static final String messagePattern = "{} de {}-{} a {}-{}: {} ({})";
	private Logger logger;
	private List<OplModelParseError> errosModeloOpl = new ArrayList<OplModelParseError>();
	private boolean temErro = false;

	public CustomErrorHandler(IloOplFactory oplFactory, Logger logger) {
		super(oplFactory);
		this.logger = logger;
	}

	public boolean temErros() { return temErro; }

	public List<OplModelParseError> getParseErrors() { return Collections.unmodifiableList(errosModeloOpl); }

	@Override
	public boolean customHandleWarning(IloOplMessage message, IloOplLocation location) {
		logger.warn(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new OplModelParseError(OplModelParseError.Level.WARNING, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		return true;
	}

	@Override
	public boolean customHandleError(IloOplMessage message, IloOplLocation location) {
		logger.error(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new OplModelParseError(OplModelParseError.Level.ERROR, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		temErro = true;
		return true;
	}

	@Override
	public boolean customHandleFatal(IloOplMessage message, IloOplLocation location) {
		logger.error(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new OplModelParseError(OplModelParseError.Level.FATAL, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		temErro = true;
		return true;
	}
}
