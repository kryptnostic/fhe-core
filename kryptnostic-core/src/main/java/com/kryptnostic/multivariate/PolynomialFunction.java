package com.kryptnostic.multivariate;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import cern.colt.matrix.linalg.Algebra;

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
    
    public BitVector evalute( BitVector input ) {
        BitVector result = new BitVector( outputLength );
        
        for( int i = 0 ; i < monomials.size() ; ++i ) {
            Monomial term =  monomials.get( i );
            if( term.eval( input ) ) {
                result.xor( contributions.get( i ) );
            }
        }
        Algebra a = new Algebra();
        
        return result;
    }
    
    
}
