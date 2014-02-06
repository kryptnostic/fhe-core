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
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunctionRepresentationGF2;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * This class is used for operating on and evaluating vector polynomial functions over GF(2)
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionGF2 extends PolynomialFunctionRepresentationGF2 implements SimplePolynomialFunction {
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
    
    public int getOutputLength() { 
        return outputLength;
    }
    
    public SimplePolynomialFunction xor( SimplePolynomialFunction rhs ) {
        Preconditions.checkArgument( inputLength == rhs.getInputLength() , "Function being added must have the same input length." );
        Preconditions.checkArgument( outputLength == rhs.getOutputLength() , "Function being added must have the same output length." );
        
        Map<Monomial,BitVector> monomialContributionsMap = mapCopyFromMonomialsAndContributions(monomials, contributions);
        Monomial[] rhsMonomials = rhs.getMonomials();
        BitVector[] rhsContributions = rhs.getContributions();
        for( int i = 0 ; i < rhsMonomials.length ; ++i  ) {
            Monomial m = rhsMonomials[ i ];
            BitVector contribution = monomialContributionsMap.get( rhsMonomials[ i ] );
            if( contribution == null ){
                contribution = new BitVector( outputLength ) ;
                monomialContributionsMap.put( m , contribution );
            }
            contribution.xor( rhsContributions[ i ] );
        }
        
//        removeNilContributions( monomialContributionsMap );
//        Monomial[] newMonomials = new Monomial[ monomialContributionsMap.size() ];
//        BitVector[] newContributions = new BitVector[ monomialContributionsMap.size() ];
//        int index = 0;
//        for( Entry<Monomial,BitVector> entry : monomialContributionsMap.entrySet() ) {
//            BitVector contribution = entry.getValue();
////            if( contribution.cardinality() > 0 ) {
//                newMonomials[ index ] = entry.getKey();
//                newContributions[ index ] = contribution;
//                ++index;
////            }
//        }
        
        return PolynomialFunctionGF2.fromMonomialContributionMap( inputLength , outputLength , monomialContributionsMap );
    }
    
    public SimplePolynomialFunction and( SimplePolynomialFunction rhs ) {
        Preconditions.checkArgument( inputLength == rhs.getInputLength() , "Functions must have the same input length." );
        Preconditions.checkArgument( outputLength == rhs.getOutputLength() , "Functions must have the same output length." );
        Map<Monomial, BitVector> results = Maps.newHashMap();
        Monomial[] rhsMonomials = rhs.getMonomials();
        BitVector[] rhsContributions = rhs.getContributions();
        for( int i = 0 ; i < monomials.length ; ++i ) {
            for( int j = 0 ;  j < rhsMonomials.length; ++j ) {
                Monomial product = this.monomials[ i ].product( rhsMonomials[ j ] );
                BitVector contribution = this.contributions[ i ].copy();
                contribution.and( rhsContributions[ j ] );
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
        
        return new PolynomialFunctionGF2( inputLength , outputLength, newMonomials , newContributions);
    }
    
    public BitVector apply( BitVector input ) {
        BitVector result = new BitVector( outputLength );
        
        for( int i = 0 ; i < monomials.length ; ++i ) {
            Monomial term =  monomials[ i ];
            if( term.eval( input ) ) {
                result.xor( contributions[ i ] );
            }
        }
                
        return result;
    }
    
    @Override
    public BitVector apply( BitVector lhs , BitVector rhs ) {
        return apply( FunctionUtils.concatenate( lhs , rhs) );
    }
   
    public SimplePolynomialFunction compose( SimplePolynomialFunction inner ) {
        //Verify the functions are composable
        Preconditions.checkArgument( 
                inputLength == inner.getOutputLength() ,
                "Input length of outer function must match output length of inner function it is being composed with"
                );
        Set<Monomial> requiredMonomials = Sets.newHashSet( monomials );
        Map<Monomial, Set<Monomial>> memoizedComputations = initializeMemoMap( inputLength , inner.getMonomials() , inner.getContributions() );
        
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
        
        return fromMonomialContributionMap( inner.getInputLength() , outputLength , composedFunction );  
    }
    
    @Override
    public SimplePolynomialFunction compose( SimplePolynomialFunction lhs, SimplePolynomialFunction rhs) {
        return this.compose( concatenate( lhs , rhs ) );
        
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
            BitVector contribution = BitUtils.randomVector( outputLen );
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
    
    public static PolynomialFunctionGF2 concatenate( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs ) {
        Preconditions.checkArgument( lhs.getInputLength() == rhs.getInputLength() , "Functions being composed must have compatible monomial lengths" );
        int lhsOutputLength = lhs.getOutputLength();
        int rhsOutputLength = rhs.getOutputLength();
        int combinedOutputLength = lhsOutputLength + rhsOutputLength;
        Map<Monomial, BitVector> lhsMap = 
                PolynomialFunctionGF2.mapViewFromMonomialsAndContributions( lhs.getMonomials() , lhs.getContributions() );
        Map<Monomial, BitVector> rhsMap =
                PolynomialFunctionGF2.mapViewFromMonomialsAndContributions( rhs.getMonomials() , rhs.getContributions() );
        Map<Monomial, BitVector> monomialContributionMap = Maps.newHashMap();
        
        Set<Monomial> monomials = Sets.union( lhsMap.keySet() , rhsMap.keySet() );
        for( Monomial monomial : monomials ) {
            BitVector lhsContribution = lhsMap.get( monomial );
            BitVector rhsContribution = rhsMap.get( monomial );
            long[] newElements = new long[ combinedOutputLength >>> 3 ];
            
            if( lhsContribution != null ) { 
                for( int i = 0 ; i < newElements.length ; ++i ) {
                    newElements[ i ] = lhsContribution.elements()[ i ];
                }
            }
            
            if( rhsContribution != null ) { 
                for( int i = 0 ; i < rhsOutputLength ; ++i ) {
                    newElements[ i + lhsOutputLength ] = rhsContribution.elements()[ i ];
                }
            }
            
            monomialContributionMap.put( monomial , new BitVector( newElements , combinedOutputLength ) );
        }
        
        return PolynomialFunctionGF2
                    .fromMonomialContributionMap( 
                            lhs.getInputLength(), 
                            combinedOutputLength ,  
                            monomialContributionMap );
        
    }
    
    public static SimplePolynomialFunction concatenateInputsAndOutputs( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs ) {
//        Preconditions.checkArgument( lhs.getInputLength() == rhs.getInputLength() , "Functions being composed must have compatible monomial lengths" );
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
        int newMonomialArrayLength = lhsElementLength + rhsElementLength;
        int newContributionArrayLength = lhsContributions[ 0 ].elements().length + rhsContributions[ 0 ].elements().length;
        Monomial[] monomials = new Monomial[ lhsMonomials.length + rhsMonomials.length ];
        BitVector[] contributions = new BitVector[ monomials.length ];
        
        for( int i = 0 ; i < lhsMonomials.length ; ++i ) {
            monomials[ i ] = new Monomial( Arrays.copyOf( lhsMonomials[ i ].elements() , newMonomialArrayLength ) , combinedInputLength );
            contributions[ i ] = new BitVector( Arrays.copyOf( lhsContributions[ i ].elements() , newContributionArrayLength ) , combinedOutputLength ); 
        }
        
        for( int i = 0 ; i < rhsMonomials.length ; ++i ) {
            long[] newElements = new long[ newMonomialArrayLength ];
            long[] newContributionElements = new long[ newContributionArrayLength ];
            
            for( int j = lhsElementLength ; j < newMonomialArrayLength ; ++j ) {
                newElements[ j ] = rhsMonomials[ i ].elements()[ j -lhsElementLength ];
                newContributionElements [ j ] = rhsContributions[ i ].elements()[ j - lhsElementLength ];
            }
            monomials[ i + lhsMonomials.length ] = new Monomial( newElements , combinedInputLength );
            contributions[ i + lhsContributions.length ] = new BitVector( newContributionElements , combinedOutputLength );
        }
        
        return new PolynomialFunctionGF2( combinedInputLength , combinedOutputLength , monomials , contributions );
    }
    
    
    public static PolynomialFunctionGF2 fromMonomialContributionMap( int inputLength , int outputLength , Map<Monomial,BitVector> monomialContributionsMap) {
        removeNilContributions(monomialContributionsMap);
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
    
}
