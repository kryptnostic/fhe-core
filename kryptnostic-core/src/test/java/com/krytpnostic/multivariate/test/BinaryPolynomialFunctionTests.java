package com.krytpnostic.multivariate.test;

import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.BinaryPolynomialFunction;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class BinaryPolynomialFunctionTests {
    private static final Logger logger = LoggerFactory.getLogger( BinaryPolynomialFunctionTests.class );
    private static final Random r = new Random(0);
    
    @Test
    public void testConstructionAndEvaluation() {
        SimplePolynomialFunction f = PolynomialFunctionGF2.randomFunction( 128 , 128 );
        SimplePolynomialFunction op = PolynomialFunctionGF2.randomFunction( 256 , 128 );
        SimplePolynomialFunction g = PolynomialFunctionGF2.randomFunction( 128 , 128 );
        BinaryPolynomialFunction h = new BinaryPolynomialFunction( 
                                            f ,
                                            op ,
                                            g );
                                             
        BitVector lhs = BitUtils.randomVector( 128 );
        BitVector rhs = BitUtils.randomVector( 128 );
        BitVector input = FunctionUtils.concatenate(lhs, rhs);
        
        BitVector lhsResult = f.apply( lhs );
        BitVector rhsResult = g.apply( rhs );
        BitVector opResult= op.apply( FunctionUtils.concatenate( lhsResult , rhsResult ) );
        BitVector binaryResult = h.apply( lhs, rhs );
        
        Assert.assertEquals( opResult , binaryResult );
    }
}
