package com.kryptnostic.test;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.codahale.metrics.ScheduledReporter;
import com.google.common.base.Preconditions;
import com.kryptnostic.test.metrics.MetricsConfiguration;


public class AbstractInstrumentedTest {
	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
	private static final Logger logger = LoggerFactory.getLogger( AbstractInstrumentedTest.class );
	public AbstractInstrumentedTest( Class<?> ... annotatedClasses ) {
	    registerTestConfigurations( MetricsConfiguration.class );
	    if( annotatedClasses!=null && annotatedClasses.length > 0 ) {
	        registerTestConfigurations( annotatedClasses );
	    }
	    getTestContext().refresh();
	}
	
	protected AnnotationConfigApplicationContext getTestContext() {
	    return context;
	}
	
	protected void registerTestConfigurations( Class<?> ... annotatedClasses ) {
	    AnnotationConfigApplicationContext context = getTestContext();
	    Preconditions.checkState( !context.isActive() , "Context cannot be active." );
	    context.register( annotatedClasses );
	}

	@After
	public void report() {
	    ScheduledReporter reporter = getTestContext().getBean( ScheduledReporter.class );
	    if( reporter != null ) {
	        reporter.report();
	    } else {
	        logger.warn("Unable to flush reporter. Bean couldn't be found");
	    }
	}
}
