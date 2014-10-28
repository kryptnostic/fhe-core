package com.kryptnostic.multivariate.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.PolynomialFunctionPipelineStage;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.BasePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;

public class SimplePolynomialFunctions {

    /**
     * Uses the output map to indicate the indices of the output bits of the lhs function in the new function.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction concatenateInputsAndOutputs(
            SimplePolynomialFunction lhs,
            SimplePolynomialFunction rhs ) {
        if ( lhs.isParameterized() || rhs.isParameterized() ) {
            try {
                return ParameterizedPolynomialFunctions.concatenateInputsAndOutputs( lhs, rhs );
            } catch ( Exception e ) {
                logger.error( "Exception in parameterized function concatenation." );
            }
        }
        Pair<int[], int[]> inputMaps = getSplitMap( lhs.getInputLength(), rhs.getInputLength() );
        Pair<int[], int[]> outputMaps = getSplitMap( lhs.getOutputLength(), rhs.getOutputLength() );

        return interleaveFunctions(
                lhs,
                rhs,
                inputMaps.getLeft(),
                inputMaps.getRight(),
                outputMaps.getLeft(),
                outputMaps.getRight() );
    }

    /**
     * Generates a tuple of mappings the first array maps the lhLength indices, and the second array maps to the
     * rhLength indices.
     * 
     * @return Pair of lhMap and rhMap
     */
    public static Pair<int[], int[]> getSplitMap( int lhLength, int rhLength ) {
        int totalLength = lhLength + rhLength;
        int[] lhMap = new int[ lhLength ];
        int[] rhMap = new int[ rhLength ];
        for ( int i = 0; i < totalLength; i++ ) {
            if ( i < lhLength ) {
                lhMap[ i ] = i;
            } else {
                rhMap[ i - lhLength ] = i;
            }
        }
        return Pair.of( lhMap, rhMap );
    }

    /**
     * Interleave two functions using the arrays as maps for the new monomial and contribution orderings.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction interleaveFunctions(
            SimplePolynomialFunction lhs,
            SimplePolynomialFunction rhs,
            int[] lhsInputMap,
            int[] rhsInputMap,
            int[] lhsOutputMap,
            int[] rhsOutputMap ) {
        int combinedInputLength = lhs.getInputLength() + rhs.getInputLength();
        int combinedOutputLength = lhs.getOutputLength() + rhs.getOutputLength();
        int numTerms = lhs.getMonomials().length + rhs.getMonomials().length;
        Monomial[] newMonomials = new Monomial[ numTerms ];
        BitVector[] newContributions = new BitVector[ numTerms ];

        mapAndAddTerms(
                newMonomials,
                newContributions,
                combinedInputLength,
                combinedOutputLength,
                0,
                lhs,
                lhsInputMap,
                lhsOutputMap );
        mapAndAddTerms(
                newMonomials,
                newContributions,
                combinedInputLength,
                combinedOutputLength,
                lhs.getMonomials().length,
                rhs,
                rhsInputMap,
                rhsOutputMap );

        return new OptimizedPolynomialFunctionGF2(
                combinedInputLength,
                combinedOutputLength,
                newMonomials,
                newContributions );
    }

    /**
     * Map the terms of inner onto larger arrays of monomials and contributions.
     */
    private static void mapAndAddTerms(
            Monomial[] newMonomials,
            BitVector[] newContributions,
            int combinedInputLength,
            int combinedOutputLength,
            int baseIndex,
            SimplePolynomialFunction inner,
            int[] inputMap,
            int[] outputMap ) {
        Preconditions.checkArgument(
                inner.getInputLength() <= combinedInputLength,
                "Inner input length cannot be larger than new input length." );
        Preconditions.checkArgument(
                inner.getOutputLength() <= combinedOutputLength,
                "Inner output length cannot be larger than new output length." );
        Preconditions.checkArgument(
                inner.getMonomials().length <= newMonomials.length,
                "Array of old monomials cannot be larger than new monomial array." );

        Monomial[] monomials = inner.getMonomials();
        for ( int i = 0; i < monomials.length; i++ ) {
            BitVector backingVector = BitVectors.extendAndOrder( monomials[ i ], inputMap, combinedInputLength );
            Monomial newMonomial = new Monomial( combinedInputLength );
            newMonomial.xor( backingVector );
            BitVector newContribution = BitVectors.extendAndOrder(
                    inner.getContributions()[ i ],
                    outputMap,
                    combinedOutputLength );

            newMonomials[ baseIndex + i ] = newMonomial;
            newContributions[ baseIndex + i ] = newContribution;
        }
    }

