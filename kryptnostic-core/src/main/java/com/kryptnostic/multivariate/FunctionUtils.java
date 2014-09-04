package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class FunctionUtils {
    private FunctionUtils() {
    }

    public static <T> T[] mergeArrays(T[] lhs, T[] rhs) {
        T[] newArray = Arrays.copyOf(lhs, lhs.length + rhs.length);
        for (int i = lhs.length; i < rhs.length; ++i) {
            newArray[i] = rhs[i - lhs.length];
        }

        return newArray;
    }

    public static BitVector concatenate(BitVector... elements) {
        Preconditions.checkArgument(
                Preconditions.checkNotNull(elements, "Null elements are not concatenatable.").length > 1,
                "Need at least two elements to concatenate");
        int newLength = 0;
        for (BitVector v : elements) {
            Preconditions.checkArgument(v.size() % 64 == 0, "Concatenate only works for block lengths of size 64.");
            newLength += v.elements().length;
        }

        BitVector result = new BitVector(newLength << 6);
        long[] resultBits = result.elements();
        int i = 0;
        for (BitVector v : elements) {
            long[] bits = v.elements();
            for (int j = 0; j < bits.length; ++j) {
                resultBits[i++] = bits[j];
            }

        }

        return result;
    }

    public static BitVector concatenate(BitVector lhs, BitVector rhs) {
        Preconditions.checkArgument(( lhs.size() % 64 ) == 0 && ( rhs.size() % 64 ) == 0,
                "Block length for concatenate must be a multiple of 64.");
        BitVector concatenated = new BitVector(Arrays.copyOf(lhs.elements(), lhs.elements().length
                + rhs.elements().length), lhs.size() + rhs.size());
        for (int i = 0; i < rhs.elements().length; ++i) {
            concatenated.elements()[i + lhs.elements().length] = rhs.elements()[i];
        }
        return concatenated;
    }

    /**
     * Provides a mutable map view over an array of {@code Monomial}s and corresponding {@code BitVector} contributions.
     * 
     * @param monomials
     *            The monomials to use as the map key
     * @param contributions
     *            The contributions paired with each monomial.
     * @return a {@code HashMap<Monomial,BitVector} with each monomial paired to its contribution.
     */
    public static Map<Monomial, BitVector> mapViewFromMonomialsAndContributions(Monomial[] monomials,
            BitVector[] contributions) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize(monomials.length);
        for (int i = 0; i < monomials.length; ++i) {
            result.put(monomials[i], contributions[i]);
        }
        return result;
    }

    public static SimplePolynomialFunction fromString(int monomialSize, String polynomialString) {
        List<String> lines = Splitter.on("\n").omitEmptyStrings().trimResults().splitToList(polynomialString);
        int row = 0;
        Map<Monomial, BitVector> monomialContributionsMap = Maps.newHashMap();
        for (String line : lines) {
            Iterable<String> monomials = Splitter.on("+").trimResults().omitEmptyStrings().split(line);

            for (String monomialString : monomials) {
                Monomial m = Monomial.fromString(monomialSize, monomialString);
                BitVector contribution = monomialContributionsMap.get(m);
                if (contribution == null) {
                    contribution = new BitVector(lines.size());
                    monomialContributionsMap.put(m, contribution);
                }
                contribution.set(row);
            }
            ++row;
        }

        return PolynomialFunctions.fromMonomialContributionMap(monomialSize, lines.size(), monomialContributionsMap);
    }

    /**
     * Uses the output map to indicate the indices of the output bits of the lhs function in the new function.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction concatenateInputsAndOutputs(SimplePolynomialFunction lhs,
            SimplePolynomialFunction rhs) {
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
    private static Pair<int[], int[]> getSplitMap(int lhLength, int rhLength) {
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
            BitVector backingVector = BitUtils.extendAndOrder(monomials[i], inputMap, combinedInputLength);
            Monomial newMonomial = new Monomial(combinedInputLength);
            newMonomial.xor(backingVector);
            BitVector newContribution = BitUtils.extendAndOrder(inner.getContributions()[i], outputMap,
                    combinedOutputLength);

            newMonomials[baseIndex + i] = newMonomial;
            newContributions[baseIndex + i] = newContribution;
        }
    }

}
