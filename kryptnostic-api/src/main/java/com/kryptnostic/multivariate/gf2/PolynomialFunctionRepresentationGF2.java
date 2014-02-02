package com.kryptnostic.multivariate.gf2;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cern.colt.bitvector.BitVector;


/**
 * Base class for storing and transmission of polynomial representations.
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionRepresentationGF2 {
    private static final String INPUT_LENGTH_PROPERTY = "input-length";
    private static final String OUTPUT_LENGTH_PROPERTY = "output-length";
    private static final String MONOMIALS_PROPERTY = "monomials";
    private static final String CONTRIBUTIONS_PROPERTY = "contributions";

    protected final int inputLength;
    protected final int outputLength;
    
    protected final Monomial[] monomials;
    protected final BitVector[] contributions;
    
    @JsonCreator
    public PolynomialFunctionRepresentationGF2(
            @JsonProperty( INPUT_LENGTH_PROPERTY ) int inputLength, 
            @JsonProperty( OUTPUT_LENGTH_PROPERTY ) int outputLength , 
            @JsonProperty( MONOMIALS_PROPERTY ) Monomial[] monomials , 
            @JsonProperty( CONTRIBUTIONS_PROPERTY ) BitVector[] contributions ) {
        this.inputLength = inputLength;
        this.outputLength = outputLength;
        this.monomials = monomials;
        this.contributions = contributions;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(contributions);
        result = prime * result + inputLength;
        result = prime * result + Arrays.hashCode(monomials);
        result = prime * result + outputLength;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof PolynomialFunctionRepresentationGF2))
            return false;
        PolynomialFunctionRepresentationGF2 other = (PolynomialFunctionRepresentationGF2) obj;
        if (outputLength != other.outputLength)
            return false;
        if (inputLength != other.inputLength)
            return false;
        Map<Monomial,BitVector> thisMap = mapViewFromMonomialsAndContributions(monomials, contributions);
        Map<Monomial,BitVector> objMap = mapViewFromMonomialsAndContributions(other.monomials, other.contributions);
        
        if( !Sets.symmetricDifference( thisMap.keySet() , objMap.keySet() ).isEmpty() ) {
            return false;
        }
      
        for( Entry<Monomial,BitVector> entry : thisMap.entrySet() ) {
            BitVector thisContribution = entry.getValue();
            BitVector otherContribution = objMap.get( entry.getKey() );
            if( otherContribution == null && thisContribution!=null ) {
                return false;
            }
            if( !otherContribution.equals( thisContribution ) ) {
                return false;
            }
        }
        
        return true;
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

        
        public PolynomialFunctionRepresentationGF2 build() {
            Pair<Monomial[] , BitVector[]> monomialsAndContributions = getMonomialsAndContributions(); 
            return new PolynomialFunctionRepresentationGF2(
                    inputLength, 
                    outputLength, 
                    monomialsAndContributions.getLeft() , 
                    monomialsAndContributions.getRight() );
        }
        
        protected Pair<Monomial[] , BitVector[]> getMonomialsAndContributions() {
            Monomial[] newMonomials = new Monomial[ monomials.size() ];
            BitVector[] newContributions = new BitVector[ newMonomials.length ];
            int index = 0;
            for( Entry<Monomial, BitVector> entry : monomials.entrySet() ) {
                newMonomials[ index ] = entry.getKey();
                newContributions[ index ] = entry.getValue();
                ++index;
             }
            return Pair.<Monomial[] , BitVector[]>of( newMonomials , newContributions );
        }
        
        protected PolynomialFunctionRepresentationGF2 make(int inputLength, int outputLength , Monomial[] monomials , BitVector[] contributions ) {
            return new PolynomialFunctionRepresentationGF2(inputLength, outputLength, monomials , contributions );
        }
    }
    
    @JsonProperty( INPUT_LENGTH_PROPERTY )
    public int getInputLength() {
        return inputLength;
    }
    
    @JsonProperty( MONOMIALS_PROPERTY )
    public Monomial[] getMonomials() {
        return monomials;
    }
    
    @JsonProperty( CONTRIBUTIONS_PROPERTY )
    public BitVector[] getContributions() {
        return contributions;
    }
    
    public static Builder builder( int inputLength, int outputLength  ) {
        return new Builder( inputLength , outputLength );
    }
    
    public static Map<Monomial, BitVector> mapViewFromMonomialsAndContributions( Monomial[] monomials, BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for( int i = 0 ; i < monomials.length ; ++i  ) {
            result.put( monomials[ i ].clone() , contributions[ i ].copy() );
        }
        return result;
    }
}