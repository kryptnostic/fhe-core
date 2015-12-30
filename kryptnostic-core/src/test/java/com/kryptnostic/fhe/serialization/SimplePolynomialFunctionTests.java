package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

import cern.colt.bitvector.BitVector;

public class SimplePolynomialFunctionTests extends SerializationTestUtils {
    private static final int LEN = 256;

    @Test
    public void serializeSpfTest() throws JsonGenerationException, JsonMappingException, IOException {
        SimplePolynomialFunction spf = SimplePolynomialFunctions.lightRandomFunction(LEN, LEN);

        String expected = mapper.writeValueAsString(spf);

        Assert.assertEquals(expected, serialize(spf));
    }

    @Test
    public void deserializeSpfTest() throws JsonParseException, JsonMappingException, IOException {
        SimplePolynomialFunction spf = SimplePolynomialFunctions.lightRandomFunction(LEN, LEN);
        String serialized = mapper.writeValueAsString(spf);
        SimplePolynomialFunction out = mapper.readValue(serialized, SimplePolynomialFunction.class);
        Assert.assertEquals(spf, out);

        Assert.assertArrayEquals(spf.getContributions(), out.getContributions());
        Assert.assertArrayEquals(spf.getMonomials(), out.getMonomials());

        for (int i = 0; i < 10000; i++) {

            BitVector rando = BitVectors.randomVector(LEN);

            Assert.assertEquals(spf.apply(rando), out.apply(rando));
        }
    }

}
