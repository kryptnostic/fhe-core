package com.kryptnostic.linear.tests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class MatrixTests {
    private static final Logger logger = LoggerFactory.getLogger( MatrixTests.class );
    @Test
    public void inverseTest() {
        EnhancedBitMatrix m = EnhancedBitMatrix.randomSqrMatrix( 10 );
        try {
            m.inverse();
        } catch( SingularMatrixException e ) {
            logger.error( "Singlur matrices cannot be inverted." , e );
        }
        
    }
    
    
}
