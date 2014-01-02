package com.krytpnostic.multivariate.test;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.multivariate.Monomial;
import com.kryptnostic.multivariate.MultivariateUtils;
import com.kryptnostic.multivariate.PolynomialFunction;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;


public class PolynomialFunctionTests {
    private static final Random r = new Random( System.currentTimeMillis() );
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionTests.class );
    @Test
    public void builderTest() {
        PolynomialFunction.Builder builder = PolynomialFunction.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunction f = builder.build();
        BitVector result = f.evalute( MultivariateUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
    @Test
    public void evaluationTest() {
        PolynomialFunction.Builder builder = PolynomialFunction.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunction f = builder.build();
        BitVector result = f.evalute( MultivariateUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
}
