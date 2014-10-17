package com.kryptnostic.testing;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TravisCI {
    private static final long ALLOWED_BUILD_TIME_MILLIS = TimeUnit.MINUTES.toMillis( 16 );
    private static final long PING_TIME_MILLIS = TimeUnit.MINUTES.toMillis( 1 );
    private static final String STATUS = "I haven't frozen, please don't stop me!";
    private static final Logger logger = LoggerFactory.getLogger( TravisCI.class );
    
    @Test
    public void pingTravisCI() throws InterruptedException {
        long start = System.currentTimeMillis();
        while( (System.currentTimeMillis() - start) < ALLOWED_BUILD_TIME_MILLIS ) {
            
            logger.error( STATUS);
            System.out.println( STATUS );
            System.err.println( STATUS );
            
            Thread.sleep( PING_TIME_MILLIS );
        }
    }
}
