package br.com.petrobras.gpo.infra.log4j;

import br.com.petrobras.gpo.infra.slf4j.Meter;
import br.com.petrobras.gpo.infra.slf4j.MeterFactory;

public class MeterRunnable implements Runnable {

	@Override
	public void run() {
		Meter m;
		
		m = MeterFactory.getMeter(this.getClass()).start();
		try {
			Thread.sleep(1100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		m.ok();

		m = MeterFactory.getMeter(this.getClass()).start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		m.ok();

		m = MeterFactory.getMeter(this.getClass()).start();
		try {
			Thread.sleep(60);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		m.ok();
	
		m = MeterFactory.getMeter(this.getClass()).start();
		try {
			Thread.sleep(60);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		m.fail(new IllegalArgumentException("bla"));
		
		m = MeterFactory.getMeter(this.getClass()).start();
		m.ok();

	}

}
