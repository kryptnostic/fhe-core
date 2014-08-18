package com.kryptnostic.multivariate;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Represents a single stage in the generation process for a pipeline of polynomial functions.
 * 
 * @author mtamayo
 */
public class PolynomialFunctionPipelineStage {
    private static final Logger logger = LoggerFactory.getLogger(PolynomialFunctionPipelineStage.class);
    private final EnhancedBitMatrix c1;
    private final EnhancedBitMatrix c2;
    private final SimplePolynomialFunction combination;
    private SimplePolynomialFunction lower;
    private SimplePolynomialFunction upper;
    private final SimplePolynomialFunction step;

    private PolynomialFunctionPipelineStage(SimplePolynomialFunction f, SimplePolynomialFunction inner) {
        c1 = EnhancedBitMatrix.randomInvertibleMatrix(f.getOutputLength());
        c2 = EnhancedBitMatrix.randomInvertibleMatrix(f.getOutputLength());
        combination = PolynomialFunctions.linearCombination(c1, c2);
        Pair<SimplePolynomialFunction, SimplePolynomialFunction> functionPair = PolynomialFunctions
                .randomlyPartitionMVQ(f);
        try {
            lower = c1.inverse().multiply(functionPair.getLeft());
            upper = c2.inverse().multiply(functionPair.getRight());
        } catch (SingularMatrixException e) {
            logger.error("Encountered singular matrix, where none should be possible due to generation procedure.");
        }
        step = PolynomialFunctions.concatenate(lower, upper).compose(inner);
    }

    public EnhancedBitMatrix getC1() {
        return c1;
    }

    public EnhancedBitMatrix getC2() {
        return c2;
    }

    public SimplePolynomialFunction getCombination() {
        return combination;
    }

    public SimplePolynomialFunction getLower() {
        return lower;
    }

    public SimplePolynomialFunction getUpper() {
        return upper;
    }

    public SimplePolynomialFunction getStep() {
        return step;
    }

    public static PolynomialFunctionPipelineStage build(SimplePolynomialFunction f, SimplePolynomialFunction inner) {
        return new PolynomialFunctionPipelineStage(f, inner);
    }
}
