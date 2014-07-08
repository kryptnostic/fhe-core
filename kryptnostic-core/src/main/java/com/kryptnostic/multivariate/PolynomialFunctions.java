package com.kryptnostic.multivariate;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Utility and factory methods for PolynomialFunctions.
 * @author Matthew Tamayo-Rios
 */
public final class PolynomialFunctions {
    private PolynomialFunctions(){}
    
    public static SimplePolynomialFunction XOR( int xorLength ) {
        int inputLength = xorLength >>> 1;
        Monomial[] monomials = new Monomial[ xorLength ];
        BitVector[] contributions = new BitVector[ xorLength ];
        
        for( int i = 0 ; i < inputLength ; ++i ) {
            int offsetIndex = i + inputLength;
            monomials[ i ] = Monomial.linearMonomial( xorLength , i);
            monomials[ offsetIndex ] = Monomial.linearMonomial( xorLength , offsetIndex );
            BitVector contribution = new BitVector( xorLength );
            contribution.set( i );
            contributions[ i ] = contribution;
            contributions[ offsetIndex ] = contribution.copy(); //In theory everything else makes a copy so we could cheat here and save memory.
        }
        
        return new PolynomialFunctionGF2( xorLength , xorLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction BINARY_XOR( int xorLength ) {
        int inputLength = xorLength << 1;
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        
        for( int i = 0 ; i < xorLength ; ++i ) {
            int offsetIndex = i + xorLength;
            monomials[ i ] = Monomial.linearMonomial( inputLength , i);
            monomials[ offsetIndex ] = Monomial.linearMonomial( inputLength , offsetIndex );
            BitVector contribution = new BitVector( xorLength );
            contribution.set( i );
            contributions[ i ] = contribution;
            //TODO: In theory everything else makes a copy so we could cheat here and save memory.
            contributions[ offsetIndex ] = contribution.copy(); 
        }
        
        return new PolynomialFunctionGF2( inputLength , xorLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction AND( int andLength ) {
        int inputLength = andLength >>> 1;
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        
        for( int i = 0 ; i < inputLength ; ++i ) {
            int offsetIndex = i + inputLength;
            monomials[ i ] = Monomial
                                .linearMonomial( andLength , i)
                                .inplaceProd( Monomial.linearMonomial( andLength , offsetIndex ) );
            BitVector contribution = new BitVector( andLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        return new PolynomialFunctionGF2( andLength , andLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction BINARY_AND( int andLength ) {
        int inputLength = andLength << 1;
        Monomial[] monomials = new Monomial[ andLength ];
        BitVector[] contributions = new BitVector[ andLength ];
        
        for( int i = 0 ; i < andLength ; ++i ) {
            int offsetIndex = i + andLength;
            monomials[ i ] = Monomial
                                .linearMonomial( inputLength , i)
                                .inplaceProd( Monomial.linearMonomial( inputLength , offsetIndex ) );
            BitVector contribution = new BitVector( andLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        return new PolynomialFunctionGF2( inputLength , andLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction LSH( int inputLength , int shiftLength ) {
        Monomial[] monomials = new Monomial[ inputLength - shiftLength ];
        BitVector[] contributions = new BitVector[ inputLength - shiftLength ];
        int upperLimit = inputLength - shiftLength;
        for( int i = 0 ; i < upperLimit ; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( i + shiftLength );
            contributions[ i ] = contribution;
        }
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction NEG( int inputLength ) {
        Monomial[] monomials = new Monomial[ inputLength + 1];
        BitVector[] contributions = new BitVector[ inputLength + 1];
        for( int i = 0 ; i < ( monomials.length - 1) ; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        monomials[ inputLength ] = new Monomial( inputLength );
        contributions[ inputLength ] = new BitVector( inputLength );
        contributions[ inputLength ].not();
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction RSH( int inputLength , int shiftLength ) {
        Monomial[] monomials = new Monomial[ inputLength - shiftLength ];
        BitVector[] contributions = new BitVector[ inputLength - shiftLength ];
        int index;
        for( int i = shiftLength ; i < inputLength ; ++i ) {
            index = i - shiftLength;
            monomials[ index ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( index );
            contributions[ index ] = contribution;
        }
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction HALF_ADDER( int length ) {
        return PolynomialFunctions.concatenate(
                PolynomialFunctions.BINARY_XOR( length ) ,
                PolynomialFunctions
                    .LSH( length , 1 )
                    .compose( PolynomialFunctions.BINARY_AND( length ) ) 
                 ); 
    }
    
    public static PolynomialFunction ADDER( int length ) {
        return ADDER( length , BINARY_XOR( length ) , LSH( length , 1 ).compose( BINARY_AND( length ) )  );
    }
    
    //TODO: Finish adder generation.
    public static PolynomialFunction ADDER( int length , SimplePolynomialFunction xor , SimplePolynomialFunction carry ) {
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2();
        
        /*
         * Actually building out the algebraic representation of an adder is prohibitively expensive.
         * Initialization:
         * carry = ( x & y ) << 1; 256 -> 128
         * current = x + y; 256 -> 128
         */
        
        SimplePolynomialFunction halfAdder = PolynomialFunctions.concatenate( xor , carry );
        
        for( int i = 0 ; i < length - 1 ; ++i ) {
            cpf.prefix( halfAdder );
        }
        cpf.suffix( xor );
        return cpf;
    }


    /**
     * Static factory method for building identity functions that extract the upper half of the input.
     * These are useful when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated.  
     * f(x,y) = x+y = lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param monomialOrder The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction upperBinaryIdentity( int monomialOrder ) {
        int baseIndex = monomialOrder >>> 1;
        Monomial[] monomials = new Monomial[ baseIndex ];
        BitVector[] contributions = new BitVector[ baseIndex ];
        
        for( int i = baseIndex ; i < monomialOrder ; ++i ) {
            int adjustedIndex = i - baseIndex;
            monomials[ adjustedIndex ] = Monomial.linearMonomial( monomialOrder , i );
            BitVector contribution = new BitVector( monomialOrder );
            contribution.set( i );
            contributions[ adjustedIndex ] = contribution;
        }
        
        return new PolynomialFunctionGF2( monomialOrder , monomialOrder , monomials , contributions);
    }

    /**
     * Static factory method for building identity functions that extract the lower half of the input.
     * These are useful when dealing with functions that operate on two ciphertext inputs of equal length.
     * 
     * For example computing the XOR of two 64 bit ciphertexts, where the inputs are concatenated.  
     * f(x,y) = x+y = lowerBinaryIdentity( 128 ).xor( upperBinaryIdentity( 128 ).
     * 
     * @param monomialOrder The number of inputs bits for the SimplePolynomialFunction.
     * @return A SimplePolynomialFunction that passes through only the lower half of its input bits.
     */
    public static SimplePolynomialFunction lowerBinaryIdentity( int monomialOrder ) {
        int maxIndex = monomialOrder >>> 1;
        Monomial[] monomials = new Monomial[ maxIndex ];
        BitVector[] contributions = new BitVector[ maxIndex ];
        for( int i = 0 ; i < maxIndex ; ++i ) {
            monomials[i] = Monomial.linearMonomial( monomialOrder , i);
            BitVector contribution = new BitVector( monomialOrder );
            contribution.set( i );
            contributions[i] = contribution;
        }
        
        //No need to use 
        return new PolynomialFunctionGF2( monomialOrder , monomialOrder , monomials , contributions);
    }

    /**
     * Generates random polynomial functions containing a maximum of 16 terms 
     * of max order 3.
     * @param inputLen The number of input bits to the polynomial function.
     * @param outputLen The number of output bits to the polynomial function.
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction( int inputLen , int outputLen ) {
        return randomFunction( inputLen , outputLen , 16 , 3 );
    }
    
    /**
     * Generates random polynomial functions.
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @param numTerms 
     * @param maxOrder
     * @return a random polynomial function over GF(2)
     */
    public static SimplePolynomialFunction randomFunction( int inputLength , int outputLength , int numTerms , int maxOrder ) {
        Map<Monomial, BitVector> contributionMap = Maps.newHashMap();
        for( int i = 0 ; i < outputLength ; ++i ) {
            Set<Monomial> monomials = Sets.newHashSet();
            while( monomials.size() < numTerms ) {
                Monomial monomial = Monomial.randomMonomial( inputLength , maxOrder );
                if( monomials.add( monomial ) ) {
                    BitVector contribution = contributionMap.get( monomial );
                    if( contribution == null ) {
                        contribution = new BitVector( outputLength );
                        contributionMap.put( monomial , contribution );
                    }
                    contribution.set( i );
                }
            }
            
        }
        
        return fromMonomialContributionMap( inputLength, outputLength, contributionMap );
    }
    
    /**
     * Generates dense random multivariate quadratic functions.
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @return a random multivariate quadratic polynomial function over GF(2)
     */
    public static SimplePolynomialFunction denseRandomMultivariateQuadratic(int inputLength, int outputLength) {
        Map<Monomial, BitVector> contributionMap = Maps.newHashMap();
        int maxIndex = (inputLength*(inputLength + 1))>>>1;
        Monomial[] monomials = new Monomial[ maxIndex ];
        BitVector[] contributions = new BitVector[ maxIndex ];
        
        for(int i = 0; i<inputLength; ++i) {
        	monomials[ i ] = new Monomial( inputLength ).chainSet( i );
        	contributions[ i ] = BitUtils.randomVector( outputLength );
        }
        
        int flatIndex = 0;
        for( int j = 0; j < inputLength; ++j) {
        	for( int k = j; k < inputLength; ++k ) {
        	    flatIndex = j*inputLength - ((j*(j-1))>>>1) + k - j;
        		monomials[ flatIndex ] = new Monomial( inputLength ).chainSet( j ).chainSet( k );
        		contributions[ flatIndex ] = BitUtils.randomVector( outputLength );
//        		contributionMap.put( monomial ,  );
        	}
        }       
        return new PolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
//        return PolynomialFunctions.fromMonomialContributionMap( inputLength, outputLength, contributionMap );
    }
    
    /**
     * Constructs an array of random multivariate quadratic functions.
     * @param inputLength Number of input bits to the polynomial function.
     * @param outputLength Number of output bits to the polynomial function.
     * @param count The number of functions to construct in the array.
     * @return
     */
    public static SimplePolynomialFunction[] arrayOfRandomMultivariateQuadratics(int inputLength, int outputLength, int count) {
    	SimplePolynomialFunction [] functions = new SimplePolynomialFunction[ count ];
    	
    	for(int i = 0; i < functions.length; ++i) {
    		functions[i] = denseRandomMultivariateQuadratic(inputLength, outputLength);  
    	}
    	
    	return functions;
    }
    
    /**
     * @TODO
     * @param inputLength
     * @param outputLength
     * @return
     */
    public static SimplePolynomialFunction randomLinearCombination( int inputLength, int outputLength ) {
    	return null;
    }

	public static SimplePolynomialFunction fromMonomialContributionMap( int inputLength , int outputLength , Map<Monomial,BitVector> monomialContributionsMap) {
	    PolynomialFunctionGF2.removeNilContributions(monomialContributionsMap);
	    Monomial[] newMonomials = new Monomial[ monomialContributionsMap.size() ];
	    BitVector[] newContributions = new BitVector[ monomialContributionsMap.size() ];
	    int index = 0;
	    for( Entry<Monomial,BitVector> entry : monomialContributionsMap.entrySet() ) {
	        BitVector contribution = entry.getValue();
	        newMonomials[ index ] = entry.getKey();
	        newContributions[ index ] = contribution;
	        ++index;
	    }
	    return new PolynomialFunctionGF2( inputLength , outputLength , newMonomials , newContributions );
	}

	/**
	 * Builds a new function by concatenating the output of the input functions. It does not change the length
	 * of the input and the new outputs will be the same order as they are passed in.
	 * @param first function whose outputs will become the first set of output of the new function
	 * @param second function whose outputs will become the first set of output of the new function
	 * @return a function that maps inputs to outputs consisting of the concatenated output of the first and second functions.
	 */
	public static SimplePolynomialFunction concatenate( SimplePolynomialFunction first , SimplePolynomialFunction second ) {
	    Preconditions.checkArgument( first.getInputLength() == second.getInputLength() , "Functions being composed must have compatible monomial lengths" );
	    int lhsOutputLength = first.getOutputLength();
	    int rhsOutputLength = second.getOutputLength();
	    int combinedOutputLength = lhsOutputLength + rhsOutputLength;
	    Map<Monomial, BitVector> lhsMap = 
	            FunctionUtils.mapViewFromMonomialsAndContributions( first.getMonomials() , first.getContributions() );
	    Map<Monomial, BitVector> rhsMap =
	            FunctionUtils.mapViewFromMonomialsAndContributions( second.getMonomials() , second.getContributions() );
	    Map<Monomial, BitVector> monomialContributionMap = Maps.newHashMap();
	    
	    Set<Monomial> monomials = Sets.union( lhsMap.keySet() , rhsMap.keySet() );
	    for( Monomial monomial : monomials ) {
	        BitVector lhsContribution = lhsMap.get( monomial );
	        BitVector rhsContribution = rhsMap.get( monomial );
	        long[] newElements = new long[ combinedOutputLength >>> 6 ];
	        
	        if( lhsContribution != null ) { 
	            for( int i = 0 ; i < lhsOutputLength >>> 6; ++i ) {
	                newElements[ i ] = lhsContribution.elements()[ i ];
	            }
	        }
	        
	        if( rhsContribution != null ) { 
	            for( int i = 0 ; i < rhsOutputLength >>> 6; ++i ) {
	                newElements[ i + lhsOutputLength>>>6 ] = rhsContribution.elements()[ i ];
	            }
	        }
	        
	        monomialContributionMap.put( monomial , new BitVector( newElements , combinedOutputLength ) );
	    }
	    
	    return fromMonomialContributionMap( 
	                        first.getInputLength(), 
	                        combinedOutputLength ,  
	                        monomialContributionMap );
	    
	}
	
	public static Pair<SimplePolynomialFunction,SimplePolynomialFunction> randomlyPartitionMVQ( SimplePolynomialFunction f ) {
	    Preconditions.checkArgument( f.getMaximumMonomialOrder() == 2 );
	    
	    SimplePolynomialFunction g = denseRandomMultivariateQuadratic( f.getInputLength(), f.getOutputLength() );
	    SimplePolynomialFunction h = f.xor(g);
	    
	    return Pair.of(g, h);
	}
}
