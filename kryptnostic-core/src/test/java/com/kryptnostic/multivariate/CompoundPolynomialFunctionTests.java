package com.kryptnostic.multivariate;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class CompoundPolynomialFunctionTests {
    @Test
    public void testCreateAndEvaluate() {
        SimplePolynomialFunction f = SimplePolynomialFunctions.randomFunction( 128 , 512 );
        SimplePolynomialFunction g = SimplePolynomialFunctions.randomFunction( 512 , 256 );
        SimplePolynomialFunction h = SimplePolynomialFunctions.randomFunction( 256 , 64 );
        CompoundPolynomialFunction cpf = CompoundPolynomialFunctions.fromFunctions( f , g , h );
        
        Assert.assertEquals( 128 , cpf.getInputLength() );
        Assert.assertEquals( 64 , cpf.getOutputLength() );
        
        BitVector v = BitVectors.randomVector( cpf.getInputLength() );
        
        Assert.assertEquals( h.apply( g.apply( f.apply( v ) ) ) ,  cpf.apply( v ) );
    }
    
    @Test
    public void testComposeAndEvaluate() {
        SimplePolynomialFunction f = SimplePolynomialFunctions.randomFunction( 128 , 512 );
        SimplePolynomialFunction g = SimplePolynomialFunctions.randomFunction( 512 , 256 );
        SimplePolynomialFunction h = SimplePolynomialFunctions.randomFunction( 256 , 64 );
        
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2()
                                                .compose( h )
                                                .compose( g )
                                                .compose( f );
        
        Assert.assertEquals( 128 , cpf.getInputLength() );
        Assert.assertEquals( 64 , cpf.getOutputLength() );
        
        BitVector v = BitVectors.randomVector( cpf.getInputLength() );
        
        Assert.assertEquals( h.apply( g.apply( f.apply( v ) ) ) ,  cpf.apply( v ) );
    }
}
