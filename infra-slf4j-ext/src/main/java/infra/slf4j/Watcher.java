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

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Watcher extends WatcherEvent {
	private Timer timer;
	private Logger loggerWatcher;
	private ProfileTask profileTask;

	public static final Marker WATCHER_MARKER = MarkerFactory.getMarker("WATCHER");

	/** Configuração padrão do parser usado para ler novamente a mensagem do log. */
	private static final Parser parser = new Parser();

	public class ProfileTask extends TimerTask {
		@Override
		public void run() {
			update();

			Watcher.this.counter++;

			if (loggerWatcher.isDebugEnabled()) {
				StringBuilder buffer = new StringBuilder();
				WatcherEvent.readableString(Watcher.this, buffer);
				loggerWatcher.debug("WATCH: " + buffer.toString());
			}
			if (loggerWatcher.isTraceEnabled()) {
				StringBuilder buffer = new StringBuilder();
				WatcherEvent.writeToString(Watcher.parser, Watcher.this, buffer);
				loggerWatcher.trace(Watcher.WATCHER_MARKER, "WATCH: " + buffer.toString());
			}
		}
	}

	public Watcher(String name) {
		super();
		this.name = name;
		this.uuid = MeterFactory.getuuid();
		Logger logger = LoggerFactory.getLogger(name);
		this.loggerWatcher = LoggerFactory.getLogger(logger, "watcher");
	}

	public void start() {
		timer = new Timer(true);
		profileTask = new ProfileTask();
		timer.scheduleAtFixedRate(profileTask, 0, 250);
	}

	public void stop() {
		profileTask.cancel();
		timer.cancel();
		timer = null;
	}
}
