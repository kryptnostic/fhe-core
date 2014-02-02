package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunctionRepresentationGF2;

import cern.colt.bitvector.BitVector;

/**
 * This class is used for operating on and evaluating vector polynomial functions over GF(2)
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionGF2 extends PolynomialFunctionRepresentationGF2  {
    private static final Predicate<BitVector> notNilContributionPredicate = new Predicate<BitVector>() {
        @Override
        public boolean apply(BitVector v) {
            for( long l : v.elements() ) {
                if( l != 0 ) {
                    return true;
                }
            }
            return false;
        }
    };
    
    public PolynomialFunctionGF2(int inputLength, int outputLength,
            Monomial[] monomials, BitVector[] contributions) {
        super(inputLength, outputLength, monomials, contributions);
    }
    
    public static class Builder extends PolynomialFunctionRepresentationGF2.Builder {
        public Builder(int inputLength, int outputLength) {
            super(inputLength, outputLength);
        }
        
        @Override
        protected PolynomialFunctionRepresentationGF2 make(
                int inputLength,
                int outputLength, 
                Monomial[] monomials,
                BitVector[] contributions) {
            return new PolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
        }
        
        @Override
        public PolynomialFunctionGF2 build() {
            Pair<Monomial[] , BitVector[]> monomialsAndContributions = getMonomialsAndContributions(); 
            return new PolynomialFunctionGF2(inputLength, outputLength, monomialsAndContributions.getLeft() , monomialsAndContributions.getRight() );
        }
    }
    
    public static Builder builder(int inputLength, int outputLength) {
        return new Builder(inputLength, outputLength);
    }
    
    public PolynomialFunctionGF2 add( PolynomialFunctionGF2 rhs ) {
        Map<Monomial,BitVector> monomialContributionsMap = mapCopyFromMonomialsAndContributions(monomials, contributions);
        
        for( int i = 0 ; i < rhs.monomials.length ; ++i  ) {
            Monomial m = rhs.monomials[ i ];
            BitVector contribution = monomialContributionsMap.get( rhs.monomials[ i ] );
            if( contribution == null ){
                contribution = new BitVector( outputLength ) ;
                monomialContributionsMap.put( m , contribution );
            }
            contribution.xor( rhs.contributions[ i ] );
        }
        
        removeNilContributions( monomialContributionsMap );
        Monomial[] newMonomials = new Monomial[ monomialContributionsMap.size() ];
        BitVector[] newContributions = new BitVector[ monomialContributionsMap.size() ];
        int index = 0;
        for( Entry<Monomial,BitVector> entry : monomialContributionsMap.entrySet() ) {
            BitVector contribution = entry.getValue();
            if( contribution.cardinality() > 0 ) {
                newMonomials[ index ] = entry.getKey();
                newContributions[ index ] = contribution;
                ++index;
            }
        }
        
        return new PolynomialFunctionGF2(inputLength, outputLength, newMonomials, newContributions);
    }
    
    public PolynomialFunctionGF2 product( PolynomialFunctionGF2 inner ) {
        Map<Monomial, BitVector> results = Maps.newHashMap();
        for( int i = 0 ; i < monomials.length ; ++i ) {
            for( int j = 0 ;  j < inner.monomials.length; ++j ) {
                Monomial product = this.monomials[ i ].product( inner.monomials[ j ] );
                BitVector contribution = this.contributions[ i ].copy();
                contribution.and( inner.contributions[ j ] );
                contribution.xor( Objects.firstNonNull( results.get( product ) , new BitVector( outputLength ) ) );
                results.put( product , contribution );
            }
        }
        
        removeNilContributions( results );
        Monomial[] newMonomials = new Monomial[ results.size() ]; 
        BitVector[] newContributions = new BitVector[ results.size() ];
        int index = 0;
        for( Entry<Monomial ,BitVector> result : results.entrySet() ) {
            BitVector contribution = result.getValue();
            if( contribution.cardinality() > 0 ) {
                newMonomials[ index ] = result.getKey();
                newContributions[ index ] = contribution;
                ++index;
            }
        }
        
        return new PolynomialFunctionGF2( inner.inputLength , outputLength, newMonomials , newContributions);
    }
    
    public BitVector evaluate( BitVector input ) {
        BitVector result = new BitVector( outputLength );
        
        for( int i = 0 ; i < monomials.length ; ++i ) {
            Monomial term =  monomials[ i ];
            if( term.eval( input ) ) {
                result.xor( contributions[ i ] );
            }
        }
                
        return result;
    }
    
    /**
     * Computes the function composition of the current function and another function.
     * @param inner The function to be used composed as the input to the current function.
     * @return A new function representing the function composition of this function and inner, 
     * such that evaluating it on input is equivalent to {@code this.evaluate( inner.evaluate( input ) )}  
     */
    public PolynomialFunctionGF2 compose( PolynomialFunctionGF2 inner ) {
        //Verify the functions are composable
        Preconditions.checkArgument( 
                inputLength == inner.outputLength ,
                "Input length of outer function must match output length of inner function it is being composed with"
                );
        Set<Monomial> requiredMonomials = Sets.newHashSet( monomials );
        Map<Monomial, Set<Monomial>> memoizedComputations = initializeMemoMap( inputLength , inner.monomials , inner.contributions );
        
        /*
         * Figure out most desirable product to compute next and use this to build up all required monomials.
         * 
         * We do this by:
         * 
         * 1) Computing all pairwise computable monomial factors for the outer monomial, 
         * which is cheap relative to an actual product computation.
         * 
         * 2) Finding the most frequently occurring factors from step 1.
         * 
         * 3) Computing the product corresponding to most frequently occurring factor.
         * 
         * 4) Memoizing the product from step 3.
         * 
         * We are done when we've have computed the products for all outer monomials.
         */
        
        while( !Sets.difference( requiredMonomials , memoizedComputations.keySet() ).isEmpty() ) {
            //TODO: allPossibleProductts already filters out previously computed products, remove double filtering.
            Map<Monomial, List<Monomial>> possibleProducts = allPossibleProduct( memoizedComputations.keySet() );  // 1
            Monomial mostFrequent = mostFrequentFactor( this.monomials , possibleProducts.keySet() , memoizedComputations.keySet() );                // 2
            List<Monomial> factors = Preconditions.checkNotNull( 
                    possibleProducts.get( mostFrequent ) ,
                    "Composition failure! Encountered unexpected null when searching for next product to compute.");
            Set<Monomial> mproducts = product( 
                    Preconditions.checkNotNull( memoizedComputations.get( factors.get( 0 ) ) ), 
                    Preconditions.checkNotNull( memoizedComputations.get( factors.get( 1 ) ) ) );  //3
            memoizedComputations.put( mostFrequent , mproducts ); //4
        }
        
        Map<Monomial, BitVector> composedFunction = Maps.newHashMap();
        
        /*
         * Each monomial that has been computed in terms of the inner function contributes a set of monomials
         * to each output of the outer function.  We need to resolve the sum of all of these in order to calculate
         * what the contribution of each newMonomial looks like.
         * 
         * For each BitVector in the contribution we check if that monomial contributes.
         */
        
        for( int row = 0; row < outputLength ; ++row ) {
            Set<Monomial> monomialsForOutputRow = ImmutableSet.of();
            for( int i = 0 ; i < contributions.length; ++i ) {
                if( contributions[ i ].get( row ) ) {
                    //Symmetric difference, is equivalently to repeatedly xoring the sets together
                    monomialsForOutputRow = Sets.symmetricDifference( 
                            monomialsForOutputRow , 
                            Preconditions.checkNotNull(
                                    memoizedComputations.get( monomials[ i ] ) ,
                                    "Monomial contributions cannot be null for a required monomial"
                                    ) 
                            );
                }
            }
            
            //For each monomial contributing to the output, set the contribution bit in the new contribution vectors.
            for( Monomial monomial : monomialsForOutputRow ){
                BitVector contribution = composedFunction.get( monomial );
                if( contribution == null ) {
                    contribution = new BitVector( outputLength );
                    composedFunction.put( monomial , contribution );
                }
                contribution.set( row );
            }
            
        }
        
        removeNilContributions( composedFunction );
        int newSize = composedFunction.keySet().size();
        Monomial[] backingMonomials = new Monomial[ newSize ];
        BitVector[] backingContributions = new BitVector[ newSize ];
        
        int mIndex = 0;
        for( Entry<Monomial, BitVector> entry : composedFunction.entrySet() ) {
            backingMonomials[ mIndex ] = entry.getKey();
            backingContributions[ mIndex ] = entry.getValue();
            ++mIndex;
        }
        
        PolynomialFunctionGF2 composed = new PolynomialFunctionGF2( 
                inner.inputLength, 
                outputLength, 
                backingMonomials, 
                backingContributions );
        
        return composed;
        
    }
    
    public PolynomialFunctionGF2 extend( int length ) {
        //TODO: Add re-entrant read/write lock for updating contributions.
        Monomial[] newMonomials = new Monomial[ monomials.length ];
        BitVector[] newContributions = new BitVector[ monomials.length ];
        
        for( int i = 0 ; i < contributions.length ; ++i ) {
            BitVector current = contributions[ i ];
            newMonomials[ i ] = monomials[ i ].clone();
            newContributions[ i ] = new BitVector( Arrays.copyOf( current.elements() , current.elements().length << 1 ) , current.size() << 1 );
        }
        
        return new PolynomialFunctionGF2(length, length, newMonomials, newContributions);
    }
    
    public PolynomialFunctionGF2 clone() {
        Monomial[] newMonomials = new Monomial[ monomials.length ];
        BitVector[] newContributions = new BitVector[ monomials.length ];
        
        for( int i = 0 ; i < monomials.length ; ++i ) {
            newMonomials[i] = monomials[i].clone();
            newContributions[i] = contributions[i].copy();
        }
        
        return new PolynomialFunctionGF2( 
                inputLength, 
                outputLength, 
                newMonomials, 
                newContributions );
    }
    
    public static Map<Monomial, Set<Monomial>> initializeMemoMap( int outerInputLength , Monomial[] monomials , BitVector[] contributions ) {
        Map<Monomial, Set<Monomial>> memoizedComputations = Maps.newHashMap();
        for( int i = 0 ; i < outerInputLength ; ++i ) {
            memoizedComputations.put( 
                    Monomial.linearMonomial( outerInputLength , i ) , 
                    contributionsToMonomials( i , monomials , contributions )
                    );
        }
        
        return memoizedComputations;
    }
    
    public static Monomial mostFrequentFactor( Monomial[] toBeComputed , Set<Monomial> readyToCompute , Set<Monomial> alreadyComputed ) {
        Monomial result = null;
        int max = -1;
        for( Monomial ready : readyToCompute ) {
            if( !alreadyComputed.contains( ready ) ) {
                int count = 0;
                for( Monomial onDeck : toBeComputed ) {
                    if( onDeck.hasFactor( ready ) ) {
                        count++;
                    }
                }
                if( count > max ) {
                    max = count;
                    result = ready;
                }
            }
        }
        return result;
    }
    
    //TODO: Decide whether its worth unit testing this.
    public static Map<Monomial,List<Monomial>> allPossibleProduct( Set<Monomial> monomials ) {
        Map<Monomial, List<Monomial>> result = Maps.newHashMap();
        for( Monomial lhs : monomials ) {
            for( Monomial rhs : monomials ) {
                //Skip identical monomials
                if( !lhs.equals( rhs ) ) {
                    Monomial product = lhs.product( rhs );
                    //Don't bother adding it to the list of possible products, if we've already seen it before.
                    if( !monomials.contains( product ) ) {
                        result.put( product , ImmutableList.of( lhs, rhs ) );
                    }
                }
            }
        }
        return result;
    }
    
    //TODO: Figure out whether this worth unit testing.
    public static Set<Monomial> product( Set<Monomial> lhs, Set<Monomial> rhs ) {
        Set<Monomial> result = Sets.newHashSetWithExpectedSize( lhs.size() * rhs.size() / 2 );
        for( Monomial mlhs : lhs ) {
            for( Monomial mrhs : rhs ) {
                Monomial product = mlhs.product( mrhs );
                if( !result.add( product ) ) {
                    result.remove( product );
                }
            }
        }
        return result;
    }
    
    public static Map<Monomial, BitVector> mapCopyFromMonomialsAndContributions( Monomial[] monomials, BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for( int i = 0 ; i < monomials.length ; ++i  ) {
            result.put( monomials[ i ].clone() , contributions[ i ].copy() );
        }
        return result;
    }
    
    public static void removeNilContributions( Map<Monomial,BitVector> monomialContributionMap ) {
        Set<Monomial> forRemoval = Sets.newHashSet();
        for( Entry<Monomial,BitVector> monomialContribution : monomialContributionMap.entrySet() ) {
            if( !notNilContributionPredicate.apply( monomialContribution.getValue() ) ) {
                forRemoval.add( monomialContribution.getKey() );
            }
        }
        for( Monomial m : forRemoval ) {
            monomialContributionMap.remove( m );
        }
    }
    
    //TODO: Figure out what's more efficient filter keys + copy to immutable map, or removing from existing map.
    public static Map<Monomial,BitVector> filterNilContributions( Map<Monomial, BitVector> monomialContributionMap ) {
        return ImmutableMap.copyOf( Maps.filterKeys( monomialContributionMap , notNilContributionPredicate ) );
    }
    
    public static Set<Monomial> contributionsToMonomials( int row , Monomial[] monomials, BitVector[] contributions ) {
        /*
         * Converts a single row of contributions into monomials.
         */
        Set<Monomial> result =Sets.newHashSetWithExpectedSize( contributions.length/2 );
        for( int i = 0 ; i < contributions.length ; ++i ) {
            if( contributions[ i ].get( row ) ) {
                result.add( monomials[ i ] );
            }
        }
        return result;
    }
    
    public static PolynomialFunctionGF2 randomFunction( int inputLen , int outputLen ) {
        PolynomialFunctionGF2.Builder builder = PolynomialFunctionGF2.builder( inputLen , outputLen );
        for( int i = 0 ; i < 16 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( outputLen );
            builder.setMonomialContribution( Monomial.randomMonomial( inputLen , 3 ) , contribution);
        }
        
        return builder.build();
    }
    
    public static PolynomialFunctionGF2 identity( int monomialCount ) {
        Monomial[] monomials = new Monomial[ monomialCount ];
        BitVector[] contributions = new BitVector[ monomialCount ];
        
        for( int i = 0 ; i < monomialCount ; ++i ) {
            monomials[i] = Monomial.linearMonomial( monomialCount , i);
            BitVector contribution = new BitVector( monomialCount );
            contribution.set( i );
            contributions[i] = contribution;
        }
        
        return new PolynomialFunctionGF2( monomialCount , monomialCount , monomials , contributions);
    }
    
    public static PolynomialFunctionGF2 truncatedIdentity( int outputLength , int inputLength ) {
        return truncatedIdentity( 0 , outputLength - 1 , inputLength );
    }
    
    public static PolynomialFunctionGF2 truncatedIdentity( int startMonomial , int stopMonomial , int inputLength) {
        int outputLength = stopMonomial - startMonomial + 1;
        Monomial[] monomials = new Monomial[ outputLength ];
        BitVector[] contributions = new BitVector[ outputLength ];
        
        for( int i = 0 ; i < outputLength ; ++i ) {
            monomials[i] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( outputLength );
            contribution.set( i );
            contributions[i] = contribution;
        }
        
        return new PolynomialFunctionGF2( inputLength , outputLength , monomials , contributions);
    }
    
    public static PolynomialFunctionGF2 XOR( int xorLength ) {
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
    
    public static PolynomialFunctionGF2 prepareForLhsOfBinaryOp( PolynomialFunctionGF2 lhs ) {
        Monomial[] monomials = new Monomial[ lhs.monomials.length ];
        BitVector[] contributions = new BitVector[ lhs.contributions.length ];
        for( int i = 0 ; i < lhs.monomials.length ; ++i ) {
            long[] elements = monomials[i].elements();
            monomials[i] = new Monomial( Arrays.copyOf( elements , elements.length << 1 ), lhs.getInputLength() << 1 );
            contributions[i] = contributions[i].copy();
        }
        
        return new PolynomialFunctionGF2( monomials[0].size() , contributions.length , monomials, contributions );
    }
    
    public static PolynomialFunctionGF2 prepareForRhsOfBinaryOp( PolynomialFunctionGF2 rhs ) {
        Monomial[] monomials = new Monomial[ rhs.monomials.length ];
        BitVector[] contributions = new BitVector[ rhs.contributions.length ];
        for( int i = 0 ; i < rhs.monomials.length ; ++i ) {
            long[] elements = monomials[i].elements();
            long[] newElements = new long[ elements.length << 1 ];
            for( int j = 0 ; j < elements.length ; ++j ) {
                newElements[ j ] = elements[ j ];
            }
            monomials[i] = new Monomial( newElements , rhs.getInputLength() << 1 );
            contributions[i] = contributions[i].copy();
        }
        
        return new PolynomialFunctionGF2( monomials[0].size() , contributions.length , monomials, contributions );
    }
    
}
