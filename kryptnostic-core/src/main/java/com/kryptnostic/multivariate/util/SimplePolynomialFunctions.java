package com.kryptnostic.multivariate.util;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;


public class SimplePolynomialFunctions {
    private static final Logger logger = LoggerFactory.getLogger(SimplePolynomialFunctions.class);
    private SimplePolynomialFunctions() {}

    /**
     * Uses the output map to indicate the indices of the output bits of the lhs function in the new function.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction concatenateInputsAndOutputs(SimplePolynomialFunction lhs,
            SimplePolynomialFunction rhs) {
        if (lhs.isParameterized() || rhs.isParameterized()) {
            try {
                return ParameterizedPolynomialFunctions.concatenateInputsAndOutputs(lhs, rhs);
            } catch (Exception e) {
                logger.error("Exception in parameterized function concatenation.");
            }
        }
        Pair<int[], int[]> inputMaps = getSplitMap(lhs.getInputLength(), rhs.getInputLength());
        Pair<int[], int[]> outputMaps = getSplitMap(lhs.getOutputLength(), rhs.getOutputLength());

        return interleaveFunctions(lhs, rhs, inputMaps.getLeft(), inputMaps.getRight(), outputMaps.getLeft(),
                outputMaps.getRight());
    }

    /**
     * Generates a tuple of mappings the first array maps the lhLength indices, and the second array maps to the
     * rhLength indices.
     * 
     * @return Pair of lhMap and rhMap
     */
    public static Pair<int[], int[]> getSplitMap(int lhLength, int rhLength) {
        int totalLength = lhLength + rhLength;
        int[] lhMap = new int[lhLength];
        int[] rhMap = new int[rhLength];
        for (int i = 0; i < totalLength; i++) {
            if (i < lhLength) {
                lhMap[i] = i;
            } else {
                rhMap[i - lhLength] = i;
            }
        }
        return Pair.of(lhMap, rhMap);
    }

    /**
     * Interleave two functions using the arrays as maps for the new monomial and contribution orderings.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction interleaveFunctions(SimplePolynomialFunction lhs,
            SimplePolynomialFunction rhs, int[] lhsInputMap, int[] rhsInputMap, int[] lhsOutputMap, int[] rhsOutputMap) {
        int combinedInputLength = lhs.getInputLength() + rhs.getInputLength();
        int combinedOutputLength = lhs.getOutputLength() + rhs.getOutputLength();
        int numTerms = lhs.getMonomials().length + rhs.getMonomials().length;
        Monomial[] newMonomials = new Monomial[numTerms];
        BitVector[] newContributions = new BitVector[numTerms];

        mapAndAddTerms(newMonomials, newContributions, combinedInputLength, combinedOutputLength, 0, lhs, lhsInputMap,
                lhsOutputMap);
        mapAndAddTerms(newMonomials, newContributions, combinedInputLength, combinedOutputLength,
                lhs.getMonomials().length, rhs, rhsInputMap, rhsOutputMap);

        return new OptimizedPolynomialFunctionGF2(combinedInputLength, combinedOutputLength, newMonomials,
                newContributions);
    }
    

    /**
     * Map the terms of inner onto larger arrays of monomials and contributions.
     */
    private static void mapAndAddTerms(Monomial[] newMonomials, BitVector[] newContributions, int combinedInputLength,
            int combinedOutputLength, int baseIndex, SimplePolynomialFunction inner, int[] inputMap, int[] outputMap) {
        Preconditions.checkArgument(inner.getInputLength() <= combinedInputLength,
                "Inner input length cannot be larger than new input length.");
        Preconditions.checkArgument(inner.getOutputLength() <= combinedOutputLength,
                "Inner output length cannot be larger than new output length.");
        Preconditions.checkArgument(inner.getMonomials().length <= newMonomials.length,
                "Array of old monomials cannot be larger than new monomial array.");
        
        Monomial[] monomials = inner.getMonomials();
        for (int i = 0; i < monomials.length; i++) {
            BitVector backingVector = BitVectors.extendAndOrder(monomials[i], inputMap, combinedInputLength);
            Monomial newMonomial = new Monomial(combinedInputLength);
            newMonomial.xor(backingVector);
            BitVector newContribution = BitVectors.extendAndOrder(inner.getContributions()[i], outputMap,
                    combinedOutputLength);

            newMonomials[baseIndex + i] = newMonomial;
            newContributions[baseIndex + i] = newContribution;
        }
    }
}
