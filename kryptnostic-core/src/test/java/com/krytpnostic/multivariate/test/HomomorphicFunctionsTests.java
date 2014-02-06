package com.krytpnostic.multivariate.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.crypto.fhe.PolynomialFunctions;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * Tests for special polynomial function provided as building blocks for more complex functions.
 * @author Matthew Tamayo-Rios
 */
public class HomomorphicFunctionsTests {
    private static final Logger logger = LoggerFactory.getLogger( HomomorphicFunctionsTests.class );
    private static final Random r = new Random( System.currentTimeMillis() );
    private static final PrivateKey privKey = new PrivateKey( 128 , 64 );
    private static final PublicKey pubKey = new PublicKey( privKey );
    
    @Test 
    public void testHomomorphicXor() {
        SimplePolynomialFunction xor = PolynomialFunctions.XOR( 64 );
        long start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicXor = privKey.computeHomomorphicFunction( xor );
        long stop = System.currentTimeMillis();
        logger.info( "Homomorphic XOR generation took {} ms" , stop - start );
        logger.info( "Homomorphic XOR has {} monomials" , homomorphicXor.getTotalMonomialCount() );
        
        BitVector v = BitUtils.randomVector( 64 );
        BitVector vConcatR = new BitVector( new long[] { 
                v.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        
        BitVector cv = pubKey.getEncrypter().apply( vConcatR );
        start = System.currentTimeMillis();
        BitVector hResult = privKey.getDecryptor().apply( homomorphicXor.apply( cv ) );
        stop = System.currentTimeMillis();
        logger.info( "Homomorphic XOR evaluation took {} ms" , stop - start );
        BitVector result = xor.apply( v );
        
        Assert.assertEquals( hResult, result );
    }
    
    @Test 
    public void testHomomorphicAnd() {
        SimplePolynomialFunction and = PolynomialFunctions.AND( 64 );
        long start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicAnd = privKey.computeHomomorphicFunction( and );
        long stop = System.currentTimeMillis();
        logger.info( "Homomorphic AND generation took {} ms" , stop - start );
        logger.info( "Homomorphic AND has {} monomials" , homomorphicAnd.getTotalMonomialCount() );
        
        BitVector v = BitUtils.randomVector( 64 );
        BitVector vConcatR = new BitVector( new long[] { 
                v.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        
        BitVector cv = pubKey.getEncrypter().apply( vConcatR );
        start = System.currentTimeMillis();
        BitVector hResult = privKey.getDecryptor().apply( homomorphicAnd.apply( cv ) );
        stop = System.currentTimeMillis();
        logger.info( "Homomorphic AND evaluation took {} ms" , stop - start );
        BitVector result = and.apply( v );
        
        Assert.assertEquals( hResult, result );
    }
}
