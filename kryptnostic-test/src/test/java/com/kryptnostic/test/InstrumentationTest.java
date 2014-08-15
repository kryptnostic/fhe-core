package com.kryptnostic.test;

import org.junit.BeforeClass;
import org.junit.Test;

public class InstrumentationTest extends AbstractInstrumentedTest {
    
	@BeforeClass
	public static void setupContext() {
		registerTestConfigurationsForClass( InstrumentationTest.class , TestConfiguration.class );
	}
	
	@Test
	public void exercise() throws InterruptedException {
		SomeInstrumentedClass sic = getTestContext().getBean( SomeInstrumentedClass.class );
		for( int i = 0 ; i < 1000 ; ++i ) {
			try {
				sic.boom();
			} catch( IllegalStateException e ) {}
			sic.tick();
			sic.tock();
			sic.takeSomeTime();
		}
	}
}
