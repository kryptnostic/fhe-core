package com.kryptnostic.multivariate;

import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.BinaryPolynomialFunction;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

import org.junit.Assert;

public class BinaryPolynomialFunctionTests {
    private static final Logger logger = LoggerFactory.getLogger( BinaryPolynomialFunctionTests.class );
    private static final Random r = new Random(0);
    
    @Test
    public void testConstructionAndEvaluation() {
        SimplePolynomialFunction f = PolynomialFunctions.randomFunction( 128 , 128 );
        SimplePolynomialFunction op = PolynomialFunctions.randomFunction( 256 , 128 );
        SimplePolynomialFunction g = PolynomialFunctions.randomFunction( 128 , 128 );
        BinaryPolynomialFunction h = new BinaryPolynomialFunction( 
                                            f ,
                                            op ,
                                            g );
                                             
        BitVector lhs = BitVectors.randomVector( 128 );
        BitVector rhs = BitVectors.randomVector( 128 );
        BitVector input = BitVectors.concatenate(lhs, rhs);
        
        BitVector lhsResult = f.apply( lhs );
        BitVector rhsResult = g.apply( rhs );
        BitVector opResult= op.apply( BitVectors.concatenate( lhsResult , rhsResult ) );
        BitVector binaryResult = h.apply( lhs, rhs );
        
        Assert.assertEquals( opResult , binaryResult );
    }
}
