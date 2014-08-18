package com.kryptnostic.test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.common.base.Preconditions;
import com.kryptnostic.test.metrics.MetricsConfiguration;


public class AbstractInstrumentedTest {
	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
	
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

}
