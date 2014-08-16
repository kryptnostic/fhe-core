package com.kryptnostic.test;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.annotation.Timed;

@Configuration
public class InstrumentationTests {
    @Inject
    private SomeInstrumentedClass sic;
    
    @Timed
    public void exercise() throws InterruptedException {
        try {
            sic.boom();
        } catch( IllegalStateException e ) {}
        sic.tick();
        sic.tock();
        sic.takeSomeTime();
    }
    
    @Bean
    public SomeInstrumentedClass sic() {
        return new SomeInstrumentedClass();
    }
}
