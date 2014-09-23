package com.kryptnostic.multivariate;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Stopwatch;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.composition.RecursiveLinearizingComposer;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class PolynomialFunctionsTests {
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionsTests.class );

    @Test
    public void testLowerTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = PolynomialFunctions.lowerTruncatingIdentity(128, 64);
        BitVector input = BitUtils.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitUtils.subVector(input, 0, 1), output);
    }
    
    @Test
    public void testUpperTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = PolynomialFunctions.upperTruncatingIdentity(128, 64);
        BitVector input = BitUtils.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitUtils.subVector(input, 1, 2), output);
    }
    
    @Test 
    public void testBucketing() {
//        BasePolynomialFunction f = (BasePolynomialFunction)PolynomialFunctions.denseRandomMultivariateQuadratic( 128 , 128);
        BasePolynomialFunction f = (BasePolynomialFunction)PolynomialFunctions.randomFunction( 64 , 64 , 10 , 3 );
        BasePolynomialFunction inner = (BasePolynomialFunction) EnhancedBitMatrix.randomMatrix( 64 , 64 ) .multiply(  PolynomialFunctions.identity( 64 ) );
        
        
        RecursiveLinearizingComposer composer = new RecursiveLinearizingComposer( f );
        Stopwatch watch = Stopwatch.createStarted();
        SimplePolynomialFunction composed = composer.compose( inner );
        logger.info( "Compose time: {} ms" , watch.elapsed( TimeUnit.MILLISECONDS ) );
        
        BitVector input =  BitUtils.randomVector( inner.getInputLength() );
        BitVector expected = f.apply( inner.apply( input ) );
        BitVector actual = composed.apply( input );
        
        Assert.assertEquals( expected , actual );
    }
}
