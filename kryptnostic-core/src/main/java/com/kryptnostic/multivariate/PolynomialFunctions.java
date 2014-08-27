package com.kryptnostic.multivariate;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Utility and factory methods for PolynomialFunctions.
 * 
 * @author Matthew Tamayo-Rios
 */
public final class PolynomialFunctions {
    private static final Logger logger = LoggerFactory.getLogger(PolynomialFunctions.class);
    private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int INTEGER_BYTES_X4 = INTEGER_BYTES * 4;
    private static final Base64 codec = new Base64();

    private PolynomialFunctions() {
    }

    public static SimplePolynomialFunction XOR(int xorLength) {
        int inputLength = xorLength >>> 1;
        Monomial[] monomials = new Monomial[xorLength];
        BitVector[] contributions = new BitVector[xorLength];

        for (int i = 0; i < inputLength; ++i) {
            int offsetIndex = i + inputLength;
            monomials[i] = Monomial.linearMonomial(xorLength, i);
            monomials[offsetIndex] = Monomial.linearMonomial(xorLength, offsetIndex);
            BitVector contribution = new BitVector(xorLength);
            contribution.set(i);
            contributions[i] = contribution;
            /*
             * In theory everything else makes a copy so we could cheat here and save memory.
             */
            contributions[offsetIndex] = contribution.copy();
        }

        return new OptimizedPolynomialFunctionGF2(xorLength, xorLength, monomials, contributions);
    }

    public static SimplePolynomialFunction BINARY_XOR(int xorLength) {
        int inputLength = xorLength << 1;
        Monomial[] monomials = new Monomial[inputLength];
        BitVector[] contributions = new BitVector[inputLength];

        for (int i = 0; i < xorLength; ++i) {
            int offsetIndex = i + xorLength;
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            monomials[offsetIndex] = Monomial.linearMonomial(inputLength, offsetIndex);
            BitVector contribution = new BitVector(xorLength);
            contribution.set(i);
            contributions[i] = contribution;
            // TODO: In theory everything else makes a copy so we could cheat
            // here and save memory.
            contributions[offsetIndex] = contribution.copy();
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, xorLength, monomials, contributions);
    }

