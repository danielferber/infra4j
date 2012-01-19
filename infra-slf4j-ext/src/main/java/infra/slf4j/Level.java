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
package infra.slf4j;

import org.slf4j.spi.LocationAwareLogger;

public enum Level {
	TRACE("TRACE", LocationAwareLogger.TRACE_INT),
	DEBUG("DEBUG", LocationAwareLogger.DEBUG_INT),
	INFO("INFO", LocationAwareLogger.INFO_INT),
	WARN("WARN", LocationAwareLogger.WARN_INT),
	ERROR("ERROR", LocationAwareLogger.ERROR_INT);

	private final String name;
	private final int level;

	@Override
	public String toString() {
		return this.name;
	}

	public int intValue() {
		return this.level;
	}

	private Level(String name, int level) {
		this.name = name;
		this.level = level;
	}
}
