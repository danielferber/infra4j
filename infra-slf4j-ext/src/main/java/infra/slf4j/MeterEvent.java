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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Entity
public class MeterEvent implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final String UUID = "uuid";
	private static final String COUNTER = "c";
	private static final String THROWABLE = "tr";
	private static final String TIME = "t";
	private static final String CONTEXT = "ctx";
	private static final String THREAD = "th";
	private static final String DEPTH = "d";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** An arbitraty ID for the job. */
	@Column(nullable = false, length = 300)
    protected String name;

    /** How many times the job has executed. */
    protected long counter = 0;

	@Column(nullable = true, length = 50)
    protected String uuid;

    /** When the job was created/scheduled. */
	protected long createTime;

    /** When the job started execution. */
    protected long startTime = 0;

    /** When the job finished execution. */
    protected long stopTime = 0;

    /** An arbitrary short, human readable message to describe the task being measured. */
	@Column(nullable = true, length = 300)
    protected String message;

    /** An arbitrary exception to signal that the task failed. */
	@Column(nullable = true, length = 300)
	protected String exceptionClass;
	@Column(nullable = true, length = 300)
	protected String exceptionMessage;

    /** If the job completed successfully, as expected (true) or failed (false). */
	protected boolean success = false;

	protected long threadStartId;
	protected long threadStopId;
	@Column(nullable = true, length = 300)
	protected String threadStartName;
	@Column(nullable = true, length = 300)
	protected String threadStopName;
	protected int threadDepth;

	protected long depthCount;
	protected long depthContext;

	@ElementCollection
	@MapKeyColumn(name="chave", length=100)
	@Column(name="valor", length=100)
	@CollectionTable(name="contexto_meter", joinColumns=@JoinColumn(name="meterevent_ref"))
	protected Map<String, String> context;

    public String getName() { return name; }
    public String getMessage() { return message; }
    public long getCount() { return counter; }

    public long getCreateTime() { return createTime; }
    public long getStopTime() { return stopTime; }
    public long getStartTime() { return startTime; }

    public String getExceptionClass() { return exceptionClass; }
    public String getExceptionMessage() { return exceptionMessage; }

    public boolean isSuccess() { return success; }

	public static void readableString(MeterEvent meter, StringBuilder buffer) {
		if (meter.message != null) {
			buffer.append(meter.message);
		} else {
			buffer.append(meter.name);
		}
		if (meter.startTime > 0 && meter.stopTime > 0) {
			buffer.append(" ");
			long duration = meter.stopTime - meter.startTime;
			buffer.append(Parser.bestUnit(duration, Parser.TIME_UNITS, Parser.TIME_FACTORS));
		}
	}

	public static void writeToString(Parser p, MeterEvent e, StringBuilder buffer) {
		buffer.append(p.DATA_OPEN);

		/* name */
    	buffer.append(e.name);

    	/* message */
    	if (e.message != null) {
    		buffer.append(p.PROPERTY_DIV);
    		p.writeQuotedString(buffer,e.message);
	    	buffer.append(p.PROPERTY_SPACE);
    	}

    	/* counter */
    	if (e.counter > 0) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.COUNTER);
	   		buffer.append(p.PROPERTY_EQUALS);
	   		buffer.append(e.counter);
    	}

    	/* uuid */
    	if (e.uuid != null) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.UUID);
	   		buffer.append(p.PROPERTY_EQUALS);
	   		buffer.append(e.uuid);
    	}

    	if (e.depthContext != 0 || e.depthCount != 0) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.DEPTH);
	   		buffer.append(p.PROPERTY_EQUALS);
	   		buffer.append(e.depthContext);
	   		buffer.append(p.PROPERTY_DIV);
	   		buffer.append(e.depthCount);
    	}

    	if (e.threadStartId != 0) {
    		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.THREAD);
	   		buffer.append(p.PROPERTY_EQUALS);
	   		buffer.append(e.threadStartId);
	   		buffer.append(p.PROPERTY_DIV);
	   		p.writeQuotedString(buffer, e.threadStartName);
	   		if (e.threadStopId != 0) {
		   		buffer.append(p.PROPERTY_DIV);
		   		buffer.append(e.threadStopId);
		   		buffer.append(p.PROPERTY_DIV);
		   		p.writeQuotedString(buffer, e.threadStopName);
	   		}
    	}

   		/* exception */
   		if (e.exceptionClass != null) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.THROWABLE);
	   		buffer.append(p.PROPERTY_EQUALS);
   	   		buffer.append(e.exceptionClass);
   	   		if (e.exceptionMessage != null) {
   	   			buffer.append(p.PROPERTY_DIV);
   	   			p.writeQuotedString(buffer,e.exceptionMessage);
   	   		}
   		}

   		/* createTime, startTime, stopTime */
   		if (e.createTime > 0) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.TIME);
	   		buffer.append(p.PROPERTY_EQUALS);
	   		buffer.append(e.createTime);
	   		if (e.startTime != 0) {
	   	   		buffer.append(p.PROPERTY_DIV);
	   	   		buffer.append(e.startTime);
	   	   		if (e.stopTime != 0) {
	   	   	   		buffer.append(p.PROPERTY_DIV);
	   	   	   		buffer.append(e.stopTime);
	   	   		}
	   		}
   		}

   		/* context */
    	Map<String, String> globalContext = MeterFactory.getContext();
    	if ((e.context != null && ! e.context.isEmpty()) || (globalContext != null && ! globalContext.isEmpty())) {
	   		buffer.append(p.PROPERTY_SEPARATOR);
	   		buffer.append(p.PROPERTY_SPACE);
	   		buffer.append(MeterEvent.CONTEXT);
	   		buffer.append(p.PROPERTY_EQUALS);
    		buffer.append(p.MAP_OPEN);
    		boolean primeiro = true;
    		if (e.context != null && ! e.context.isEmpty()) {
	    		Iterator<Entry<String, String>> i = e.context.entrySet().iterator();
	    		while (i.hasNext()) {
	    			Entry<String, String> entry = i.next();
	        		if (primeiro) {
	        			primeiro = false;
	        		} else {
	        			buffer.append(p.MAP_SEPARATOR);
	        			buffer.append(p.MAP_SPACE);
	        		}
	        		buffer.append(entry.getKey());
	        		if (entry.getValue() != null) {
	        			buffer.append(p.MAP_EQUAL);
	        			p.writeQuotedString(buffer,entry.getValue());
	        		}
	    		}
    		}
    		if (globalContext != null && ! globalContext.isEmpty()) {
    			Iterator<Entry<String, String>> i = globalContext.entrySet().iterator();
	    		while (i.hasNext()) {
	    			Entry<String, String> entry = i.next();
	    			// não imprime valor pois contexto local tem preferência sobre o global.
	        		if (primeiro) {
	        			primeiro = false;
	        		} else {
	        			buffer.append(p.MAP_SEPARATOR);
	        			buffer.append(p.MAP_SPACE);
	        		}
	    			if (e.context.containsKey(entry.getKey())) continue;
	        		buffer.append(entry.getKey());
	        		if (entry.getValue() != null) {
	        			buffer.append(p.MAP_EQUAL);
	        			buffer.append(p.STRING_DELIM);
	        			buffer.append(entry.getValue().toString());
	        			buffer.append(p.STRING_DELIM);
	        		}
	    		}
    		}
    		buffer.append(p.MAP_CLOSE);
    	}
		buffer.append(p.DATA_CLOSE);
	}

	public static void readFromString(Parser p, MeterEvent e, String encodedData) throws IOException {
        /* Reseta todos os atributos. */
		e.name = null;
		e.message = null;
		e.counter = 0;
		e.createTime = e.startTime = e.stopTime = 0;
		e.exceptionClass = null;
		e.exceptionMessage = null;
		e.context = null;

		p.reset(encodedData);

		/* O nome é obrigatório. */
		e.name = p.readIdentifierString();
		if (p.readOptionalOperator(p.PROPERTY_DIV)) {
			/* A descrição é opcional. */
			e.message=p.readQuotedString();
		}


		if (! p.readOptionalOperator(p.PROPERTY_SEPARATOR)) return;

		String propertyName = p.readIdentifierString();
		while (propertyName != null) {
			p.readOperator(p.PROPERTY_EQUALS);
			if (MeterEvent.COUNTER.equals(propertyName)) {
				e.counter = p.readLong();
			} else if (MeterEvent.UUID.equals(propertyName)) {
					e.uuid = p.readUuid();
			} else if (MeterEvent.THREAD.equals(propertyName)) {
				e.threadStartId = p.readLong();
				p.readOperator(p.PROPERTY_DIV);
				e.threadStartName = p.readQuotedString();
				if (p.readOptionalOperator(p.PROPERTY_DIV)) {
					e.threadStopId = p.readLong();
					p.readOperator(p.PROPERTY_DIV);
					e.threadStopName = p.readQuotedString();
				}
			} else if (MeterEvent.DEPTH.equals(propertyName)) {
				e.depthContext = p.readLong();
				p.readOperator(p.PROPERTY_DIV);
				e.depthCount = p.readLong();
			} else if (MeterEvent.THROWABLE.equals(propertyName)) {
				e.exceptionClass=p.readIdentifierString();
				if (p.readOptionalOperator(p.PROPERTY_DIV)) {
					e.exceptionMessage=p.readQuotedString();
				}
			} else if (MeterEvent.TIME.equals(propertyName)) {
				e.createTime = p.readLong();
				if (p.readOptionalOperator(p.PROPERTY_DIV)) {
					e.startTime = p.readLong();
					if (p.readOptionalOperator(p.PROPERTY_DIV)) {
						e.stopTime = p.readLong();
					}
				}
			} else if (MeterEvent.CONTEXT.equals(propertyName)) {
				e.context = new HashMap<String, String>();
				p.readOperator('[');
				if (! p.readOptionalOperator(']')) {
					do {
						String key = p.readIdentifierString();
						String value = null;
						if (p.readOptionalOperator(p.MAP_EQUAL)) {
							value = p.readQuotedString();
						}
						e.context.put(key, value);
					} while (p.readOptionalOperator(p.MAP_SEPARATOR));
				}
				p.readOperator(p.MAP_CLOSE);
			} else {
				// property desconhecida, ignora
			}
			if (p.readOptionalOperator(p.PROPERTY_SEPARATOR)) {
				propertyName = p.readIdentifierString();
			} else {
				break;
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (counter ^ (counter >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MeterEvent other = (MeterEvent) obj;
		if (context == null) {
			if (other.context != null) {
				return false;
			}
		} else if (!context.equals(other.context)) {
			return false;
		}
		if (counter != other.counter) {
			return false;
		}
		if (createTime != other.createTime) {
			return false;
		}
		if (depthContext != other.depthContext) {
			return false;
		}
		if (depthCount != other.depthCount) {
			return false;
		}
		if (exceptionClass == null) {
			if (other.exceptionClass != null) {
				return false;
			}
		} else if (!exceptionClass.equals(other.exceptionClass)) {
			return false;
		}
		if (exceptionMessage == null) {
			if (other.exceptionMessage != null) {
				return false;
			}
		} else if (!exceptionMessage.equals(other.exceptionMessage)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (startTime != other.startTime) {
			return false;
		}
		if (stopTime != other.stopTime) {
			return false;
		}
		if (success != other.success) {
			return false;
		}
		if (threadDepth != other.threadDepth) {
			return false;
		}
		if (threadStartId != other.threadStartId) {
			return false;
		}
		if (threadStartName == null) {
			if (other.threadStartName != null) {
				return false;
			}
		} else if (!threadStartName.equals(other.threadStartName)) {
			return false;
		}
		if (threadStopId != other.threadStopId) {
			return false;
		}
		if (threadStopName == null) {
			if (other.threadStopName != null) {
				return false;
			}
		} else if (!threadStopName.equals(other.threadStopName)) {
			return false;
		}
		if (uuid == null) {
			if (other.uuid != null) {
				return false;
			}
		} else if (!uuid.equals(other.uuid)) {
			return false;
		}
		return true;
	}

	public long getExecutionTime() {
		if (startTime == 0) return 0;
		if (stopTime == 0) return System.nanoTime() - startTime;
		return stopTime - startTime;
	}

	public long getWaitingTime() {
		if (startTime == 0) return System.nanoTime() - createTime;
		return startTime - createTime;
	}

	@Override
	public String toString() {
		return this.uuid+":"+this.name+":"+this.counter;
	}

}
