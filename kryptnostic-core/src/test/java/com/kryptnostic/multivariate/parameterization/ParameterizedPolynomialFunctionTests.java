package com.kryptnostic.multivariate.parameterization;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;
import com.kryptnostic.multivariate.util.PolynomialFunctions;

public class ParameterizedPolynomialFunctionTests {
    private static final Logger logger = LoggerFactory.getLogger(ParameterizedPolynomialFunctionTests.class);

    @Test
    public void testConcatenateInputsAndOutputs() throws Exception {
        SimplePolynomialFunction function = PolynomialFunctions.lightRandomFunction(128, 64);
        SimplePolynomialFunction function2 = PolynomialFunctions.lightRandomFunction(128, 128);

        SimplePolynomialFunction function3 = PolynomialFunctions.lightRandomFunction(128, 128);
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

    @Test
    public void testComposeInnerParameterized() {
        SimplePolynomialFunction outer = PolynomialFunctions.lightRandomFunction(128, 64);
        SimplePolynomialFunction inner = ParameterizedPolynomialFunctions.randomParameterizedFunction(128, 128);
        logger.info("Composing with inner parameterized function.");
        SimplePolynomialFunction composed = outer.compose(inner);
        logger.info("Done composing.");
        BitVector input = BitVectors.randomVector(128);
        BitVector intermediate = inner.apply(input);
        BitVector expected = outer.apply(intermediate);
        BitVector actual = composed.apply(input);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testComposeOuterParameterized() {
        SimplePolynomialFunction inner = PolynomialFunctions.lightRandomFunction(64, 64);
        SimplePolynomialFunction outer = ParameterizedPolynomialFunctions.randomParameterizedFunction(64, 128);

        BitVector input = BitVectors.randomVector(64);
        BitVector intermediate = inner.apply(input);
        BitVector expected = outer.apply(intermediate);
        logger.info("Composing with outer parameterized function.");
        SimplePolynomialFunction composed = outer.compose(inner);
        logger.info("Done composing.");
        BitVector actual = composed.apply(input);
        Assert.assertEquals(expected, actual);
    }

    // TODO fix bug when inputs and outputs not same length. is caused by pipelines not matching correctly (both sized by input)
    @Test
    public void testComposeParameterizedWithParameterized() {
        SimplePolynomialFunction inner = ParameterizedPolynomialFunctions.randomParameterizedFunction(64, 64);
        SimplePolynomialFunction outer = ParameterizedPolynomialFunctions.randomParameterizedFunction(64, 64);
        
        SimplePolynomialFunction composed = outer.compose(inner);
        BitVector input = BitVectors.randomVector(64);
        
        Assert.assertEquals(outer.apply(inner.apply(input)), composed.apply(input));
    }

    // Simply tests that a random parameterized function can be evaluated.
    @Test
    public void testRandomParameterizedFunction() {
        int inputLength = 128, outputLength = 128;
        SimplePolynomialFunction ppf = ParameterizedPolynomialFunctions.randomParameterizedFunction(inputLength, outputLength);
        Assert.assertNotNull(ppf.apply(BitVectors.randomVector(inputLength)));
    }
}
