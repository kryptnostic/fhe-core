package com.kryptnostic.test;
import org.junit.BeforeClass;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.kryptnostic.test.metrics.MetricsConfiguration;


public abstract class AbstractInstrumentedTest {
	private static final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@BeforeClass 
	public static void registerMetrics() {
		context.register( MetricsConfiguration.class );
	}
	
	protected static AnnotationConfigApplicationContext getTestContext() {
		return context;
	}

}
