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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(RichRuntimeException.class);

	Map<String, Object> metaData = new TreeMap<String, Object>();

	Set<Object> operations = new LinkedHashSet<Object>();
	Set<Object> reasons = new LinkedHashSet<Object>();

	protected RichRuntimeException(Object operation, Object reason) {
		super();
		if (operation == null || reason == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(operation={}, reason={}) with null parameter.", operation, reason);
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	protected RichRuntimeException(Object operation, Object reason, Throwable cause) {
		super(cause);
		if (operation == null || reason == null || cause == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(operation={}, reason={}, cause={}) with null parameter.", new Object[] { operation, reason, cause });
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	protected RichRuntimeException(Object reason) {
		super();
		if (reason == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(operation={}, reason={}) with null parameter.", reason);
		}
		this.reasons.add(reason);
	}

	protected RichRuntimeException(Object reason, Throwable cause) {
		super(cause);
		if (reason == null || cause == null) {
			RichRuntimeException.logger.error("Called RichRuntimeException(reason={}, cause={}) with null parameter.", reason, cause);
		}
		this.reasons.add(reason);
	}

	/** Builder method to add meta data to the exception. */
	public RichRuntimeException data(String key, Object value) {
		if (key == null || value == null) {
			RichRuntimeException.logger.error("Called put(key={}, value={}) with null parameter.", key, value);
			return this;
		}
		metaData.put(key, value);
		return this;
	}

	/** Builder method to add a reason to the exception. */
	public RichRuntimeException reason(Object reason) {
		if (reason == null) {
			RichRuntimeException.logger.error("Called reason(reason={}) with null parameter.", reason);
			return this;
		}
		reasons.add(reason);
		return this;
	}

	/** Builder method to add a operation to the operation hierarchy of the exception. */
	public RichRuntimeException operation(Object operation) {
		if (operation == null) {
			RichRuntimeException.logger.error("Called operation(operation={}) with null parameter.", operation);
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

}
