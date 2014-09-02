package com.kryptnostic.bitwise;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.linear.BitUtils;


public class BitUtilsTests {
    private static final Random r = new Random( 0 );
    
    @Test
    public void testRandomVectorWithSpecificWeight() {
        int hammingWeight = r.nextInt( 128 );
        BitVector v = BitUtils.randomVector( 128 , hammingWeight );
        Assert.assertEquals( hammingWeight , v.cardinality() );
    }
    
    @Test
    public void testRandomVectorReturnsCorrectLength() {
        int length = r.nextInt( 128 );
        BitVector v = BitUtils.randomVector( length );
        Assert.assertEquals( length , v.size() );
    }
    
    @Test
    public void testExtend() {
        long[] values = {r.nextLong()};  
        BitVector expected = new BitVector(values, 64);
        BitVector extended = BitUtils.extend(expected, 128);
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i), extended.get(i));
        }
    }
}
