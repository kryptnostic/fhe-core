package com.kryptnostic.multivariate.parameterization;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class ParameterizedPolynomialFunctionTests {

    @Test
    public void testConcatenateInputsAndOutputs() {
        SimplePolynomialFunction function = PolynomialFunctions.randomFunction(128, 64);
        SimplePolynomialFunction function2 = PolynomialFunctions.randomFunction(128, 128);
        SimplePolynomialFunction[] pipeline = { function2 };

        ParameterizedPolynomialFunctionGF2 parameterized = (ParameterizedPolynomialFunctionGF2) ParameterizedPolynomialFunctions
                .fromUnshiftedVariables(function.getInputLength(), function, pipeline);

        SimplePolynomialFunction concat = ParameterizedPolynomialFunctions.concatenateInputsAndOutputs(function2,
                parameterized);

        BitVector input1 = BitUtils.randomVector(128);
        BitVector input2 = BitUtils.randomVector(128);
        BitVector expectedResult = FunctionUtils.concatenate(parameterized.apply(input1), function2.apply(input2));
        BitVector actualResult = concat.apply(FunctionUtils.concatenate(input1, input2));
        Assert.assertEquals(expectedResult, actualResult);
    }
}
