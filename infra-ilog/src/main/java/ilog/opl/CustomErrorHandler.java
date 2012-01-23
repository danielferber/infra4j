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
package ilog.opl;

import infra.exception.assertions.controlstate.bug.ImpossibleConditionException;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;


/**
 * Callback que registra os erros encontrados ao realizar o modelo OPL.
 * @author "Daniel Felix Ferber (x7ws) - Grupo de Pesquisa Operacional
 */
public class CustomErrorHandler extends IloCustomOplErrorHandler {
	private static final String messagePattern = "{} de {}-{} a {}-{}: {} ({})";
	private Logger logger;
	private List<DetalheErroModeloOpl> errosModeloOpl = new ArrayList<DetalheErroModeloOpl>();
	private boolean temErro = false;

	public CustomErrorHandler(IloOplFactory oplFactory, Logger logger) {
		super(oplFactory);
		this.logger = logger;
	}

	public boolean temErros() {
		return temErro;
	}

	@Override
	public boolean customHandleWarning(IloOplMessage message, IloOplLocation location) {
		logger.warn(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new DetalheErroModeloOpl(DetalheErroModeloOpl.Level.WARNING, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		return true;
	}

	@Override
	public boolean customHandleError(IloOplMessage message, IloOplLocation location) {
		logger.error(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new DetalheErroModeloOpl(DetalheErroModeloOpl.Level.ERROR, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		temErro = true;
		return true;
	}

	@Override
	public boolean customHandleFatal(IloOplMessage message, IloOplLocation location) {
		logger.error(CustomErrorHandler.messagePattern, new Object[] { location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getLocalized(), message.getMessageCatalogId() });
		errosModeloOpl.add(new DetalheErroModeloOpl(DetalheErroModeloOpl.Level.FATAL, location.getSource(), location.getLine(), location.getColumn(), location.getEndLine(), location.getEndColumn(), message.getMessageCatalogId(), message.getLocalized()));
		temErro = true;
		return true;
	}

	public void throwExceptionOnError() throws ErroModeloException {
		if (temErro) {
			throw new ErroModeloException("Modelo OPL contém erro(s).", errosModeloOpl);
		}
	}

	public ErroModeloException createExceptionOnError() {
		if (temErro) {
			return new ErroModeloException("Modelo OPL contém erro(s).", errosModeloOpl);
		}
		throw new ImpossibleConditionException();
	}

	public static class ErroModeloException extends Exception {
		private static final long serialVersionUID = 1L;

		private final List<DetalheErroModeloOpl> detalhes;

		public ErroModeloException(String message) {
			super(message);
			detalhes = new ArrayList<DetalheErroModeloOpl>();
		}

		public ErroModeloException(String message, List<DetalheErroModeloOpl> detalhes) {
			this.detalhes = detalhes;
		}

		public List<DetalheErroModeloOpl> getDetalhes() {
			return detalhes;
		}
	}
}
