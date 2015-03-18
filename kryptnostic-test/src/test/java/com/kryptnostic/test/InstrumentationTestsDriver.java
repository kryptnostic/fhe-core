package com.kryptnostic.test;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstrumentationTestsDriver extends AbstractInstrumentedTest {
    private static Logger logger = LoggerFactory.getLogger( InstrumentationTestsDriver.class );
    
    public InstrumentationTestsDriver() {
        super( InstrumentationTests.class );
    }
    
	@Test
	public void exerciseDriver() throws InterruptedException {
	    logger.info("Defined beans: " , Arrays.asList( getTestContext().getBeanDefinitionNames() ) );
	    InstrumentationTestsApi tests = getTestContext().getBean( InstrumentationTestsApi.class );
	    Assert.assertTrue( getTestContext().isActive() );
	    Assert.assertNotNull( tests );
	    for( int i = 0 ; i < 1000 ; ++i ) {
	        tests.exercise();
	    }
	}
	
}
