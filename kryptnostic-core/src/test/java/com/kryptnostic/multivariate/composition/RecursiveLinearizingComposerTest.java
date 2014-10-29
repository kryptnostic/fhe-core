package com.kryptnostic.multivariate.composition;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Stopwatch;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.PolynomialFunctionsTests;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.BasePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class RecursiveLinearizingComposerTest {
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionsTests.class );
    
    @Test 
    public void testRecurisveLinearizingComposer() {
        logger.debug("Starting recursive linearing composer tests.");
        BasePolynomialFunction f = (BasePolynomialFunction)SimplePolynomialFunctions.denseRandomMultivariateQuadratic( 128 , 128);
        BasePolynomialFunction inner = (BasePolynomialFunction) EnhancedBitMatrix.randomMatrix( 128 , 256 ) .multiply(  SimplePolynomialFunctions.identity( 256 ) );
        
        
        RecursiveLinearizingComposer composer = new RecursiveLinearizingComposer( f );
        Stopwatch watch = Stopwatch.createStarted();
        SimplePolynomialFunction composed = composer.compose( inner );
        logger.info( "Compose time: {} ms" , watch.elapsed( TimeUnit.MILLISECONDS ) );
        
        BitVector input =  BitVectors.randomVector( inner.getInputLength() );
        BitVector expected = f.apply( inner.apply( input ) );
        BitVector actual = composed.apply( input );
        
        Assert.assertEquals( expected , actual );
    }
    
    @Before
    public void testStart() {
        logger.debug( "Starting test." );
    }
    
    @After
    public void testStop() {
        logger.debug( "Finishing test." );
    }
}