    public static SimplePolynomialFunction AND(int andLength) {
        int inputLength = andLength >>> 1;
        Monomial[] monomials = new Monomial[inputLength];
        BitVector[] contributions = new BitVector[inputLength];

        for (int i = 0; i < inputLength; ++i) {
            int offsetIndex = i + inputLength;
            monomials[i] = Monomial.linearMonomial(andLength, i).inplaceProd(
                    Monomial.linearMonomial(andLength, offsetIndex));
            BitVector contribution = new BitVector(andLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(andLength, andLength, monomials, contributions);
    }

    public static SimplePolynomialFunction BINARY_AND(int andLength) {
        int inputLength = andLength << 1;
        Monomial[] monomials = new Monomial[andLength];
        BitVector[] contributions = new BitVector[andLength];

        for (int i = 0; i < andLength; ++i) {
            int offsetIndex = i + andLength;
            monomials[i] = Monomial.linearMonomial(inputLength, i).inplaceProd(
                    Monomial.linearMonomial(inputLength, offsetIndex));
            BitVector contribution = new BitVector(andLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, andLength, monomials, contributions);
    }

    public static SimplePolynomialFunction LSH(int inputLength, int shiftLength) {
        Monomial[] monomials = new Monomial[inputLength - shiftLength];
        BitVector[] contributions = new BitVector[inputLength - shiftLength];
        int upperLimit = inputLength - shiftLength;
        for (int i = 0; i < upperLimit; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(i + shiftLength);
            contributions[i] = contribution;
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    public static SimplePolynomialFunction NEG(int inputLength) {
        Monomial[] monomials = new Monomial[inputLength + 1];
        BitVector[] contributions = new BitVector[inputLength + 1];
        for (int i = 0; i < ( monomials.length - 1 ); ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        monomials[inputLength] = new Monomial(inputLength);
        contributions[inputLength] = new BitVector(inputLength);
        contributions[inputLength].not();
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    public static SimplePolynomialFunction RSH(int inputLength, int shiftLength) {
        Monomial[] monomials = new Monomial[inputLength - shiftLength];
        BitVector[] contributions = new BitVector[inputLength - shiftLength];
        int index;
        for (int i = shiftLength; i < inputLength; ++i) {
            index = i - shiftLength;
            monomials[index] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(index);
            contributions[index] = contribution;
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    public static SimplePolynomialFunction HALF_ADDER(int length) {
        return PolynomialFunctions.concatenate(PolynomialFunctions.BINARY_XOR(length),
                PolynomialFunctions.LSH(length, 1).compose(PolynomialFunctions.BINARY_AND(length)));
    }

    public static PolynomialFunction ADDER(int length) {
        return ADDER(length, BINARY_XOR(length), LSH(length, 1).compose(BINARY_AND(length)));
    }

    // TODO: Finish adder generation.
    public static PolynomialFunction ADDER(int length, SimplePolynomialFunction xor, SimplePolynomialFunction carry) {
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2();

        /*
         * Actually building out the algebraic representation of an adder is prohibitively expensive. Initialization:
         * carry = ( x & y ) << 1; 256 -> 128 current = x + y; 256 -> 128
         */

        SimplePolynomialFunction halfAdder = PolynomialFunctions.concatenate(xor, carry);

        for (int i = 0; i < length - 1; ++i) {
            cpf.prefix(halfAdder);
        }
        cpf.suffix(xor);
        return cpf;
    }

    /**
     * Static factory method for building identity functions that extract the upper half of the input. These are useful
     * when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated. f(x,y) = x+y =
     * lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param monomialOrder
     *            The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction upperBinaryIdentity(int monomialOrder) {
        int baseIndex = monomialOrder >>> 1;
        Monomial[] monomials = new Monomial[baseIndex];
        BitVector[] contributions = new BitVector[baseIndex];

        for (int i = baseIndex; i < monomialOrder; ++i) {
            int adjustedIndex = i - baseIndex;
            monomials[adjustedIndex] = Monomial.linearMonomial(monomialOrder, i);
            BitVector contribution = new BitVector(monomialOrder);
            contribution.set(i);
            contributions[adjustedIndex] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(monomialOrder, monomialOrder, monomials, contributions);
    }

    /**
     * Useful for constructing an Gf(2)^2n -&gt; GF(2)^n function that takes the upper identity.
     * 
     * @param inputLength
     * @return A polynomial function the takes the upper inputLength / 2 bits and outputs them without modifying them
     */
    public static SimplePolynomialFunction upperIdentity(int inputLength) {
        int baseIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[baseIndex];
        BitVector[] contributions = new BitVector[baseIndex];

        for (int i = baseIndex; i < inputLength; ++i) {
            int adjustedIndex = i - baseIndex;
            monomials[adjustedIndex] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(baseIndex);
            contribution.set(i - baseIndex);
            contributions[adjustedIndex] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, baseIndex, monomials, contributions);
    }

    /**
     * Static factory method for building identity functions that extract the lower half of the input. These are useful
     * when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated. f(x,y) = x+y =
     * lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param inputLength
     *            The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction lowerBinaryIdentity(int inputLength) {
        int maxIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[maxIndex];
        BitVector[] contributions = new BitVector[maxIndex];
        for (int i = 0; i < maxIndex; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        // No need to use
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    /**
     * Useful for constructing an Gf(2)^2n -&gt; GF(2)^n function that takes the lower identity.
     * 
     * @param inputLength
     * @return A polynomial function the takes the lower inputLength / 2 bits and outputs them without modifying them
     */
    public static SimplePolynomialFunction lowerIdentity(int inputLength) {
        int maxIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[maxIndex];
        BitVector[] contributions = new BitVector[maxIndex];
        for (int i = 0; i < maxIndex; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(maxIndex);
            contribution.set(i);
            contributions[i] = contribution;
        }

        // No need to use
        return new OptimizedPolynomialFunctionGF2(inputLength, maxIndex, monomials, contributions);
    }

    /**
     * Generates random polynomial functions containing a maximum of 16 terms of max order 3.
     * 
     * @param inputLen
     *            The number of input bits to the polynomial function.
     * @param outputLen
     *            The number of output bits to the polynomial function.
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction(int inputLen, int outputLen) {
        return randomFunction(inputLen, outputLen, 16, 3);
    }

    /**
     * Generates random polynomial functions.
     * 
     * @param inputLength
     *            Number of input bits to the polynomial function.
     * @param outputLength
     *            Number of output bits to the polynomial function.
     * @param numTerms
     * @param maxOrder
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction(int inputLength, int outputLength, int numTerms, int maxOrder) {
        Map<Monomial, BitVector> contributionMap = Maps.newHashMap();
        for (int i = 0; i < outputLength; ++i) {
            Set<Monomial> monomials = Sets.newHashSet();
            while (monomials.size() < numTerms) {
                Monomial monomial = Monomial.randomMonomial(inputLength, maxOrder);
                if (monomials.add(monomial)) {
                    BitVector contribution = contributionMap.get(monomial);
                    if (contribution == null) {
                        contribution = new BitVector(outputLength);
                        contributionMap.put(monomial, contribution);
                    }
                    contribution.set(i);
                }
            }

        }

        return fromMonomialContributionMap(inputLength, outputLength, contributionMap);
    }

    /**
     * Generates dense random multivariate quadratic functions.
     * 
     * @param inputLength
     *            Number of input bits to the polynomial function.
     * @param outputLength
     *            Number of output bits to the polynomial function.
     * @return a random multivariate quadratic polynomial function over GF(2)
     */
    public static SimplePolynomialFunction denseRandomMultivariateQuadratic(int inputLength, int outputLength) {
        int maxIndex = 1 + ( ( inputLength * ( inputLength + 1 ) ) >>> 1 );
        Monomial[] monomials = new Monomial[maxIndex];
        BitVector[] contributions = new BitVector[maxIndex];

        int flatIndex = 0;
        monomials[flatIndex] = Monomial.constantMonomial(inputLength);
        contributions[flatIndex] = BitUtils.randomVector(outputLength);
        for (int j = 0; j < inputLength; ++j) {
            for (int k = j; k < inputLength; ++k) {
                /*
                 * Converts cartesian index j,k to linear index as a function of j and k j*(inputLength-1) accounts for
                 * k starting at j and that j rows have already been assigned ((j*(j-1))>>>1) tracks how many indices
                 * have been skipped in the triangle above the diagonal. k controls the assignment
                 */
                flatIndex = 1 + j * ( inputLength - 1 ) - ( ( j * ( j - 1 ) ) >>> 1 ) + k;
                monomials[flatIndex] = new Monomial(inputLength).chainSet(j).chainSet(k);
                contributions[flatIndex] = BitUtils.randomVector(outputLength);
            }
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
    }

    /**
     * Constructs an array of random multivariate quadratic functions.
     * 
     * @param inputLength
     *            Number of input bits to the polynomial function.
     * @param outputLength
     *            Number of output bits to the polynomial function.
     * @param count
     *            The number of functions to construct in the array.
     * @return
     */
    public static SimplePolynomialFunction[] arrayOfRandomMultivariateQuadratics(int inputLength, int outputLength,
            int count) {
        SimplePolynomialFunction[] functions = new SimplePolynomialFunction[count];

        for (int i = 0; i < functions.length; ++i) {
            functions[i] = denseRandomMultivariateQuadratic(inputLength, outputLength);
        }

        return functions;
    }

    /**
     * Static factory method for many-to-one functions that mix the upper and lower half of the inputs.
     * 
     * @param inputLength
     * @return Returns random linear combination of the upper half and lower half of the inputs.
     */
    public static SimplePolynomialFunction randomManyToOneLinearCombination(int inputLength) {
        return linearCombination(EnhancedBitMatrix.randomInvertibleMatrix(inputLength),
                EnhancedBitMatrix.randomInvertibleMatrix(inputLength));
    }

    public static SimplePolynomialFunction fromMonomialContributionMap(int inputLength, int outputLength,
            Map<Monomial, BitVector> monomialContributionsMap) {
        OptimizedPolynomialFunctionGF2.removeNilContributions(monomialContributionsMap);
        Monomial[] newMonomials = new Monomial[monomialContributionsMap.size()];
        BitVector[] newContributions = new BitVector[monomialContributionsMap.size()];
        int index = 0;
        for (Entry<Monomial, BitVector> entry : monomialContributionsMap.entrySet()) {
            BitVector contribution = entry.getValue();
            newMonomials[index] = entry.getKey();
            newContributions[index] = contribution;
            ++index;
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, outputLength, newMonomials, newContributions);
    }

    /**
     * Builds a new function by concatenating the output of the input functions. It does not change the length of the
     * input and the new outputs will be the same order as they are passed in.
     * 
     * @param first
     *            function whose outputs will become the first set of output of the new function
     * @param second
     *            function whose outputs will become the first set of output of the new function
     * @return a function that maps inputs to outputs consisting of the concatenated output of the first and second
     *         functions.
     */
    public static SimplePolynomialFunction concatenate(SimplePolynomialFunction first, SimplePolynomialFunction second) {
        Preconditions.checkArgument(first.getInputLength() == second.getInputLength(),
                "Functions being composed must have compatible monomial lengths");
        int lhsOutputLength = first.getOutputLength();
        int rhsOutputLength = second.getOutputLength();
        int combinedOutputLength = lhsOutputLength + rhsOutputLength;
        Map<Monomial, BitVector> lhsMap = FunctionUtils.mapViewFromMonomialsAndContributions(first.getMonomials(),
                first.getContributions());
        Map<Monomial, BitVector> rhsMap = FunctionUtils.mapViewFromMonomialsAndContributions(second.getMonomials(),
                second.getContributions());
        Map<Monomial, BitVector> monomialContributionMap = Maps.newHashMap();
        BitVector lhsZero = new BitVector(lhsOutputLength);
        BitVector rhsZero = new BitVector(rhsOutputLength);

        Set<Monomial> monomials = Sets.union(lhsMap.keySet(), rhsMap.keySet());
        for (Monomial monomial : monomials) {
            BitVector lhsContribution = Objects.firstNonNull(lhsMap.get(monomial), lhsZero);
            BitVector rhsContribution = Objects.firstNonNull(rhsMap.get(monomial), rhsZero);

            monomialContributionMap.put(monomial, FunctionUtils.concatenate(lhsContribution, rhsContribution));
        }

        return fromMonomialContributionMap(first.getInputLength(), combinedOutputLength, monomialContributionMap);

    }

    /**
     * This is a potentially unsafe method for generating liner combinations, where if the linear combination is known,
     * it maybe possible to find subspace kernels that reaveal information about the remaining variables. It is fine for
     * the purposes for generating the hash function for indexing.
     * 
     * @param inputLength
     * @param outputLength
     * @return
     */
    public static SimplePolynomialFunction unsafeRandomManyToOneLinearCombination(int inputLength, int outputLength) {
        return EnhancedBitMatrix.randomMatrix(outputLength, inputLength).multiply(
                PolynomialFunctions.identity(inputLength));
    }

    public static SimplePolynomialFunction linearCombination(EnhancedBitMatrix c1, EnhancedBitMatrix c2) {
        return c1.multiply(lowerIdentity(c1.cols() << 1)).xor(c2.multiply(upperIdentity(c2.cols() << 1)));

    }

    public static Pair<SimplePolynomialFunction, SimplePolynomialFunction> randomlyPartitionMVQ(
            SimplePolynomialFunction f) {
        Preconditions.checkArgument(f.getMaximumMonomialOrder() == 2);

        SimplePolynomialFunction g = denseRandomMultivariateQuadratic(f.getInputLength(), f.getOutputLength());
        SimplePolynomialFunction h = f.xor(g);

        return Pair.of(g, h);
    }

    public static SimplePolynomialFunction identity(int monomialCount) {
        Monomial[] monomials = new Monomial[monomialCount];
        BitVector[] contributions = new BitVector[monomialCount];

        for (int i = 0; i < monomialCount; ++i) {
            monomials[i] = Monomial.linearMonomial(monomialCount, i);
            BitVector contribution = new BitVector(monomialCount);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(monomialCount, monomialCount, monomials, contributions);
    }

    /**
     * Builds a non-linear sequence of functions that has the same output as another given sequence of functions, but
     * with a unique non-linear partitioning applied at each stage. *
     * 
     * @param inner
     *            The initial internal basis for using in the partition of the first function.
     * @param functions
     *            The sequence of functions to convert into a pipeline of partitioned functions
     * @return a sequences of functions that evaluations to the same {@code functions}.
     */

    public static Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> buildNonlinearPipeline(
            SimplePolynomialFunction inner, SimplePolynomialFunction[] functions) {
        Preconditions.checkArgument(functions.length > 0, "Pipeline must contain at least one function.");
        SimplePolynomialFunction[] pipeline = new SimplePolynomialFunction[functions.length];
        SimplePolynomialFunction innerCombination = inner;
        /*
         * functions = h_i( s ) pair = <h[0]_i,h[1]_i> satisfying the recurrence relationship h_i[s] = c1_i*h[0]_{i}(
         * c1_{i-1}*h[0]_{i-1} + c_2*h[1]_{i-1} ) + c_2*h[1]_{i-1}( c1_{i-1}*h[0]_{i-1} + c_2*h[1]_{i-1} )
         */
        for (int i = 0; i < pipeline.length; ++i) {
            PolynomialFunctionPipelineStage stage = PolynomialFunctionPipelineStage.build(functions[i],
                    innerCombination);
            // try {
            /*
             * Prepare the function so that partitioned outputs are passed to the next function in the chain the the
             * inner compose applies the appropriate combination. An unstated assumpt here is that linearCombination
             * treats the first half as corresponding to c1 and the second half as corresponding to c2. Unit tests
             * should catch any violations of that.
             */
            pipeline[i] = stage.getStep();
            innerCombination = stage.getCombination();
        }

        return Pair.of(innerCombination, pipeline);
    }

    public static Function<SimplePolynomialFunction, SimplePolynomialFunction> getComposer(
            final SimplePolynomialFunction inner) {
        return new Function<SimplePolynomialFunction, SimplePolynomialFunction>() {
            @Override
            public SimplePolynomialFunction apply(SimplePolynomialFunction input) {
                Pair<SimplePolynomialFunction, SimplePolynomialFunction> pair = PolynomialFunctions
                        .randomlyPartitionMVQ(input);
                return PolynomialFunctions.concatenate(pair.getLeft().compose(inner), pair.getRight().compose(inner));
            }
        };
    }

    public static Map<Monomial, BitVector> mapCopyFromMonomialsAndContributions(Monomial[] monomials,
            BitVector[] contributions) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize(monomials.length);
        for (int i = 0; i < monomials.length; ++i) {
            result.put(monomials[i].clone(), contributions[i].copy());
        }
        return result;
    }

    public static SimplePolynomialFunction unmarshalSimplePolynomialFunction(String input) {
        byte[] decoded = Base64.decodeBase64(input.getBytes());
        ByteBuffer buf = ByteBuffer.wrap(decoded);
        int inputLength = buf.getInt();
        int outputLength = buf.getInt();
        int monomialLength = buf.getInt();
        int contributionLength = buf.getInt();

        LongBuffer longBuffer = buf.asLongBuffer();

        Monomial[] monomials = new Monomial[monomialLength];
        for (int i = 0; i < monomials.length; i++) {
            long[] monomialLongs = new long[inputLength >> 6];
            longBuffer.get(monomialLongs);
            monomials[i] = new Monomial(monomialLongs, inputLength);
        }

        BitVector[] contributions = new BitVector[contributionLength];
        for (int i = 0; i < contributions.length; i++) {
            long[] contributionLongs = new long[outputLength >> 6];
            longBuffer.get(contributionLongs);
            contributions[i] = new BitVector(contributionLongs, outputLength);
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
    }

    public static String marshalSimplePolynomialFunction(SimplePolynomialFunction input) {
        Monomial[] monomials = input.getMonomials();
        BitVector[] contributions = input.getContributions();

        int inputLength = input.getInputLength();
        int outputLength = input.getOutputLength();

        long[] monomialData = new long[monomials.length * ( inputLength >> 6 )];
        LongBuffer monomialBuffer = LongBuffer.wrap(monomialData);
        for (int i = 0; i < monomials.length; i++) {
            monomialBuffer.put(monomials[i].elements());
        }

        long[] contributionData = new long[contributions.length * ( outputLength >> 6 )];
        LongBuffer contributionBuffer = LongBuffer.wrap(contributionData);
        for (int i = 0; i < contributions.length; i++) {
            contributionBuffer.put(contributions[i].elements());
        }

        byte[] target = new byte[( monomialData.length << 3 ) + ( contributionData.length << 3 ) + INTEGER_BYTES_X4];
        ByteBuffer buf = ByteBuffer.wrap(target);
        buf.putInt(inputLength);
        buf.putInt(outputLength);
        buf.putInt(monomials.length);
        buf.putInt(contributions.length);

        LongBuffer longBuffer = buf.asLongBuffer();
        longBuffer.put(monomialData);
        longBuffer.put(contributionData);

        return new String(codec.encode(target));
    }
}
