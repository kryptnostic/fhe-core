package com.kryptnostic.multivariate.gf2;

import com.google.common.base.Function;

import cern.colt.bitvector.BitVector;

public interface SimplePolynomialFunction extends Function<BitVector,BitVector> {
    public abstract SimplePolynomialFunction xor( SimplePolynomialFunction input );
    public abstract SimplePolynomialFunction and( SimplePolynomialFunction input );
    
    /**
     * Computes the function composition of the current function and another function.
     * @param inner The function to be used composed as the input to the current function.
     * @return A new function representing the function composition of this function and inner, 
     * such that evaluating it on input is equivalent to {@code this.evaluate( inner.evaluate( input ) )}  
     */
    public abstract SimplePolynomialFunction compose( SimplePolynomialFunction inner );
    public abstract SimplePolynomialFunction compose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    public abstract BitVector apply( BitVector lhs , BitVector rhs );
    
    public abstract int getInputLength();
    public abstract int getOutputLength();
    public abstract Monomial[] getMonomials();
    public abstract BitVector[] getContributions();
}
