package com.kryptnostic.bitwise.tests;

import java.util.Random;

import org.junit.Test;

import com.kryptnostic.linear.BitUtils;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class BitUtilsTests {
    private static final Random r = new Random( 0 );
    
    @Test
    public void testRandomVector() {
        int hammingWeight = r.nextInt( 128 );
        BitVector v = BitUtils.randomVector( 128 , hammingWeight );
        Assert.assertEquals( hammingWeight , v.cardinality() );
    }
}
