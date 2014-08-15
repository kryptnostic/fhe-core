package com.kryptnostic.test;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.kryptnostic.test.metrics.MetricsConfiguration;


public abstract class AbstractInstrumentedTest {
	private static final ConcurrentMap<Class<?>,AnnotationConfigApplicationContext> contexts = Maps.newConcurrentMap();
	
	@Before
    public void refreshContext() {
	    AnnotationConfigApplicationContext context = getTestContext();
        if( !context.isActive() ) {
            context.register( MetricsConfiguration.class );
            context.refresh();
        }
    }
	
	protected static void registerTestConfigurationsForClass( Class<?> clazz , Class<?> ... annotatedClasses ) {
	    AnnotationConfigApplicationContext context = getTestContext( clazz );
	    context.register( annotatedClasses );
	}
	
	protected static AnnotationConfigApplicationContext getTestContext( Class<?> clazz ) {
	    Preconditions.checkNotNull( clazz , "Cannot retrieve context for null class." );
	    
	    AnnotationConfigApplicationContext context = contexts.get( clazz);
	    if( context == null ) {
	        context = new AnnotationConfigApplicationContext();
	        contexts.put( clazz , context  );
	    }
	    
	    return context;
	}
	
	protected AnnotationConfigApplicationContext getTestContext() {
	    return getTestContext( getClass() );
	}
	
	protected void registerTestConfigurations( Class<?> ... annotatedClasses ) {
	    AnnotationConfigApplicationContext context = getTestContext();
	    Preconditions.checkState( !context.isActive() , "Context cannot be active." );
	}

}
