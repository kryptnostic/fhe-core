package com.kryptnostic.test;

import org.springframework.context.annotation.Bean;

import com.codahale.metrics.annotation.Timed;

public interface InstrumentationTestsApi {

    public abstract void exercise() throws InterruptedException;

    public abstract SomeInstrumentedClass sic();

}