    /**
     * Generates a {@link SimplePolynomialFunction} from the string representation.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction fromString( int monomialSize, String polynomialString ) {
        List<String> lines = Splitter.on( "\n" ).omitEmptyStrings().trimResults().splitToList( polynomialString );
        int row = 0;
        Map<Monomial, BitVector> monomialContributionsMap = Maps.newHashMap();
        for ( String line : lines ) {
            Iterable<String> monomials = Splitter.on( "+" ).trimResults().omitEmptyStrings().split( line );

            for ( String monomialString : monomials ) {
                Monomial m = Monomial.fromString( monomialSize, monomialString );
                BitVector contribution = monomialContributionsMap.get( m );
                if ( contribution == null ) {
                    contribution = new BitVector( lines.size() );
                    monomialContributionsMap.put( m, contribution );
                }
                contribution.set( row );
            }
            ++row;
        }
        return fromMonomialContributionMap( monomialSize, lines.size(), monomialContributionsMap );
    }

    /**
     * Static factory method for building identity functions that extract the upper half of the input. These are useful
     * when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated. f(x,y) = x+y =
     * lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param monomialOrder The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction upperBinaryIdentity( int monomialOrder ) {
        int baseIndex = monomialOrder >>> 1;
        Monomial[] monomials = new Monomial[ baseIndex ];
        BitVector[] contributions = new BitVector[ baseIndex ];

        for ( int i = baseIndex; i < monomialOrder; ++i ) {
            int adjustedIndex = i - baseIndex;
            monomials[ adjustedIndex ] = Monomial.linearMonomial( monomialOrder, i );
            BitVector contribution = new BitVector( monomialOrder );
            contribution.set( i );
            contributions[ adjustedIndex ] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2( monomialOrder, monomialOrder, monomials, contributions );
    }

    /**
     * Useful for constructing an Gf(2)^2n -&gt; GF(2)^n function that takes the upper identity.
     * 
     * @param inputLength
     * @return A polynomial function the takes the upper inputLength / 2 bits and outputs them without modifying them
     */
    public static SimplePolynomialFunction upperIdentity( int inputLength ) {
        int baseIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[ baseIndex ];
        BitVector[] contributions = new BitVector[ baseIndex ];

        for ( int i = baseIndex; i < inputLength; ++i ) {
            int adjustedIndex = i - baseIndex;
            monomials[ adjustedIndex ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( baseIndex );
            contribution.set( i - baseIndex );
            contributions[ adjustedIndex ] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2( inputLength, baseIndex, monomials, contributions );
    }

    /**
     * Static factory method for building identity functions that extract the lower half of the input. These are useful
     * when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated. f(x,y) = x+y =
     * lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param inputLength The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction lowerBinaryIdentity( int inputLength ) {
        int maxIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[ maxIndex ];
        BitVector[] contributions = new BitVector[ maxIndex ];
        for ( int i = 0; i < maxIndex; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }

        // No need to use
        return new OptimizedPolynomialFunctionGF2( inputLength, inputLength, monomials, contributions );
    }

    /**
     * Useful for constructing an Gf(2)^2n -&gt; GF(2)^n function that takes the lower identity.
     * 
     * @param inputLength
     * @return A polynomial function the takes the lower inputLength / 2 bits and outputs them without modifying them
     */
    public static SimplePolynomialFunction lowerIdentity( int inputLength ) {
        int maxIndex = inputLength >>> 1;
        Monomial[] monomials = new Monomial[ maxIndex ];
        BitVector[] contributions = new BitVector[ maxIndex ];
        for ( int i = 0; i < maxIndex; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( maxIndex );
            contribution.set( i );
            contributions[ i ] = contribution;
        }

        // No need to use
        return new OptimizedPolynomialFunctionGF2( inputLength, maxIndex, monomials, contributions );
    }

    public static SimplePolynomialFunction lowerTruncatingIdentity( int inputLength, int outputLength ) {
        Preconditions.checkArgument(
                inputLength > outputLength,
                "Output length must be less than input length to truncate." );
        Preconditions.checkArgument( outputLength >= 0, "Output length cannot be less than 0." );
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        for ( int i = 0; i < inputLength; i++ ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( outputLength );
            if ( i < outputLength ) {
                contribution.set( i );
            }
            contributions[ i ] = contribution;
        }
        return new BasePolynomialFunction( inputLength, outputLength, monomials, contributions );
    }

    /**
     * @return function that preserves the msb
     */
    public static SimplePolynomialFunction upperTruncatingIdentity( int inputLength, int outputLength ) {
        Preconditions.checkArgument(
                inputLength > outputLength,
                "Output length must be less than input length to truncate." );
        Preconditions.checkArgument( outputLength >= 0, "Output length cannot be less than 0." );
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        int offset = inputLength - outputLength;
        for ( int i = 0; i < inputLength; i++ ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( outputLength );
            if ( i >= offset ) {
                contribution.set( i - offset );
            }
            contributions[ i ] = contribution;
        }
        return new BasePolynomialFunction( inputLength, outputLength, monomials, contributions );
    }

    public static SimplePolynomialFunction identityRange( int start, int end, int inputLength, int outputLength ) {
        int len = end - start;
        Monomial[] monomials = new Monomial[ len ];
        BitVector[] contributions = new BitVector[ len ];

        for ( int i = 0; i < len; ++i ) {
            // int adjustedIndex = i + start;
            monomials[ i ] = Monomial.linearMonomial( inputLength, i );
            BitVector contribution = new BitVector( outputLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2( inputLength, outputLength, monomials, contributions );
    }

    /**
     * Generates random polynomial functions containing a maximum of 16 terms of max order 3.
     * 
     * @param inputLen The number of input bits to the polynomial function.
     * @param outputLen The number of output bits to the polynomial function.
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction( int inputLen, int outputLen ) {
        return randomFunction( inputLen, outputLen, 16, 3 );
    }

    /**
     * Generates random polynomial functions containing a maximum of 10 terms of max order 2.
     * 
     * @return SimplePolynomialFunction
     */
    public static SimplePolynomialFunction lightRandomFunction( int inputLen, int outputLen ) {
        return randomFunction( inputLen, outputLen, 10, 2 );
    }

    /**
     * Generates random polynomial functions.
     * 
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @param numTerms
     * @param maxOrder
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction( int inputLength, int outputLength, int numTerms, int maxOrder ) {
        Map<Monomial, BitVector> contributionMap = Maps.newHashMap();
        for ( int i = 0; i < outputLength; ++i ) {
            Set<Monomial> monomials = Sets.newHashSet();
            while ( monomials.size() < numTerms ) {
                Monomial monomial = Monomial.randomMonomial( inputLength, maxOrder );
                if ( monomials.add( monomial ) ) {
                    BitVector contribution = contributionMap.get( monomial );
                    if ( contribution == null ) {
                        contribution = new BitVector( outputLength );
                        contributionMap.put( monomial, contribution );
                    }
                    contribution.set( i );
                }
            }

        }

        return fromMonomialContributionMap( inputLength, outputLength, contributionMap );
    }

    /**
     * Generates dense random multivariate quadratic functions.
     * 
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @return a random multivariate quadratic polynomial function over GF(2)
     */
    public static SimplePolynomialFunction denseRandomMultivariateQuadratic( int inputLength, int outputLength ) {
        int maxIndex = 1 + ( ( inputLength * ( inputLength + 1 ) ) >>> 1 );
        Monomial[] monomials = new Monomial[ maxIndex ];
        BitVector[] contributions = new BitVector[ maxIndex ];

        int flatIndex = 0;
        monomials[ flatIndex ] = Monomial.constantMonomial( inputLength );
        contributions[ flatIndex ] = BitVectors.randomVector( outputLength );
        for ( int j = 0; j < inputLength; ++j ) {
            for ( int k = j; k < inputLength; ++k ) {
                /*
                 * Converts cartesian index j,k to linear index as a function of j and k j*(inputLength-1) accounts for
                 * k starting at j and that j rows have already been assigned ((j*(j-1))>>>1) tracks how many indices
                 * have been skipped in the triangle above the diagonal. k controls the assignment
                 */
                flatIndex = 1 + j * ( inputLength - 1 ) - ( ( j * ( j - 1 ) ) >>> 1 ) + k;
                monomials[ flatIndex ] = new Monomial( inputLength ).chainSet( j ).chainSet( k );
                contributions[ flatIndex ] = BitVectors.randomVector( outputLength );
            }
        }
        return new OptimizedPolynomialFunctionGF2( inputLength, outputLength, monomials, contributions );
    }

    /**
     * Constructs an array of random multivariate quadratic functions.
     * 
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @param count The number of functions to construct in the array.
     * @return
     */
    public static SimplePolynomialFunction[] arrayOfRandomMultivariateQuadratics(
            int inputLength,
            int outputLength,
            int count ) {
        SimplePolynomialFunction[] functions = new SimplePolynomialFunction[ count ];

        for ( int i = 0; i < functions.length; ++i ) {
            functions[ i ] = denseRandomMultivariateQuadratic( inputLength, outputLength );
        }

        return functions;
    }

    /**
     * Static factory method for many-to-one functions that mix the upper and lower half of the inputs.
     * 
     * @param inputLength
     * @return Returns random linear combination of the upper half and lower half of the inputs.
     */
    public static SimplePolynomialFunction randomManyToOneLinearCombination( int inputLength ) {
        return linearCombination(
                EnhancedBitMatrix.randomInvertibleMatrix( inputLength ),
                EnhancedBitMatrix.randomInvertibleMatrix( inputLength ) );
    }

    public static SimplePolynomialFunction fromMonomialContributionMap(
            int inputLength,
            int outputLength,
            Map<Monomial, BitVector> monomialContributionsMap ) {
        OptimizedPolynomialFunctionGF2.removeNilContributions( monomialContributionsMap );
        Monomial[] newMonomials = new Monomial[ monomialContributionsMap.size() ];
        BitVector[] newContributions = new BitVector[ monomialContributionsMap.size() ];
        int index = 0;
        for ( Entry<Monomial, BitVector> entry : monomialContributionsMap.entrySet() ) {
            BitVector contribution = entry.getValue();
            newMonomials[ index ] = entry.getKey();
            newContributions[ index ] = contribution;
            ++index;
        }
        return new OptimizedPolynomialFunctionGF2( inputLength, outputLength, newMonomials, newContributions );
    }

    /**
     * Builds a new function by concatenating the output of the input functions. It does not change the length of the
     * input and the new outputs will be the same order as they are passed in.
     * 
     * @param first function whose outputs will become the first set of output of the new function
     * @param second function whose outputs will become the first set of output of the new function
     * @return a function that maps inputs to outputs consisting of the concatenated output of the first and second
     *         functions.
     */
    public static SimplePolynomialFunction concatenate( SimplePolynomialFunction first, SimplePolynomialFunction second ) {
        Preconditions.checkArgument(
                first.getInputLength() == second.getInputLength(),
                "Functions being composed must have compatible monomial lengths" );
        int lhsOutputLength = first.getOutputLength();
        int rhsOutputLength = second.getOutputLength();
        int combinedOutputLength = lhsOutputLength + rhsOutputLength;
        Map<Monomial, BitVector> lhsMap = FunctionUtils.mapViewFromMonomialsAndContributions(
                first.getMonomials(),
                first.getContributions() );
        Map<Monomial, BitVector> rhsMap = FunctionUtils.mapViewFromMonomialsAndContributions(
                second.getMonomials(),
                second.getContributions() );
        Map<Monomial, BitVector> monomialContributionMap = Maps.newHashMap();
        BitVector lhsZero = new BitVector( lhsOutputLength );
        BitVector rhsZero = new BitVector( rhsOutputLength );

        Set<Monomial> monomials = Sets.union( lhsMap.keySet(), rhsMap.keySet() );
        for ( Monomial monomial : monomials ) {
            BitVector lhsContribution = Objects.firstNonNull( lhsMap.get( monomial ), lhsZero );
            BitVector rhsContribution = Objects.firstNonNull( rhsMap.get( monomial ), rhsZero );

            monomialContributionMap.put( monomial, FunctionUtils.concatenate( lhsContribution, rhsContribution ) );
        }

        return fromMonomialContributionMap( first.getInputLength(), combinedOutputLength, monomialContributionMap );

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
    public static SimplePolynomialFunction unsafeRandomManyToOneLinearCombination( int inputLength, int outputLength ) {
        return EnhancedBitMatrix.randomMatrix( outputLength, inputLength ).multiply( identity( inputLength ) );
    }

    public static SimplePolynomialFunction linearCombination( EnhancedBitMatrix c1, EnhancedBitMatrix c2 ) {
        return c1.multiply( lowerIdentity( c1.cols() << 1 ) ).xor( c2.multiply( upperIdentity( c2.cols() << 1 ) ) );

    }

    public static Pair<SimplePolynomialFunction, SimplePolynomialFunction> randomlyPartitionMVQ(
            SimplePolynomialFunction f ) {
        Preconditions.checkArgument( f.getMaximumMonomialOrder() == 2 );

        SimplePolynomialFunction g = denseRandomMultivariateQuadratic( f.getInputLength(), f.getOutputLength() );
        SimplePolynomialFunction h = f.xor( g );

        return Pair.of( g, h );
    }

    public static SimplePolynomialFunction identity( int monomialCount ) {
        Monomial[] monomials = new Monomial[ monomialCount ];
        BitVector[] contributions = new BitVector[ monomialCount ];

        for ( int i = 0; i < monomialCount; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( monomialCount, i );
            BitVector contribution = new BitVector( monomialCount );
            contribution.set( i );
            contributions[ i ] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2( monomialCount, monomialCount, monomials, contributions );
    }

    /**
     * Builds a non-linear sequence of functions that has the same output as another given sequence of functions, but
     * with a unique non-linear partitioning applied at each stage. *
     * 
     * @param inner The initial internal basis for using in the partition of the first function.
     * @param functions The sequence of functions to convert into a pipeline of partitioned functions
     * @return a sequences of functions that evaluations to the same {@code functions}.
     */

    public static Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> buildNonlinearPipeline(
            SimplePolynomialFunction inner,
            SimplePolynomialFunction[] functions ) {
        Preconditions.checkArgument( functions.length > 0, "Pipeline must contain at least one function." );
        SimplePolynomialFunction[] pipeline = new SimplePolynomialFunction[ functions.length ];
        SimplePolynomialFunction innerCombination = inner;
        /*
         * functions = h_i( s ) pair = <h[0]_i,h[1]_i> satisfying the recurrence relationship h_i[s] = c1_i*h[0]_{i}(
         * c1_{i-1}*h[0]_{i-1} + c_2*h[1]_{i-1} ) + c_2*h[1]_{i-1}( c1_{i-1}*h[0]_{i-1} + c_2*h[1]_{i-1} )
         */
        for ( int i = 0; i < pipeline.length; ++i ) {
            PolynomialFunctionPipelineStage stage = PolynomialFunctionPipelineStage.build(
                    functions[ i ],
                    innerCombination );
            // try {
            /*
             * Prepare the function so that partitioned outputs are passed to the next function in the chain the the
             * inner compose applies the appropriate combination. An unstated assumpt here is that linearCombination
             * treats the first half as corresponding to c1 and the second half as corresponding to c2. Unit tests
             * should catch any violations of that.
             */
            pipeline[ i ] = stage.getStep();
            innerCombination = stage.getCombination();
        }

        return Pair.of( innerCombination, pipeline );
    }

    public static Function<SimplePolynomialFunction, SimplePolynomialFunction> getComposer(
            final SimplePolynomialFunction inner ) {
        return new Function<SimplePolynomialFunction, SimplePolynomialFunction>() {
            @Override
            public SimplePolynomialFunction apply( SimplePolynomialFunction input ) {
                Pair<SimplePolynomialFunction, SimplePolynomialFunction> pair = randomlyPartitionMVQ( input );
                return concatenate( pair.getLeft().compose( inner ), pair.getRight().compose( inner ) );
            }
        };
    }

    public static Map<Monomial, BitVector> mapCopyFromMonomialsAndContributions(
            Monomial[] monomials,
            BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for ( int i = 0; i < monomials.length; ++i ) {
            result.put( monomials[ i ].clone(), contributions[ i ].copy() );
        }
        return result;
    }

    private static final Logger logger = LoggerFactory.getLogger( SimplePolynomialFunctions.class );

    private SimplePolynomialFunctions() {}

}
