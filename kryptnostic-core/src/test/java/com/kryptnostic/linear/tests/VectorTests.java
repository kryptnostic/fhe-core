package com.kryptnostic.linear.tests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.linear.BitUtils;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class VectorTests {
    private static Logger logger = LoggerFactory.getLogger( VectorTests.class );
    @Test
    public void testAssumptions() {
        BitVector v = new BitVector( 64 );
        Assert.assertEquals( v.elements().length , 1 );
        
        v.set(63);
        Assert.assertEquals( v.elements()[0] , 1L << 63 );
        logger.info( "Actual representation: {}" , v.elements()[0] );
        v.set(0);
        Assert.assertEquals( v.elements()[0], (1L<<63) | 1L );
        logger.info( "Actual representation: {}" , v.elements()[0] );
        
    }
    
    @Test
    public void testParityAlgorithm() {
        long a = 1 , b = 3 , c = (1<<25)|(1<<7)|(1<<53), d = (1<<25)|(1<<7)|(1<<53)|(1<<2);
        
        Assert.assertEquals( BitUtils.parity( a ), 1L );
        Assert.assertEquals( BitUtils.parity( b ), 0L );
        Assert.assertEquals( BitUtils.parity( c ), 1L );
        Assert.assertEquals( BitUtils.parity( d ), 0L );
    }
}
