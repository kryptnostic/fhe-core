package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
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

    public PolynomialFunctionGF2(int inputLength, int outputLength,
            List<Monomial> monomials, List<BitVector> contributions) {
        super(inputLength, outputLength, monomials, contributions);
    }
    
    public static class Builder extends PolynomialFunctionRepresentationGF2.Builder {
        public Builder(int inputLength, int outputLength) {
            super(inputLength, outputLength);
        }
        
        @Override
        protected PolynomialFunctionRepresentationGF2 make(
                int inputLength,
                int outputLength, List<Monomial> monomials,
                List<BitVector> contributions) {
            return new PolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
        }
        
        @Override
        public PolynomialFunctionGF2 build() {
            Pair<List<Monomial> , List<BitVector> > monomialsAndContributions = getMonomialsAndContributions(); 
            return new PolynomialFunctionGF2(inputLength, outputLength, monomialsAndContributions.getLeft() , monomialsAndContributions.getRight() );
        }
    }
    
    public static Builder builder(int inputLength, int outputLength) {
        return new Builder(inputLength, outputLength);
    }
    
    public PolynomialFunctionGF2 add( PolynomialFunctionGF2 rhs ) {
        Map<Monomial, BitVector> result = Maps.newHashMap();
        for( int i = 0 ; i < monomials.size() ; ++i  ) {
            result.put( monomials.get(i) , contributions.get( i ) );
        }
        
        for( int i = 0 ; i < rhs.monomials.size() ; ++i  ) {
            Monomial m = rhs.monomials.get( i );
            BitVector contribution = Objects.firstNonNull( result.get( rhs.monomials.get( i ) ) , new BitVector( outputLength ) );
            contribution.xor( rhs.contributions.get( i ) );
            result.put( m , contribution );
        }
        
        
        List<Monomial> newMonomials = Lists.newArrayListWithExpectedSize( result.size() );
        List<BitVector> newContributions = Lists.newArrayListWithExpectedSize( result.size() );
        
        for( Entry<Monomial,BitVector> entry : result.entrySet() ) {
            BitVector contribution = entry.getValue();
            if( contribution.cardinality() > 0 ) {
                newMonomials.add( entry.getKey() );
                newContributions.add( contribution );
            }
        }
        
        return new PolynomialFunctionGF2(inputLength, outputLength, newMonomials, newContributions);
    }
    
    public PolynomialFunctionGF2 product( PolynomialFunctionGF2 inner ) {
        Map<Monomial, BitVector> results = Maps.newHashMap();
        for( int i = 0 ; i < monomials.size() ; ++i ) {
            for( int j = 0 ;  j < inner.monomials.size(); ++j ) {
                Monomial product = this.monomials.get( i ).product( inner.monomials.get( j ) );
                BitVector contribution = this.contributions.get( i ).copy();
                contribution.and( inner.contributions.get( j ) );
                contribution.xor( Objects.firstNonNull( results.get( product ) , new BitVector( outputLength ) ) );
                results.put( product , contribution );
            }
        }
        List<Monomial> monomials = Lists.newArrayListWithExpectedSize( results.size() );
        List<BitVector> contributions = Lists.newArrayListWithExpectedSize( results.size() );
        for( Entry<Monomial ,BitVector> result : results.entrySet() ) {
            monomials.add( result.getKey() );
            contributions.add( result.getValue() );
        }
        
        return new PolynomialFunctionGF2( inner.inputLength , outputLength, monomials, contributions);
    }
    
    public BitVector evaluate( BitVector input ) {
        BitVector result = new BitVector( outputLength );
        
        for( int i = 0 ; i < monomials.size() ; ++i ) {
            Monomial term =  monomials.get( i );
            if( term.eval( input ) ) {
                result.xor( contributions.get( i ) );
            }
        }
                
        return result;
    }
    
    public PolynomialFunctionGF2 compose( PolynomialFunctionGF2 inner ) {
        Preconditions.checkArgument( 
                inputLength == inner.outputLength ,
                "Input length of outer function must match output length of inner function it is being composed with"
                );
        /*
         * f(g(x)) 
         * We compute this by determing CDAWG for the set of monomials in the outer function
         */
        Set<Monomial> requiredMonomials = Sets.newHashSet( monomials );
//        List<Set<Monomial>> results = ImmutableList.copyOf( 
//                Lists.transform( monomials , new Function<Monomial, Set<Monomial>> () {
//                    @Override
//                    public Set<Monomial> apply(Monomial m) {
//                        return Sets.newHashSet();
//                    }
//                }));
                
        Map<Monomial, Set<Monomial>> memoizedComputations = Maps.newHashMap();
        
        for( int i = 0 ; i < outputLength ; ++i ) {
            memoizedComputations.put( 
                    Monomial.linearMonomial( outputLength , i ) , 
                    contributionsToMonomials( i , monomials , contributions )
                    );
        }
        
        /*
         * Figure out most desirable product to compute next and use this to build up all required monomials.
         * 
         * We do this by:
         * 1) Computing all pairwise computable monomials, which is cheap relative to an actual product computation.
         * 2) Finding the most frequent of the pairwise computable monomials
         * 3) Computing the product corresponding to that monomial
         * 4) Memoizing that product corresponding to monomial.
         * 
         * We are done when we've have computed all required products.
         */
        
        while( !Sets.difference( requiredMonomials , memoizedComputations.keySet() ).isEmpty() ) {
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
            for( int i = 0 ; i < contributions.size(); ++i ) {
                if( contributions.get( i ).get( row ) ) {
                    //Symmetric difference, is equivalently to repeatedly xoring the sets together
                    monomialsForOutputRow = Sets.symmetricDifference( 
                            monomialsForOutputRow , 
                            Preconditions.checkNotNull(
                                    memoizedComputations.get( monomials.get( i ) ) ,
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
                Arrays.asList( backingMonomials ), 
                Arrays.asList( backingContributions ) );
        
        return composed;
        
    }
    
    public static Monomial mostFrequentFactor( List<Monomial> toBeComputed , Set<Monomial> readyToCompute , Set<Monomial> alreadyComputed ) {
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
    
    public static Set<Monomial> contributionsToMonomials( int row , List<Monomial> monomials, List<BitVector> contributions ) {
        int contribCount = contributions.size();
        Set<Monomial> result =Sets.newHashSetWithExpectedSize( contribCount/2 );
        for( int i = 0 ; i < contribCount ; ++i ) {
            if( contributions.get( i ).get( row ) ) {
                result.add( monomials.get( i ) );
            }
        }
        return result;
    }
    
    public static Set<Monomial> product( Monomial productMask, List<Monomial> monomials, List<BitVector> contributions ) {
        Set<Monomial> results = Sets.newHashSet(Monomial.constantMonomial( productMask.size() ) );
        for( int i = 0 ; i < productMask.size() ; ++i ) {
            Set<Monomial> next = Sets.newHashSet();
            if( productMask.get( i ) ) {
                for( int j = 0 ; j < contributions.size() ; ++j ) {
                    BitVector contribution = contributions.get( j );
                    if( contribution.get( i ) ) {
                        Monomial m = monomials.get( j );
                        for( Monomial monomial : results ) {
                            Monomial product = monomial.product( m );
                            if( !next.add( product ) ) {
                                next.remove( product );
                            }
                        }
                        
                    }
                }
                results = next;
            }
        }
        return results;
    }
    
    public static Monomial getMostFrequentOrderTwoMonomial(  Map<Monomial, Set<Monomial>> monomials ) {
        return getMostFrequentMonomial( monomials.keySet() ,  2 ); 
    }
    
    public static Monomial getMostFrequentMonomial( Collection<Monomial> monomials , int order ) {
        int maxFrequency = 0;
        Monomial mostFrequent = null;
        Map<Monomial, Integer> frequencies = Maps.newHashMap();
        for( Monomial monomial : monomials ) {
            for( Monomial monomialSubset : monomial.subsets( order ) ) {
                int frequency = Objects.firstNonNull( frequencies.get( monomialSubset ) , 0 ) + 1;
                if( frequency > maxFrequency ) {
                    maxFrequency = frequency;
                    mostFrequent = monomialSubset;
                }
                frequencies.put( monomialSubset , frequency );
            }
        }
        
        Preconditions.checkNotNull( mostFrequent );
        return mostFrequent;
    }
    
    public static PolynomialFunctionGF2 randomFunction( int inputLen , int outputLen ) {
        PolynomialFunctionGF2.Builder builder = PolynomialFunctionGF2.builder( inputLen , outputLen );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        return builder.build();
    }
    
    public static PolynomialFunctionGF2 identity( int monomialCount ) {
        List<Monomial> monomials = Lists.newArrayList();
        List<BitVector> contributions = Lists.newArrayList();
        
        for( int i = 0 ; i < monomialCount ; ++i ) {
            monomials.add( Monomial.linearMonomial( monomialCount , i) );
            BitVector contribution = new BitVector( monomialCount );
            contribution.set( i );
            contributions.add( contribution );
        }
        
        return new PolynomialFunctionGF2( monomialCount , monomialCount , monomials , contributions);
    }
    
    
}
