package com.kryptnostic.fhe.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.kryptnostic.bitwise.BitVectors;

import cern.colt.bitvector.BitVector;

public class BitVectorTests extends SerializationTestUtils {

    private static final int LEN = 256;
    private final String LIST_SEPARATOR = ",";

    @Test
    public void serializeBitvectorTest() throws JsonGenerationException, JsonMappingException, IOException {
        BitVector bv = BitVectors.randomVector(LEN);
        String expected = wrapQuotes(BitVectors.marshalBitvector(bv));
        Assert.assertEquals(expected, serialize(bv));
    }

    @Test
    public void deserializeBitvectorTest() throws JsonGenerationException, JsonMappingException, IOException {
        BitVector bv = BitVectors.randomVector(LEN);
        String serialized = wrapQuotes(BitVectors.marshalBitvector(bv));
        BitVector out = deserialize(serialized, BitVector.class);

        Assert.assertEquals(bv, out);
    }

    @Test
    public void serializeBitvectorCollectionTest() throws JsonGenerationException, JsonMappingException, IOException {
        Collection<BitVector> vectors = generateCollection(10);
        String expectedResult = generateExpectedCollectionResult(vectors);

        Assert.assertEquals(expectedResult, serialize(vectors));
    }

    @Test
    public void deserializeBitvectorCollectionTest() throws JsonGenerationException, JsonMappingException, IOException {
        Collection<BitVector> vectors = generateCollection(10);
        String inputString = generateExpectedCollectionResult(vectors);

        Collection<BitVector> vals = Arrays.asList((BitVector[])deserialize(inputString, BitVector[].class));

        Assert.assertEquals(vectors, vals);
    }

    private Collection<BitVector> generateCollection(int size) {
        Collection<BitVector> vectors = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            vectors.add(BitVectors.randomVector(LEN));
        }
        return vectors;
    }

    /**
     * Generate expected serialization result for collection of bitvectors
     * 
     * @param vectors
     * @return
     */
    private String generateExpectedCollectionResult(Collection<BitVector> vectors) {
        String expectedResult = "";
        Iterator<BitVector> iter = vectors.iterator();
        while (iter.hasNext()) {
            expectedResult += wrapQuotes(BitVectors.marshalBitvector(iter.next()));
            if (iter.hasNext()) {
                expectedResult += LIST_SEPARATOR;
            }
        }
        return "[" + expectedResult + "]";
    }
}
