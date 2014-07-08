package com.kryptnostic.multivariate.gf2;

import cern.colt.bitvector.BitVector;

public interface SimplePolynomialFunction extends PolynomialFunction {
    SimplePolynomialFunction xor( SimplePolynomialFunction input );
    SimplePolynomialFunction and( SimplePolynomialFunction input );
    
    /**
     * Computes the function composition of the current function and another function.
     * @param inner The function to be used composed as the input to the current function.
     * @return A new function representing the function composition of this function and inner, 
     * such that evaluating it on input is equivalent to {@code this.evaluate( inner.evaluate( input ) )}  
     */
    SimplePolynomialFunction compose( SimplePolynomialFunction inner );
    SimplePolynomialFunction compose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    
    Monomial[] getMonomials();
    BitVector[] getContributions();
    int getTotalMonomialCount();
    int getMaximumMonomialOrder();
}
