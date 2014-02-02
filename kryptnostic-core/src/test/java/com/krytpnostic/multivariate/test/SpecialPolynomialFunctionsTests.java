package com.krytpnostic.multivariate.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

import cern.colt.bitvector.BitVector;

/**
 * Tests for special polynomial function provided as building blocks for more complex functions.
 * @author Matthew Tamayo-Rios
 */
public class SpecialPolynomialFunctionsTests {
    private static final Random r = new Random( System.currentTimeMillis() );
    private static final PrivateKey privKey = new PrivateKey( 128 , 64 );
    private static final PublicKey pubKey = new PublicKey( privKey );
    
    @Test
    public void testXor() {
        PolynomialFunctionGF2 xor = PolynomialFunctionGF2.XOR( 256 );
        Assert.assertEquals( 256 , xor.getInputLength() );
        Assert.assertEquals( 256 , xor.getContributions()[0].size() );
        long[] values = new long[ 4 ];
        
        for( int i = 0 ; i < values.length ; ++i ){ 
            values[ i ] = r.nextLong();
        }
        
        long[] expected = new long[] { 
                            values[0]^values[2] ,
                            values[1]^values[3] ,
                            0L ,
                            0L };
        
        BitVector result = xor.evaluate( new BitVector( values , 256 ) );
        Assert.assertEquals( 256 , result.size() );
        Assert.assertArrayEquals( expected , result.elements() );
    }
    
    @Test 
    public void HXor() {
        PolynomialFunctionGF2 xor = PolynomialFunctionGF2.XOR( 64 );
        PolynomialFunctionGF2 hXor = privKey.computeHomomorphicFunction( xor );
        
        BitVector v = BitUtils.randomBitVector( 64 );
        BitVector vConcatR = new BitVector( new long[] { 
                v.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        
        BitVector cv = pubKey.getEncrypter().evaluate( vConcatR );
        BitVector hResult = privKey.getDecryptor().evaluate( hXor.evaluate( cv ) );
        BitVector result = xor.evaluate( v );
        
        Assert.assertEquals( hResult, result );
    }
}
