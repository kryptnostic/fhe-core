package com.kryptnostic.multivariate;

import org.junit.Assert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cern.colt.bitvector.BitVector;

import com.codahale.metrics.annotation.Timed;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

@Configuration
public class PartialEvaluationTests {
    private static final int INPUT_LENGTH = 256;
    private static final int OUTPUT_LENGTH = 256;
    
    @Timed
    public void testMonomialPartialEval() {
        Monomial m = new Monomial( INPUT_LENGTH ).chainSet( 1 ).chainSet(2).chainSet( 3 ).chainSet( 63 ).chainSet( 64 ).chainSet( 127);
        BitVector input = new BitVector( 64 );
        
        input.set( 1 );
        input.set( 2 );
        input.set( 3 );
        input.set( 63 );
        
        Monomial resolved = m.partialEval( input );
        Monomial expectedResolution = new Monomial( INPUT_LENGTH - 64).chainSet( 0 ).chainSet( 63 );
        Assert.assertEquals( expectedResolution, resolved);
        
        input.clear( 63 );
        Assert.assertNull( m.partialEval( input ) );
    }
    
    @Timed
    public void testResolve() { 
        SimplePolynomialFunction f = testFunction();
        BitVector input = BitVectors.randomVector( INPUT_LENGTH );
        BitVector resolveInput = input.partFromTo( 0 , (INPUT_LENGTH >>> 1) - 1 );
        BitVector resolveUpper = input.partFromTo( INPUT_LENGTH >>> 1 , INPUT_LENGTH - 1 );
        
        SimplePolynomialFunction g = f.resolve( resolveInput );
        Assert.assertEquals( f.apply( input ) , g.apply( resolveUpper ) );
    }
    
    @Bean
    public SimplePolynomialFunction testFunction() {
        return SimplePolynomialFunctions.denseRandomMultivariateQuadratic( INPUT_LENGTH, OUTPUT_LENGTH );
    }
}
