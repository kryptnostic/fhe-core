package com.kryptnostic.multivariate.gf2;

import cern.colt.bitvector.BitVector;

public interface SimplePolynomialFunction extends PolynomialFunction {
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
    
    public abstract Monomial[] getMonomials();
    public abstract BitVector[] getContributions();
    public abstract int getTotalMonomialCount();
}
