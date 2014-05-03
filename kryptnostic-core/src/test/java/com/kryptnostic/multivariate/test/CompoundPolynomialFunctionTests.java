package com.kryptnostic.multivariate.test;

import org.junit.Test;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;

import cern.colt.bitvector.BitVector;
import org.junit.Assert;

public class CompoundPolynomialFunctionTests {
    @Test
    public void testCreateAndEvaluate() {
        PolynomialFunction f = PolynomialFunctions.randomFunction( 128 , 512 );
        PolynomialFunction g = PolynomialFunctions.randomFunction( 512 , 256 );
        PolynomialFunction h = PolynomialFunctions.randomFunction( 256 , 64 );
        CompoundPolynomialFunction cpf = CompoundPolynomialFunctionGF2.fromFunctions( f , g , h );
        
        Assert.assertEquals( 128 , cpf.getInputLength() );
        Assert.assertEquals( 64 , cpf.getOutputLength() );
        
        BitVector v = BitUtils.randomVector( cpf.getInputLength() );
        
        Assert.assertEquals( h.apply( g.apply( f.apply( v ) ) ) ,  cpf.apply( v ) );
    }
    
    @Test
    public void testComposeAndEvaluate() {
        PolynomialFunction f = PolynomialFunctions.randomFunction( 128 , 512 );
        PolynomialFunction g = PolynomialFunctions.randomFunction( 512 , 256 );
        PolynomialFunction h = PolynomialFunctions.randomFunction( 256 , 64 );
        
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2()
                                                .compose( h )
                                                .compose( g )
                                                .compose( f );
        
        Assert.assertEquals( 128 , cpf.getInputLength() );
        Assert.assertEquals( 64 , cpf.getOutputLength() );
        
        BitVector v = BitUtils.randomVector( cpf.getInputLength() );
        
        Assert.assertEquals( h.apply( g.apply( f.apply( v ) ) ) ,  cpf.apply( v ) );
    }
}
