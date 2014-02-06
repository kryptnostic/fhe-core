package com.krytpnostic.multivariate.test;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.crypto.fhe.PolynomialFunctions;
import com.kryptnostic.linear.BitUtils;
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
    public void testLSH() {
        SimplePolynomialFunction lsh = PolynomialFunctions.LSH( 128 , 23 );
        BitVector v = BitUtils.randomVector( 128 );
        BitVector result = lsh.apply( v );
        logger.info("Original vector: {}" , v );
        logger.info("Vector left shifted {} times: {}" , 23 , result );
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
        logger.info("Original vector: {}" , v );
        logger.info("Vector right shifted {} times: {}" , 23 , result );
        long v0 = ( v.elements()[0] >>> 23 ) | ( v.elements()[1] << 41 );
        long v1 = ( v.elements()[1] >>> 23 ) ; 
        Assert.assertEquals( v0 , result.elements()[0] );
        Assert.assertEquals( v1 , result.elements()[1] );
    }
    
    @Test
    public void testADDER() {
        long start = System.currentTimeMillis();
        PolynomialFunction adder = PolynomialFunctions.ADDER( 128 );
        long stop = System.currentTimeMillis();
        logger.info("Adder generation took {} ms" , stop - start );
        BitVector v = BitUtils.randomVector( 128 );
        BitVector result = adder.apply( v );
        Assert.assertEquals( v.elements()[ 0 ] + v.elements()[1] , result.elements()[0] );
    }
}
