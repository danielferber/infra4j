package infra.ilog.opl;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


public class OplSettingsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum Reason {
		TMP_DIR;
	}

	public final Reason reason;
	public final Map<String, Object> context = new TreeMap<String, Object>();

	public OplSettingsException(Reason reason, IOException e) {
		super(reason.name(), e);
		this.reason = reason;
	}

//	public Object get(String key) {
//		return context.get(key);
//	}
//
//	public OplSettingsException put(String key, Object value) {
//		context.put(key, value);
//		return this;
//	}
//
//	public Map<String, Object> getContext() {
//		return Collections.unmodifiableMap(context);
//	}
}
