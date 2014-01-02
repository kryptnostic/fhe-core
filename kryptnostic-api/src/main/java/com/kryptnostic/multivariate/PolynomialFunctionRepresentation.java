package com.kryptnostic.multivariate;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import cern.colt.bitvector.BitVector;


/**
 * Base class for storing and transmission of polynomial representations.
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionRepresentation {
    protected final int inputLength;
    protected final int outputLength;
    
    protected final List<Monomial> monomials;
    protected final List<BitVector> contributions;
    
    public PolynomialFunctionRepresentation(int inputLength, int outputLength , List<Monomial> monomials , List<BitVector> contributions ) {
        this.inputLength = inputLength;
        this.outputLength = outputLength;
        this.monomials = monomials;
        this.contributions = contributions;
    }
    
    public static class Builder {
        protected Map<Monomial, BitVector> monomials = Maps.newHashMap();
        protected final int inputLength;
        protected final int outputLength;
        
        public Builder( int inputLength, int outputLength ) {
            this.inputLength = inputLength;
            this.outputLength = outputLength;
        }
                
        public void addMonomial( Monomial monomial ) {
            if( !monomials.containsKey( monomial ) ) {
                monomials.put( monomial , new BitVector( outputLength ) );
            } else {
                throw new InvalidParameterException( "Monomial " + monomial.toString() + " already exists.");
            }
        }
        
        public void addMonomialContribution( Monomial monomial , int outputBit ) {
            BitVector term = monomials.get( monomial );
            if( term == null ) {
                term = new BitVector( outputLength );
                monomials.put( monomial, term );
            }
            term.set( outputBit );
        }
        
        public void setMonomialContribution( Monomial monomial , BitVector contribution ) {
            monomials.put( monomial , contribution );
        }

        
        public PolynomialFunctionRepresentation build() {
            Pair<List<Monomial> , List<BitVector> > monomialsAndContributions = getMonomialsAndContributions(); 
            return new PolynomialFunctionRepresentation(inputLength, outputLength, monomialsAndContributions.getLeft() , monomialsAndContributions.getRight() );
        }
        
        protected Pair<List<Monomial> , List<BitVector> > getMonomialsAndContributions() {
            ImmutableList.Builder<Monomial> monomialsBuilder = ImmutableList.builder();
            ImmutableList.Builder<BitVector> contribBuilder = ImmutableList.builder();
            for( Entry<Monomial, BitVector> entry : monomials.entrySet() ) {
                monomialsBuilder.add( entry.getKey() );
                contribBuilder.add( entry.getValue() );
            }
            return Pair.<List<Monomial> , List<BitVector> >of(monomialsBuilder.build(), contribBuilder.build());
        }
        
        protected PolynomialFunctionRepresentation make(int inputLength, int outputLength , List<Monomial> monomials , List<BitVector> contributions ) {
            return new PolynomialFunctionRepresentation(inputLength, outputLength, monomials , contributions );
        }
    }
    
    public static Builder builder( int inputLength, int outputLength  ) {
        return new Builder( inputLength , outputLength );
    }
}