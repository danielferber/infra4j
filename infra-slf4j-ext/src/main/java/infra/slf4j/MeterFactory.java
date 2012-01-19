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

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterFactory {
	private static final Map<String, String> context = new TreeMap<String, String>();
	private static final UUID uuid = UUID.randomUUID();
//	private static WatcherEvent watcherEvent = new WatcherEvent();

	public static Meter getMeter(String name) {
		return new Meter(LoggerFactory.getLogger(name), name);
	}

	public static Meter getMeter(Class<?> clazz) {
		return new Meter(LoggerFactory.getLogger(clazz), clazz.getName());
	}

	public static Meter getMeter(Class<?> clazz, String name) {
		String instanceName = clazz.getName()+"."+name;
		return new Meter(LoggerFactory.getLogger(instanceName), instanceName);
	}

	public static Meter getMeter(Logger logger, String name) {
		String instanceName = logger.getName()+"."+name;
		return new Meter(LoggerFactory.getLogger(instanceName), instanceName);
	}

	public static Meter getMeter(MeterEvent taskGeral, String name) {
		String instanceName = taskGeral.getName()+"."+name;
		return new Meter(LoggerFactory.getLogger(instanceName), instanceName);
	}

	public static void put(String name) {
    	MeterFactory.context.put(name, null);
    }

    public static void put(String name, String value) {
    	MeterFactory.context.put(name, value);
    }

    public static void remove(String name) {
    	MeterFactory.context.remove(name);
    }

    static Map<String, String> getContext() {
		return MeterFactory.context;
	}

    public static String getuuid() {
		return MeterFactory.uuid.toString().replace('-', '.');
	}
//    public static WatcherEvent getWatcherEvent() {
//		return MeterFactory.watcherEvent;
//	}
}
