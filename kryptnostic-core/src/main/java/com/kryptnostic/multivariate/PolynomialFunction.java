package com.kryptnostic.multivariate;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.EnhancedBitMatrix;

import cern.colt.bitvector.BitVector;

public class PolynomialFunction extends PolynomialFunctionRepresentation {

    public PolynomialFunction(int inputLength, int outputLength,
            List<Monomial> monomials, List<BitVector> contributions) {
        super(inputLength, outputLength, monomials, contributions);
    }
    
    public static class Builder extends PolynomialFunctionRepresentation.Builder {
        public Builder(int inputLength, int outputLength) {
            super(inputLength, outputLength);
        }
        
        @Override
        protected PolynomialFunctionRepresentation make(
                int inputLength,
                int outputLength, List<Monomial> monomials,
                List<BitVector> contributions) {
            return new PolynomialFunction(inputLength, outputLength, monomials, contributions);
        }
        
        @Override
        public PolynomialFunction build() {
            Pair<List<Monomial> , List<BitVector> > monomialsAndContributions = getMonomialsAndContributions(); 
            return new PolynomialFunction(inputLength, outputLength, monomialsAndContributions.getLeft() , monomialsAndContributions.getRight() );
        }
    }
    
    public static Builder builder(int inputLength, int outputLength) {
        return new Builder(inputLength, outputLength);
    }
    
    public PolynomialFunction add( PolynomialFunction rhs ) {
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
        
        return new PolynomialFunction(inputLength, outputLength, newMonomials, newContributions);
    }
    public PolynomialFunction product( PolynomialFunction inner ) {
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
        
        return new PolynomialFunction( inner.inputLength , outputLength, monomials, contributions);
    }
    
    public BitVector evalute( BitVector input ) {
        BitVector result = new BitVector( outputLength );
        
        for( int i = 0 ; i < monomials.size() ; ++i ) {
            Monomial term =  monomials.get( i );
            if( term.eval( input ) ) {
                result.xor( contributions.get( i ) );
            }
        }
                
        return result;
    }
    
    public PolynomialFunction compose( PolynomialFunction inner ) {
        /*
         * f(g(x)) 
         * We compute this by determing CDAWG for the set of monomials in the outer function
         */
        
        Map<Monomial, Integer> frequencies = Maps.newHashMap();
        for( Monomial m : monomials ) {
            for( Monomial pair : m.subsets( 2 ) ) {
                frequencies.put( pair , Objects.firstNonNull( frequencies.get( pair ) , 0 ) + 1 );
            }
        }
        
        TreeSet<MonomialCountHolder> sortedFrequencies = Sets.newTreeSet( Iterables.transform( frequencies.entrySet() , MonomialCountHolder.getFrequencyTransformer() ) );
        frequencies = null;
        
        for( MonomialCountHolder frequency : sortedFrequencies ) {
            /*
             * For each monomial product 
             */
        }
        
        List<BitVector> transposeContrib = Lists.newArrayList( contributions ); 
        EnhancedBitMatrix.transpose( transposeContrib , outputLength );
        
        Map<Monomial, Map<Monomial,BitVector>> computeCache;
        
        
        
        PolynomialFunction composed = new PolynomialFunction(inputLength, outputLength, monomials, contributions);
        
        return composed;
        
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
    
    public static PolynomialFunction randomFunction( int inputLen , int outputLen ) {
        PolynomialFunction.Builder builder = PolynomialFunction.builder( inputLen , outputLen );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        return builder.build();
    }
    
    public static PolynomialFunction identity( int monomialCount ) {
        List<Monomial> monomials = Lists.newArrayList();
        List<BitVector> contributions = Lists.newArrayList();
        
        for( int i = 0 ; i < monomialCount ; ++i ) {
            monomials.add( Monomial.linearMonomial( monomialCount , i) );
            BitVector contribution = new BitVector( monomialCount );
            contribution.set( i );
            contributions.add( contribution );
        }
        
        return new PolynomialFunction( monomialCount , monomialCount , monomials , contributions);
    }
    
    
}
