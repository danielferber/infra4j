package infra.ilog.opl;

import infra.exception.assertions.datastate.IllegalArgumentException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class OplModelException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final List<OplModelParseError> parseErrors = new ArrayList<OplModelParseError>();

	public OplModelException(List<OplModelParseError> newParseErrors) {
		super();
		assert IllegalArgumentException.apply(newParseErrors != null);
		assert IllegalArgumentException.apply(newParseErrors.size() > 0);
		parseErrors.addAll(newParseErrors);
	}

	public List<OplModelParseError> getParseErrors() {
		return Collections.unmodifiableList(parseErrors);
	}
}
