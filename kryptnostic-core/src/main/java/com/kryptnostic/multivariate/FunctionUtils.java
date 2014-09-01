package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class FunctionUtils {
	private FunctionUtils() {}
	
    public static <T> T[] mergeArrays( T[] lhs, T[] rhs ) {
        T[] newArray = Arrays.copyOf( lhs , lhs.length + rhs.length );
        for( int i = lhs.length ; i < rhs.length ; ++i ) {
            newArray[ i ] = rhs[ i - lhs.length ];
        }
        
        return newArray;
    }
    
    public static BitVector concatenate( BitVector ... elements ) {
        Preconditions.checkArgument( Preconditions.checkNotNull( elements , "Null elements are not concatenatable.").length > 1, "Need at least two elements to concatenate" );
        int newLength= 0;
        for( BitVector v : elements ){
            Preconditions.checkArgument( v.size()%64==0 , "Concatenate only works for block lengths of size 64." );
            newLength += v.elements().length;
        }
        
        BitVector result = new BitVector( newLength<<6 );
        long[] resultBits = result.elements();
        int i = 0;
        for( BitVector v : elements ) {
            long[] bits = v.elements();
            for( int j = 0 ; j < bits.length ; ++j ) {
                resultBits[i++] = bits[j];
            }
            
        }
        
        return result;
    }
    
    public static BitVector concatenate( BitVector lhs, BitVector rhs ) {
        Preconditions.checkArgument( (lhs.size()%64)==0 && (rhs.size()%64)==0 , "Block length for concatenate must be a multiple of 64.");
        BitVector concatenated = new BitVector( Arrays.copyOf( lhs.elements() , lhs.elements().length + rhs.elements().length ) , lhs.size() + rhs.size() );
        for( int i = 0 ; i < rhs.elements().length ; ++i ) {
            concatenated.elements()[ i + lhs.elements().length ] = rhs.elements()[ i ];
        }
        return concatenated;
    }
    
    /**
     * Provides a mutable map view over an array of {@code Monomial}s and corresponding {@code BitVector} contributions.
     *  
     * @param monomials The monomials to use as the map key
     * @param contributions The contributions paired with each monomial. 
     * @return a {@code HashMap<Monomial,BitVector} with each monomial paired to its contribution. 
     */
    public static Map<Monomial, BitVector> mapViewFromMonomialsAndContributions( Monomial[] monomials, BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for( int i = 0 ; i < monomials.length ; ++i  ) {
            result.put( monomials[ i ] , contributions[ i ] );
        }
        return result;
    }
    
    public static SimplePolynomialFunction fromString( int monomialSize , String polynomialString ) {
        List<String> lines = 
                Splitter
                    .on( "\n" )
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList( polynomialString );
        int row = 0;
        Map<Monomial, BitVector> monomialContributionsMap = Maps.newHashMap();
        for( String line : lines ) {
            Iterable<String> monomials = 
                    Splitter
                    .on( "+" )
                    .trimResults()
                    .omitEmptyStrings()
                    .split( line );
            
            for( String monomialString : monomials ) {
                Monomial m = Monomial.fromString( monomialSize , monomialString );
                BitVector contribution = monomialContributionsMap.get( m );
                if( contribution == null ) {
                    contribution = new BitVector( lines.size() );
                    monomialContributionsMap.put( m , contribution );
                } 
                contribution.set( row );
            }
            ++row;
        }
        
        return PolynomialFunctions.fromMonomialContributionMap( monomialSize , lines.size() , monomialContributionsMap );
    }
 
    public static SimplePolynomialFunction concatenateInputsAndOutputs( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs ) {
        int combinedInputLength = lhs.getInputLength() + rhs.getInputLength();
        Monomial[] lhsMonomials = lhs.getMonomials();
        Monomial[] rhsMonomials = rhs.getMonomials();
        int numMonomials = lhsMonomials.length + rhsMonomials.length;
        Monomial[] newMonomials = new Monomial[numMonomials];
        for (int i = 0; i < lhsMonomials.length; i++) {
            newMonomials[i] = lhsMonomials[i].extend(combinedInputLength);
        }
        
        for (int i = 0; i < rhsMonomials.length; i++) {
            newMonomials[i + lhsMonomials.length] = rhsMonomials[i].extendAndShift(combinedInputLength, lhs.getInputLength());
        }
        
        int combinedOutputLength = lhs.getOutputLength() + rhs.getOutputLength();
        BitVector[] lhsContributions = lhs.getContributions();
        BitVector[] rhsContributions = rhs.getContributions();
        int numContributions = lhsContributions.length + rhsContributions.length;
        BitVector[] newContributions = new BitVector[numContributions];
        for(int i = 0; i < lhsContributions.length; i++) {
            newContributions[i] = BitUtils.extend(lhsContributions[i], combinedOutputLength);
        }
        
        for (int i = 0; i < rhsContributions.length; i++) {
            newContributions[i + lhsContributions.length] = BitUtils.extendAndShift(rhsContributions[i], combinedOutputLength, lhs.getOutputLength());
        }
        // TODO bug! ParameterizedFunctions have a longer monomials than function length, so this creates a monomial with a larger backing size than combinedInputLength
        return new OptimizedPolynomialFunctionGF2( combinedInputLength, combinedOutputLength, newMonomials, newContributions );
    }
}
