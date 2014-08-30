package com.kryptnostic.bitwise;

import java.util.Random;

import org.junit.Test;

import com.kryptnostic.linear.BitUtils;

import cern.colt.bitvector.BitVector;
import org.junit.Assert;


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
}
