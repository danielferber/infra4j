package br.pro.danielferber.lang;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongModuloCounter {
	private final AtomicLong number;
	private long min = 0;
	private long max = Long.MAX_VALUE;
	
	public AtomicLongModuloCounter() {
		this.number = new AtomicLong(min);
	}

	public AtomicLongModuloCounter(long max) {
		this.number = new AtomicLong(min);
		this.max = max;
	}

	public AtomicLongModuloCounter(long min, long max) {
		this.number = new AtomicLong(min);
		this.min = min;
		this.max = max;
	}

	public long next() {
		number.compareAndSet(max, 0);
		return number.getAndIncrement();
	}
}
