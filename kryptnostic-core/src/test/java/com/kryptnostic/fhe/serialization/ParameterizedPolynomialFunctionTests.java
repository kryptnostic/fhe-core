package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

import cern.colt.bitvector.BitVector;

public class ParameterizedPolynomialFunctionTests {

    @Test
    public void testPpfSerialization() throws JsonProcessingException {
        SimplePolynomialFunction base = SimplePolynomialFunctions.randomFunction(128, 128);
        SimplePolynomialFunction[] pipelines = { SimplePolynomialFunctions.identity(128) };
        SimplePolynomialFunction parameterized = ParameterizedPolynomialFunctions.fromUnshiftedVariables(
                base.getInputLength(), base, pipelines);

        ObjectMapper mapper =  KodexObjectMapperFactory.getObjectMapper();
        String serializedPpf = mapper.writeValueAsString(parameterized);
        Assert.assertNotNull(serializedPpf);
    }

    @Test
    public void testPpfDeserialization() throws IOException {
        SimplePolynomialFunction base = SimplePolynomialFunctions.randomFunction(128, 128);
        SimplePolynomialFunction[] pipelines = { SimplePolynomialFunctions.identity(128) };
        SimplePolynomialFunction parameterized = ParameterizedPolynomialFunctions.fromUnshiftedVariables(
                base.getInputLength(), base, pipelines);

        ObjectMapper mapper =  KodexObjectMapperFactory.getObjectMapper();
        String serializedPpf = mapper.writeValueAsString(parameterized);

        SimplePolynomialFunction recovered = mapper.readValue(serializedPpf, SimplePolynomialFunction.class);
        BitVector input = BitVectors.randomVector(128);
        Assert.assertEquals(parameterized.apply(input), recovered.apply(input));
    }
}
