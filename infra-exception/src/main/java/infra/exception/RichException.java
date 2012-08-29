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

/**
 * An all purpose exception that may carry information that further describes
 * the cause of failure and the operation that were running when the exception
 * was raised. This is a brief solution to identify all possible exceptions for
 * an operation, without need to create a full heavy weighted hierarchy of
 * exceptions. The reason might be implemented as an enumeration for each
 * operation.
 *
 * @author Daniel Felix Ferber
 */
public class RichException extends Exception {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = ExceptionService.logger;

	/**
	 * Additional structured data carried by the exception for further
	 * description of the error.
	 */
	protected final Map<String, Object> metaData = new TreeMap<String, Object>();

	/**
	 * Object(s) that describe the operation(s) within the context of the
	 * exception. There may be more than one operation if one operation depends
	 * on others.
	 */
	protected final Set<Object> operations = new LinkedHashSet<Object>();
	/**
	 * Object(s) that describe the reasons(s) of the exception. Reason is the
	 * cause of the failure.
	 */
	protected final Set<Object> reasons = new LinkedHashSet<Object>();

	protected final static Object nullToStr(Object o) {
		if (o == null) return "'null'";
		return o;
	}

	public RichException(Object operation, Object reason) {
		super();
		if (operation == null || reason == null) {
			RichException.logger.error("Called RichException(operation={}, reason={}) with a null parameter.", RichException.nullToStr(operation), RichException.nullToStr(reason));
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	public RichException(Object operation, Object reason, Throwable cause) {
		super(cause);
		if (operation == null || reason == null || cause == null) {
			RichException.logger.error("Called RichException(operation={}, reason={}, cause={}) with null parameter.", new Object[] { RichException.nullToStr(operation), RichException.nullToStr(reason), RichException.nullToStr(cause) });
		}
		this.reasons.add(reason);
		this.operations.add(operation);
	}

	public RichException(Object reason) {
		super();
		if (reason == null) {
			RichException.logger.error("Called RichException(reason='null') with null parameter.");
		}
		this.reasons.add(reason);
	}

	public RichException(Object reason, Throwable cause) {
		super(cause);
		if (reason == null || cause == null) {
			RichException.logger.error("Called RichException(reason={}, cause={}) with null parameter.", RichException.nullToStr(reason), RichException.nullToStr(cause));
		}
		this.reasons.add(reason);
	}

	/** Builder method to add meta data to the exception. */
	public RichException data(String key, Object value) {
		if (key == null || value == null) {
			RichException.logger.error("Called data(key={}, value={}) with null parameter.", RichException.nullToStr(key), RichException.nullToStr(value));
			return this;
		}
		metaData.put(key, value);
		return this;
	}

	/** Builder method to add meta data to the exception. */
//	public RichException data(Map<String, ? extends Object> moreMetaData) {
//		if (moreMetaData == null) {
//			RichException.logger.error("Called data(moreMetaData='null') with null parameter.");
//			return this;
//		}
//		metaData.putAll(moreMetaData);
//		return this;
//	}

	/** Builder method to add a reason to the exception. */
	public RichException reason(Object reason) {
		if (reason == null) {
			RichException.logger.error("Called reason(reason='null') with null parameter.");
			return this;
		}
		reasons.add(reason);
		return this;
	}

	/** Builder method to add a operation to the operation hierarchy of the exception. */
	public RichException operation(Object operation) {
		if (operation == null) {
			RichException.logger.error("Called operation(operation='null') with null parameter.");
			return this;
		}
		operations.add(operation);
		return this;
	}

	public final Object getData(String key) {
		return this.metaData.get(key);
	}

	public final Map<String, Object> getData() {
		return Collections.unmodifiableMap(metaData);
	}

	public final Set<Object> getReasons() {
		return Collections.unmodifiableSet(reasons);
	}

	public final Set<Object> getOperations() {
		return Collections.unmodifiableSet(operations);
	}

	public final boolean isOperation(Object operation) {
		return operations.contains(operation);
	}

	public final boolean hasReason(Object reason) {
		return reasons.contains(reason);
	}


}
