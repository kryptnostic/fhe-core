package com.kryptnostic.test;

import java.util.Random;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;

public class SomeInstrumentedClass {
	private static final Random r = new Random();
	
	@Timed
	public void takeSomeTime() throws InterruptedException {
		Thread.sleep( r.nextInt( 50 ) );
	}
	
	@Gauge
	public void tick() throws InterruptedException {
		Thread.sleep( r.nextInt( 50 ) );
	}
	
	@Metered
	public void tock() throws InterruptedException {
		Thread.sleep( r.nextInt( 50 ) );
	}
	
	@ExceptionMetered 
	public void boom() {
		Preconditions.checkState( r.nextBoolean() );
	}
}
