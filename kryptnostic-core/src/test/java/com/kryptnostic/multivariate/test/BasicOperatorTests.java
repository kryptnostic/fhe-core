package com.kryptnostic.multivariate.test;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

public class BasicOperatorTests {
    private static Logger logger = LoggerFactory.getLogger( BasicOperatorTests.class );
    private static final Random r = new Random( 0 );
    
    @Test
    public void testXor() {
        SimplePolynomialFunction xor = PolynomialFunctions.XOR( 256 );
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
        
        BitVector result = xor.apply( new BitVector( values , 256 ) );
        Assert.assertEquals( 256 , result.size() );
        Assert.assertArrayEquals( expected , result.elements() );
    }
    
    @Test
    public void testBinaryXor() {
        SimplePolynomialFunction xor = PolynomialFunctions.BINARY_XOR( 128 );
        Assert.assertEquals( 256 , xor.getInputLength() );
        Assert.assertEquals( 128 , xor.getContributions()[0].size() );
        
        long[] values = new long[ 4 ];
        
        for( int i = 0 ; i < values.length ; ++i ){ 
            values[ i ] = r.nextLong();
        }
        
        long[] expected = new long[] { 
                            values[0]^values[2] ,
                            values[1]^values[3] };
        
        BitVector result = xor.apply( new BitVector( values , 256 ) );
        BitVector result2 = xor.apply( new BitVector( Arrays.copyOfRange( values , 0, 2 ) , 128 ) , new BitVector( Arrays.copyOfRange( values , 2 , 4 ) , 128 ) );
        Assert.assertEquals( 128 , result.size() );
        Assert.assertEquals( 128 , result2.size() );
        Assert.assertArrayEquals( expected , result.elements() );
        Assert.assertArrayEquals( expected , result2.elements() );
    }
    
    @Test
    public void testAnd() {
        SimplePolynomialFunction and = PolynomialFunctions.AND( 256 );
        Assert.assertEquals( 256 , and.getInputLength() );
        Assert.assertEquals( 256 , and.getContributions()[0].size() );

        long[] values = new long[ 4 ];
        
        for( int i = 0 ; i < values.length ; ++i ){ 
            values[ i ] = r.nextLong();
        }
        
        long[] expected = new long[] { 
                            values[0]&values[2] ,
                            values[1]&values[3] ,
                            0L ,
                            0L };
        
        BitVector result = and.apply( new BitVector( values , 256 ) );
        Assert.assertEquals( 256 , result.size() );
        Assert.assertArrayEquals( expected , result.elements() );
    }
    
    @Test
    public void testBinaryAnd() {
        SimplePolynomialFunction and = PolynomialFunctions.BINARY_AND( 128 );
        Assert.assertEquals( 256 , and.getInputLength() );
        Assert.assertEquals( 128 , and.getContributions()[0].size() );

        long[] values = new long[ 4 ];
        
        for( int i = 0 ; i < values.length ; ++i ){ 
            values[ i ] = r.nextLong();
        }
        
        long[] expected = new long[] { 
                            values[0]&values[2] ,
                            values[1]&values[3] };
        
        BitVector result = and.apply( new BitVector( values , 256 ) );
        BitVector result2 = and.apply( new BitVector( Arrays.copyOfRange( values , 0, 2 ) , 128 ) , new BitVector( Arrays.copyOfRange( values , 2 , 4 ) , 128 ) );
        Assert.assertEquals( 128 , result.size() );
        Assert.assertEquals( 128 , result2.size() );
        Assert.assertArrayEquals( expected , result.elements() );
        Assert.assertArrayEquals( expected , result2.elements() );
    }
    
    @Test
    public void testNEG() {
        SimplePolynomialFunction neg = PolynomialFunctions.NEG( 128 );
        BitVector v = BitUtils.randomVector( 128 );
        BitVector result = neg.apply( v );
        BitVector expected = v.copy();
        expected.not();
        Assert.assertEquals( expected , result );
    }
    
    @Test
    public void testLSH() {
        SimplePolynomialFunction lsh = PolynomialFunctions.LSH( 128 , 23 );
        BitVector v = BitUtils.randomVector( 128 );
        BitVector result = lsh.apply( v );
        logger.trace("Original vector: {}" , v );
        logger.trace("Vector left shifted {} times: {}" , 23 , result );
        long v0 = v.elements()[0] << 23;
        long v1 = ( v.elements()[1] << 23 ) | ( v.elements()[0] >>> 41 ); 
        Assert.assertEquals( v0 , result.elements()[0] );
        Assert.assertEquals( v1 , result.elements()[1] );
    }
    
    @Test
    public void testRSH() {
        SimplePolynomialFunction rsh = PolynomialFunctions.RSH( 128 , 23 );
        BitVector v = BitUtils.randomVector( 128 );
        BitVector result = rsh.apply( v );
        logger.trace("Original vector: {}" , v );
        logger.trace("Vector right shifted {} times: {}" , 23 , result );
        long v0 = ( v.elements()[0] >>> 23 ) | ( v.elements()[1] << 41 );
        long v1 = ( v.elements()[1] >>> 23 ) ; 
        Assert.assertEquals( v0 , result.elements()[0] );
        Assert.assertEquals( v1 , result.elements()[1] );
    }
    
    @Test
    public void testADDER() {
        long start = System.currentTimeMillis();
        PolynomialFunction adder = PolynomialFunctions.ADDER( 64 );
        long stop = System.currentTimeMillis();
        logger.trace("Adder generation took {} ms" , stop - start );
        start = System.currentTimeMillis();
        for( int i = 0 ; i < 100 ; ++i ) {
            BitVector u = BitUtils.randomVector( 64 );
            BitVector v = BitUtils.randomVector( 64 );

            logger.trace("Adder input length: {}", adder.getInputLength() );
            BitVector result = adder.apply( u , v );

            logger.trace("Expected: {} + {} = {}" , u.elements()[0] , v.elements()[ 0 ] , u.elements()[0] + v.elements()[ 0 ] );
            logger.trace("Observed: {} + {} = {}" , u.elements()[0] , v.elements()[ 0 ] , result.elements()[ 0 ] );
            Assert.assertEquals( u.elements()[ 0 ] + v.elements()[0] , result.elements()[0] );
        }
        stop = System.currentTimeMillis();
        logger.trace( "Average time to evaluate adder: {} ms" , ( stop - start ) / 100.0D  );
        
    }
}
