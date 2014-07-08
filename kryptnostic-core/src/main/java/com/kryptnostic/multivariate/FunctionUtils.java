package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

public class FunctionUtils {
    public static <T> T[] mergeArrays( T[] lhs, T[] rhs ) {
        T[] newArray = Arrays.copyOf( lhs , lhs.length + rhs.length );
        for( int i = lhs.length ; i < rhs.length ; ++i ) {
            newArray[ i ] = rhs[ i - lhs.length ];
        }
        
        return newArray;
    }
    
    public static BitVector subVector( BitVector v , int from , int to ) {
        int len = to - from;
        return new BitVector( Arrays.copyOfRange( v.elements() , from , to ) , len << 3 );
    }
    
    public static BitVector concatenate( BitVector lhs, BitVector rhs ) {
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
        int lhsInputLength = lhs.getInputLength();
        int rhsInputLength = rhs.getInputLength();
        int lhsOutputLength = lhs.getOutputLength(); 
        int rhsOutputLength = rhs.getOutputLength();
        int combinedInputLength = lhsInputLength + rhsInputLength;
        int combinedOutputLength = lhsOutputLength + rhsOutputLength;
        Monomial[] lhsMonomials = lhs.getMonomials();
        Monomial[] rhsMonomials = rhs.getMonomials();
        BitVector[] lhsContributions = lhs.getContributions();
        BitVector[] rhsContributions = rhs.getContributions();
        int lhsElementLength = lhsMonomials[ 0 ].elements().length;
        int rhsElementLength = rhsMonomials[ 0 ].elements().length;
        int lhsContributionLength = lhsContributions[ 0 ].elements().length;
        int rhsContributionLength = rhsContributions[ 0 ].elements().length;
        int newMonomialArrayLength = lhsElementLength + rhsElementLength;
        int newContributionArrayLength = lhsContributionLength + rhsContributionLength;
        Monomial[] monomials = new Monomial[ lhsMonomials.length + rhsMonomials.length ];
        BitVector[] contributions = new BitVector[ monomials.length ];
        
        for( int i = 0 ; i < lhsMonomials.length ; ++i ) {
            monomials[ i ] = new Monomial( Arrays.copyOf( lhsMonomials[ i ].elements() , newMonomialArrayLength ) , combinedInputLength );
            contributions[ i ] = new BitVector( Arrays.copyOf( lhsContributions[ i ].elements() , newContributionArrayLength ) , combinedOutputLength ); 
        }
        
        for( int i = 0 ; i < rhsMonomials.length ; ++i ) {
            long[] newElements = new long[ newMonomialArrayLength ];
            long[] newContributionElements = new long[ newMonomialArrayLength ];
            
            for( int j = lhsElementLength ; j < newMonomialArrayLength ; ++j ) {
                newElements[ j ] = rhsMonomials[ i ].elements()[ j - lhsElementLength ];
                for( int k = lhsContributionLength ; k < newContributionArrayLength ; ++k ) {
                    newContributionElements[ k ] = rhsContributions[ i ].elements()[ k - rhsContributionLength ];
                }
            }
            monomials[ i + lhsMonomials.length ] = new Monomial( newElements , combinedInputLength );
            contributions[ i + lhsContributions.length ] = new BitVector( newContributionElements , combinedOutputLength );
        }
        
        return new PolynomialFunctionGF2( combinedInputLength , combinedOutputLength , monomials , contributions );
    }
}
