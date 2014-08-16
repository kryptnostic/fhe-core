package com.kryptnostic.test;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.kryptnostic.test.metrics.MetricsConfiguration;


@Configuration
public class AbstractInstrumentedTest {
	private static final ConcurrentMap<Class<?>,AnnotationConfigApplicationContext> contexts = Maps.newConcurrentMap();
	
	public AbstractInstrumentedTest( Class<?> ... annotatedClasses ) {
	    registerTestConfigurations( MetricsConfiguration.class );
	    if( annotatedClasses!=null && annotatedClasses.length > 0 ) {
	        registerTestConfigurations( annotatedClasses );
	    }
	    getTestContext().refresh();
	}
	
	private static AnnotationConfigApplicationContext getTestContext( Class<?> clazz ) {
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
	    context.register( annotatedClasses );
	}

}
