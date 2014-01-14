package com.kryptnostic.multivariate;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
    
    public PolynomialFunction compose( PolynomialFunction inner ) {
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
    
    public static PolynomialFunction randomFunction( int intputLen , int outputLen ) {
        return null;
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
