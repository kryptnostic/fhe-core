package com.kryptnostic.multivariate.parameterization;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class ParameterizedPolynomialFunctionTests {

    @Test
    public void testConcatenateInputsAndOutputs() throws Exception {
        SimplePolynomialFunction function = PolynomialFunctions.randomFunction(128, 64);
        SimplePolynomialFunction function2 = PolynomialFunctions.randomFunction(128, 128);

        SimplePolynomialFunction function3 = PolynomialFunctions.randomFunction(128, 128);
        SimplePolynomialFunction[] pipeline = { function3 };

        ParameterizedPolynomialFunctionGF2 parameterized = (ParameterizedPolynomialFunctionGF2) ParameterizedPolynomialFunctions
                .fromUnshiftedVariables(function.getInputLength(), function, pipeline);

        SimplePolynomialFunction concat = ParameterizedPolynomialFunctions.concatenateInputsAndOutputs(parameterized,
                function2);

        BitVector input1 = BitVectors.randomVector(128);
        BitVector input2 = BitVectors.randomVector(128);
        BitVector expectedResult = BitVectors.concatenate(parameterized.apply(input1), function2.apply(input2));
        BitVector actualResult = concat.apply(BitVectors.concatenate(input1, input2));
        Assert.assertEquals(expectedResult, actualResult);
    }
}
