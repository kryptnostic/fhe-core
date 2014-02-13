package com.krytpnostic.multivariate.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.crypto.fhe.HomomorphicFunctions;
import com.kryptnostic.crypto.fhe.PolynomialFunctions;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * Tests for special polynomial function provided as building blocks for more complex functions.
 * @author Matthew Tamayo-Rios
 */
public class HomomorphicFunctionsTests {
    private static final Logger logger = LoggerFactory.getLogger( HomomorphicFunctionsTests.class );
    private static final Random r = new Random( System.currentTimeMillis() );
    private static final PrivateKey privateKey = new PrivateKey( 128 , 64 );
    private static final PublicKey pubKey = new PublicKey( privateKey );
    
    @Test 
    public void testHomomorphicXor() {
        SimplePolynomialFunction xor = PolynomialFunctions.XOR( 64 );
        long start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicXor = privateKey.computeHomomorphicFunction( xor );
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
        BitVector hResult = homomorphicXor.apply( cv ); 
        stop = System.currentTimeMillis();
        hResult = privateKey.getDecryptor().apply( hResult );
        logger.info( "Homomorphic XOR evaluation took {} ms" , stop - start );
        BitVector result = xor.apply( v );
        
        Assert.assertEquals( hResult, result );
    }
    /*
    @Test 
    public void testHomomorphicAnd() {
        SimplePolynomialFunction and = PolynomialFunctions.AND( 64 );
        long start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicAnd = privateKey.computeHomomorphicFunction( and );
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
        BitVector hResult = homomorphicAnd.apply( cv );
        stop = System.currentTimeMillis();
        hResult = privateKey.getDecryptor().apply( hResult );
        logger.info( "Homomorphic AND evaluation took {} ms" , stop - start );
        BitVector result = and.apply( v );
        
        Assert.assertEquals( hResult, result );
    }
    */
    /*
    @Test
    public void testHomomorphicAdder() {
        long start , stop;
        
        start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicXor = HomomorphicFunctions.BinaryHomomorphicXor( 64 , privateKey );
        stop = System.currentTimeMillis();
        
        start = System.currentTimeMillis();
        logger.info( "Homomorphic XOR generation took {} ms" , stop - start );
        
        start = System.currentTimeMillis();
        SimplePolynomialFunction homomorphicCarry = HomomorphicFunctions.BinaryHomomorphicCarry( 64 , privateKey );
        stop = System.currentTimeMillis();
        logger.info( "Homomorphic CARRY generation took {} ms" , stop - start );
        
        PolynomialFunction adder = PolynomialFunctions.ADDER( 64 );
        PolynomialFunction homomorphicAdder = PolynomialFunctions.ADDER( 64 , homomorphicXor, homomorphicCarry );
        BitVector v = BitUtils.randomVector( 64 );
        BitVector vConcatR = new BitVector( new long[] { 
                v.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        
        BitVector cv = pubKey.getEncrypter().apply( vConcatR );
        
        start = System.currentTimeMillis();
        BitVector hResult = homomorphicAdder.apply( cv );
        stop = System.currentTimeMillis();
        hResult = privateKey.getDecryptor().apply( hResult );
        logger.info( "Homomorphic AND evaluation took {} ms" , stop - start );
        BitVector result = adder.apply( v );
        
        Assert.assertEquals( hResult, result );
    }
    */
    @Test
    public void testHomomorphicHalfAdder() {
        long start, stop;
        SimplePolynomialFunction homomorphicHalfAdder = HomomorphicFunctions.HomomorphicHalfAdder( 64 , privateKey );
        SimplePolynomialFunction adder = PolynomialFunctions.HALF_ADDER( 64 );
        BitVector v1 = BitUtils.randomVector( 64 );
        BitVector v2 = BitUtils.randomVector( 64 );
        BitVector vConcatR1 = new BitVector( new long[] { 
                v1.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        BitVector vConcatR2 = new BitVector( new long[] { 
                v2.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        BitVector cv1 = pubKey.getEncrypter().apply( vConcatR1 );
        BitVector cv2 = pubKey.getEncrypter().apply( vConcatR2 );
        start = System.currentTimeMillis();
        BitVector hResult = homomorphicHalfAdder.apply( cv1 , cv2 );
        stop = System.currentTimeMillis();
//        BitVector p1 = new BitVector( new long[] { hResult.elements()[ 0 ] , hResult.elements()[ 1 ] } , 128 );
//        BitVector p2 = new BitVector( new long[] { hResult.elements()[ 2 ] , hResult.elements()[ 3 ] } , 128 );
//        BitVector dp1 = privateKey.getDecryptor().apply( p1 );
//        BitVector dp2 = privateKey.getDecryptor().apply( p2 );
//        hResult = FunctionUtils.concatenate( dp1 , dp2 );
        logger.info( "Homomorphic half adder evaluation took {} ms" , stop - start );
        BitVector result = adder.apply( v1 , v2 );
        
        Assert.assertEquals( hResult, result );
    }
}
