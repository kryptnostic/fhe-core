package com.kryptnostic.test;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseKryptnosticTest {
    private final Logger        logger   = LoggerFactory.getLogger( getClass() );
    private final AtomicInteger refCount = new AtomicInteger();

    @Before
    public void logTestStarted() {
        if ( refCount.incrementAndGet() == 0 ) {
            logger.error( "Starting tests in: {}", getClass().getCanonicalName() );
        }
    }

    @After
    public void logTestFinished() {
        if ( refCount.getAndDecrement() == 1 ) {
            logger.debug( "Finished tests in: {}", getClass().getCanonicalName() );
        }
    }
}
