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

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Meter extends MeterEvent {
	private static final long serialVersionUID = 1L;

	/** Por enquanto, faz log direito no SLF4J. */
	private final Logger logger;

	private final WatcherEvent watcherEvent;
	/** Configuração padrão do parser usado para ler novamente a mensagem do log. */
	private static final Parser parser = new Parser();

	/** How many times each job has been executed. */
	private static final ConcurrentMap<String, AtomicLong> meterCounter = new ConcurrentHashMap<String, AtomicLong>();

	//    private static AtomicLong depthContextCounter = new AtomicLong(0);
	//    private static ThreadLocal<Long> threadDepthContext = new ThreadLocal<Long>() {
	//    	protected Long  initialValue() {
	//    		return depthContextCounter.incrementAndGet();
	//    	}
	//    };

	public static final Marker START_MARKER = MarkerFactory.getMarker("METER_START");
	public static final Marker START_WATCH_MARKER = MarkerFactory.getMarker("WATCHER_START");
	public static final Marker OK_MARKER = MarkerFactory.getMarker("METER_OK");
	public static final Marker OK_WATCH_MARKER = MarkerFactory.getMarker("WATCHER_OK");
	public static final Marker FAIL_MARKER = MarkerFactory.getMarker("METER_FAIL");
	public static final Marker FAIL_WATCH_MARKER = MarkerFactory.getMarker("WATCHER_FAIL");
	public static final Marker FINALIZED_MARKER = MarkerFactory.getMarker("METER_FINALIZED");

	public Meter(Logger logger, String name) {
		this.name = name;
		this.logger = logger;
		this.uuid = MeterFactory.getuuid();
		Meter.meterCounter.putIfAbsent(name, new AtomicLong(0));
		this.counter = Meter.meterCounter.get(name).incrementAndGet();
		this.watcherEvent = new WatcherEvent();
		this.watcherEvent.name = this.name;
		this.watcherEvent.counter = this.counter;
		createTime = System.nanoTime();
	}

	public Logger getLogger() { return logger; }

	public Meter setMessage(String message, Object... args) {
		try {
			this.message = String.format(message, args);
		} catch (IllegalFormatException e) {
			logger.warn("Meter.setMessage(...)", e);
		}
		return this;
	}

	public Meter put(String name) {
		if (context == null) this.context = new HashMap<String, String>();
		context.put(name, null);
		return this;
	}

	public Meter put(String name, String value) {
		if (context == null) this.context = new HashMap<String, String>();
		context.put(name, value);
		return this;
	}

	public Meter remove(String name) {
		if (context == null) return this;
		context.remove(name);
		return this;
	}

	//    public Meter contextualize(String name) {
	//    	context.put(name, null);
	//    	return this;
	//    }
	//
	//    public Meter contextualize(String name, Object value) {
	//    	context.put(name, value);
	//    	return this;
	//    }

	// ========================================================================

	public Meter start() {
		return startImpl(null, null);
	}

	/*
	 * TODO este método é necessário? poderia ser put(name).start();
	 */
	public MeterEvent start(String name) {
		return startImpl(name, null);
	}

	/*
	 * TODO este método é necessário? poderia ser put(name, value).start();
	 */
	public MeterEvent start(String name, String value) {
		return startImpl(name, value);
	}

	protected Meter startImpl(String name, String value) {
		assert createTime != 0;
		try {
			if (startTime != 0) logger.error("Inconsistent Meter start()", new Exception("Meter.start(...): startTime != 0"));
			if (name != null) put(name, value);

			Thread currentThread = Thread.currentThread();
			this.threadStartId = currentThread.getId();
			this.threadStartName = currentThread.getName();

			if (logger.isDebugEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.readableString(this, buffer);
				logger.debug("START: " + buffer.toString());
			}
			if (logger.isTraceEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.writeToString(Meter.parser, this, buffer);
				logger.trace(Meter.START_MARKER,"START: " + buffer.toString());

				watcherEvent.update();
				buffer = new StringBuilder();
				WatcherEvent.writeToString(Meter.parser, this.watcherEvent, buffer);
				logger.trace(Meter.START_WATCH_MARKER, "WATCH: " + buffer.toString());
			}
			startTime = System.nanoTime();
		} catch (Throwable t) {
			logger.error("Excetion thrown in Meter", t);
		}
		return this;
	}

	// ========================================================================

	public MeterEvent ok() {
		return this.ok(null, null);
	}

	public MeterEvent ok(String name) {
		return this.ok(name, null);
	}

	public MeterEvent ok(String name, String value) {
		return okImpl(name, value);
	}

	protected MeterEvent okImpl(String name, String value) {
		assert createTime != 0;
		try {
			if (stopTime != 0) logger.error("Inconsistent Meter ok()", new Exception("Meter.stop(...): stopTime != 0"));
			stopTime = System.nanoTime();
			if (startTime == 0) logger.error("Inconsistent Meter ok()", new Exception("Meter.stop(...): startTime == 0"));
			if (name != null) put(name, value);
			success = true;

			Thread currentThread = Thread.currentThread();
			this.threadStopId = currentThread.getId();
			this.threadStopName = currentThread.getName();

			if (logger.isInfoEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.readableString(this, buffer);
				logger.info("OK: " + buffer.toString());
			}

			if (logger.isTraceEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.writeToString(Meter.parser, this, buffer);
				logger.trace(Meter.OK_MARKER, "OK: " + buffer.toString());

				watcherEvent.update();
				buffer = new StringBuilder();
				WatcherEvent.writeToString(Meter.parser, this.watcherEvent, buffer);
				logger.trace(Meter.OK_WATCH_MARKER, "WATCH: " + buffer.toString());
			}
		} catch (Throwable t) {
			logger.error("Excetion thrown in Meter", t);
		}
		return this;
	}

	// ========================================================================

	//    public Meter fail() {
	//    	return this.fail(null, null, null);
	//    }
	//
	//    public Meter fail(String name) {
	//    	return this.fail(null, name, null);
	//    }
	//
	//    public Meter fail(String name, Object value) {
	//    	return this.fail(null, name, value);
	//   }

	public MeterEvent fail(Throwable throwable) {
		return this.fail(throwable, null, null);
	}

	public MeterEvent fail(Throwable throwable, String name) {
		return this.fail(throwable, name, null);
	}

	public MeterEvent fail(Throwable throwable, String name, String value) {
		return failImpl(throwable, name, value);
	}

	protected MeterEvent failImpl(Throwable throwable, String name, String value) {
		try {
			assert createTime != 0;
			if (stopTime != 0) logger.error("Inconsistent Meter", new Exception("Meter.stop(...): stopTime != 0"));
			stopTime = System.nanoTime();
			if (startTime == 0) logger.error("Inconsistent Meter", new Exception("Meter.stop(...): startTime == 0"));
			if (name != null) context.put(name, value);
			if (throwable != null) {
				exceptionClass = throwable.getClass().getName();
				exceptionMessage = throwable.getLocalizedMessage();
			}
			success = false;

			Thread currentThread = Thread.currentThread();
			this.threadStopId = currentThread.getId();
			this.threadStopName = currentThread.getName();

			if (logger.isWarnEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.readableString(this, buffer);
				logger.warn("FAIL: " + buffer.toString());
			}
			if (logger.isTraceEnabled()) {
				StringBuilder buffer = new StringBuilder();
				MeterEvent.writeToString(Meter.parser, this, buffer);
				logger.trace(Meter.FAIL_MARKER, "FAIL: " + buffer.toString());

				watcherEvent.update();
				buffer = new StringBuilder();
				WatcherEvent.writeToString(Meter.parser, this.watcherEvent, buffer);
				logger.trace(Meter.FAIL_WATCH_MARKER, "WATCH: " + buffer.toString());
			}
		} catch (Throwable t) {
			logger.error("Excetion thrown in Meter", t);
		}
		return this;
	}

	@Override
	protected void finalize() throws Throwable {
		if (stopTime == 0) {
			failImpl(null, null, null);
		}
		super.finalize();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}
}
