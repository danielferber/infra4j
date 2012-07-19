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
package infra.exception;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;

public class RichRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = ServicoExcecao.logger;

	Map<String, Object> metaData = new TreeMap<String, Object>();

	Set<Object> operations = new LinkedHashSet<Object>();
	Set<Object> reasons = new LinkedHashSet<Object>();

	private static Object nullToStr(Object o) {
		if (o == null) return "'null'";
		return o;
	}

	public RichRuntimeException(Object operation, Object reason) {
		super();
		if (operation == null || reason == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(operation={}, reason={}) with null parameter.", RichRuntimeException.nullToStr(operation), RichRuntimeException.nullToStr(reason));
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	public RichRuntimeException(Object operation, Object reason, Throwable cause) {
		super(cause);
		if (operation == null || reason == null || cause == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(operation={}, reason={}, cause={}) with null parameter.", new Object[] { RichRuntimeException.nullToStr(operation), RichRuntimeException.nullToStr(reason), RichRuntimeException.nullToStr(cause) });
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	public RichRuntimeException(Object reason) {
		super();
		if (reason == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(reason='null') with null parameter.");
		}
		this.reasons.add(reason);
	}

	public RichRuntimeException(Object reason, Throwable cause) {
		super(cause);
		if (reason == null || cause == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(reason={}, cause={}) with null parameter.", RichRuntimeException.nullToStr(reason), RichRuntimeException.nullToStr(cause));
		}
		this.reasons.add(reason);
	}


	/** Builder method to add meta data to the exception. */
	public RichRuntimeException data(String key, Object value) {
		if (key == null || value == null) {
			RichRuntimeException.logger.error("Called data(key={}, value={}) with null parameter.", RichRuntimeException.nullToStr(key), RichRuntimeException.nullToStr(value));
			return this;
		}
		metaData.put(key, value);
		return this;
	}

	/** Builder method to add a reason to the exception. */
	public RichRuntimeException reason(Object reason) {
		if (reason == null) {
			RichRuntimeException.logger.error("Called reason(reason='null') with null parameter.");
			return this;
		}
		reasons.add(reason);
		return this;
	}

	/** Builder method to add a operation to the operation hierarchy of the exception. */
	public RichRuntimeException operation(Object operation) {
		if (operation == null) {
			RichRuntimeException.logger.error("Called operation(operation='null') with null parameter.");
			return this;
		}
		operations.add(operation);
		return this;
	}

	public Object getData(String key) {
		return this.metaData.get(key);
	}

	public Set<Object> getReasons() {
		return Collections.unmodifiableSet(reasons);
	}

	public Set<Object> getOperations() {
		return Collections.unmodifiableSet(operations);
	}

	public boolean isOperation(Object operation) {
		return operations.contains(operation);
	}

	public boolean hasReason(Object reason) {
		return reasons.contains(reason);
	}

	public static RichRuntimeException enrich(Throwable e, Object operation) {
		return RichRuntimeException.enrichImpl(e).operation(operation);
	}

	public static RichRuntimeException enrich(Throwable e, Object operation, Object reason) {
		return RichRuntimeException.enrichImpl(e).operation(operation).reason(reason);
	}

	public static RichRuntimeException enrich(Throwable e) {
		return RichRuntimeException.enrichImpl(e);
	}

	public static RichRuntimeException enrichImpl(Throwable e) {
		if (e instanceof RichRuntimeException) { // ( ou UnhandledRuntimeException)
			return (RichRuntimeException) e;
		}
		RichRuntimeException newE = new UnhandledRuntimeException(e);
		StackTraceElement[] st = newE.getStackTrace();
		st = Arrays.copyOfRange(st, 2, st.length);
		newE.setStackTrace(st);
		return newE;
	}
}
