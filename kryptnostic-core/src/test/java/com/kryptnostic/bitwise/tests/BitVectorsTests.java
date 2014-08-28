package com.kryptnostic.bitwise.tests;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.BitUtils;

public class BitVectorsTests {
    private static final int LENGTH = 512;

    @Test
    // TODO: make a test that 2 unequal BVs are marshaled differently
    public void testBitvectorMarshalUnmarshal() {
        BitVector original = BitUtils.randomVector(LENGTH);

        String data = null;

        long start = System.nanoTime();
        int runs = 300;
        for (int i = 0; i < runs; i++) {
            data = BitVectors.marshalBitvector(original);
        }
        long time = System.nanoTime() - start;
        System.out.printf("Average time to marshal was %.1f us%n", time / runs / 1e3);

        start = System.nanoTime();
        BitVector result = null;
        for (int i = 0; i < runs; i++) {
            result = BitVectors.unmarshalBitvector(data);
        }
        time = System.nanoTime() - start;
        System.out.printf("Average time to unmarshal was %.1f us%n", time / runs / 1e3);

        Assert.assertArrayEquals(original.elements(), result.elements());
        
        Assert.assertEquals(original, result);
    }

    @Test
    public void testBitvectorMarshalNull() {
        Assert.assertNull(BitVectors.marshalBitvector(null));
    }

    @Test
    public void testBitvectorUnmarshalNull() {
        Assert.assertNull(BitVectors.unmarshalBitvector(null));
    }
}
