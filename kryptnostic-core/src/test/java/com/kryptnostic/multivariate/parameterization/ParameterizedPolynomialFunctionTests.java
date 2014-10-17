package com.kryptnostic.multivariate.parameterization;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

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
        SimplePolynomialFunction base = PolynomialFunctions.lightRandomFunction(128, 128);
        SimplePolynomialFunction[] pipelines = {PolynomialFunctions.identity(128)};
        SimplePolynomialFunction inner = ParameterizedPolynomialFunctions.fromUnshiftedVariables(base.getInputLength(), base, pipelines);
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
        
        SimplePolynomialFunction base = PolynomialFunctions.lightRandomFunction(64, 128);
        base = ParameterizedPolynomialFunctions.extend(128, base);
        List<CompoundPolynomialFunction> pipelines = Lists.newArrayList();
        pipelines.add(new CompoundPolynomialFunctionGF2(Lists.newArrayList(PolynomialFunctions.identity(64))));
        SimplePolynomialFunction outer = new ParameterizedPolynomialFunctionGF2(64, base.getOutputLength(), base.getMonomials(), base.getContributions(), pipelines);
        BitVector input = BitVectors.randomVector(64);
        BitVector intermediate = inner.apply(input);
        BitVector expected = outer.apply(intermediate);
        logger.info("Composing with outer parameterized function.");
        SimplePolynomialFunction composed = outer.compose(inner);
        logger.info("Done composing.");
        BitVector actual = composed.apply(input);
        Assert.assertEquals(expected, actual);
    }
    
    // TODO compose parameterized with parameterized
}
