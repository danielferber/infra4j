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

import infra.exception.assertions.datastate.NullArgumentException;

public class OplModelParseError {
	public static enum Level { WARNING, ERROR, FATAL }
	private final Level level;
	private final String source;
	private final int startLine;
	private final int startColumn;
	private final int endLine;
	private final int endColumn;
	private final String catalogId;
	private final String message;

	public OplModelParseError(Level level, String source, int startLine, int startColumn, int endLine, int endColumn, String catalogId, String message) {
		super();
		assert NullArgumentException.apply(level, source, catalogId, message);
		this.level = level;
		this.source = source;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		this.catalogId = catalogId;
		this.message = message;
	}

	public Level getLevel() { return level; }
	public String getSource() { return source; }
	public int getStartLine() { return startLine; }
	public int getStartColumn() { return startColumn; }
	public int getEndLine() { return endLine; }
	public int getEndColumn() { return endColumn; }
	public String getCatalogId() { return catalogId; }
	public String getMessage() { return message; }

	@Override
	public String toString() {
		return String.format("[%s] %s (%d, %d - %d, %d): %s (%s)", level.toString(), source, startLine, startColumn, endLine, endColumn, message, catalogId);
	}
}
