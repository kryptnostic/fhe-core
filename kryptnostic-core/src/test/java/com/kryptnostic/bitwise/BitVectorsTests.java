package com.kryptnostic.bitwise;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

public class BitVectorsTests {
    private static final int LENGTH = 512;
    private static final Random r = new Random();

    @Test
    // TODO: make a test that 2 unequal BVs are marshaled differently
    public void testBitvectorMarshalUnmarshal() {
        BitVector original = BitVectors.randomVector(LENGTH);

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
    
    @Test
    public void testBinaryConcatenate() {
        BitVector lhs = BitVectors.randomVector( r.nextInt( 256 )  + 1 );
        BitVector rhs = BitVectors.randomVector( r.nextInt( 256 ) + 1 );
        BitVector actual = BitVectors.concatenate( lhs , rhs );
        
        for( int i = 0 ; i < lhs.size(); ++i ) {
            Assert.assertEquals( lhs.get( i ) , actual.get( i ) );
        }
        
        for( int i = 0 ; i < rhs.size(); ++i ) {
            Assert.assertEquals( rhs.get( i ) , actual.get( lhs.size() + i ) );
        }
    }
    
    @Test
    public void testConcatenate() {
        
    }

    @Test
    public void testRandomVectorWithSpecificWeight() {
        int hammingWeight = r.nextInt(128);
        BitVector v = BitVectors.randomVector(128, hammingWeight);
        Assert.assertEquals(hammingWeight, v.cardinality());
    }

    @Test
    public void testRandomVectorReturnsCorrectLength() {
        int length = r.nextInt(128);
        BitVector v = BitVectors.randomVector(length);
        Assert.assertEquals(length, v.size());
    }

    @Test
    public void testExtend() {
        long[] values = { r.nextLong() };
        BitVector expected = new BitVector(values, 64);
        BitVector extended = BitVectors.extend(expected, 128);
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i), extended.get(i));
        }
    }

    @Test
    public void testSortByMapping() {
        long[] values = { r.nextLong() };
        BitVector original = new BitVector(values, 64);
        int[] mapping = new int[original.size()];
        for (int i = 0; i < mapping.length; i++) {
            int index = ( i + 10 ) % mapping.length;
            mapping[i] = index;
        }

        BitVector sorted = BitVectors.extendAndOrder(original, mapping, original.size());

        for (int i = 0; i < original.size(); i++) {
            Assert.assertEquals(original.get(i), sorted.get(mapping[i]));
        }
    }
}
