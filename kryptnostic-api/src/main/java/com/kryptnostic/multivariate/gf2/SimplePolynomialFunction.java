package com.kryptnostic.multivariate.gf2;

import java.util.List;

import cern.colt.bitvector.BitVector;

import com.codahale.metrics.annotation.Timed;

public interface SimplePolynomialFunction extends PolynomialFunction {
    @Timed
    SimplePolynomialFunction xor( SimplePolynomialFunction input );

    @Timed
    SimplePolynomialFunction and( SimplePolynomialFunction input );
    
    /**
     * Computes the function composition of the current function and another function.
     * @param inner The function to be used composed as the input to the current function.
     * @return A new function representing the function composition of this function and inner, 
     * such that evaluating it on input is equivalent to {@code this.evaluate( inner.evaluate( input ) )}  
     */
    @Timed
    SimplePolynomialFunction compose( SimplePolynomialFunction inner );
    
    @Timed
    SimplePolynomialFunction compose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    
    /**
     * 
     * @return a new function that represents the partial composition of this function and 
     * inner, such that evaluating it is equivalent to {@code this.evaluate( inner.evaluate( lhs ), rhs )}  
     */
    @Timed
    SimplePolynomialFunction partialComposeLeft(SimplePolynomialFunction inner);
    
    /**
     * Computes this( [ lhs( x ) , rhs( y) ] ) 
     * @param lhs
     * @param rhs
     * @return
     */
    @Timed
    SimplePolynomialFunction concatenatingCompose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    
    @Timed
    SimplePolynomialFunction resolve( BitVector input );
    
    @Timed
    SimplePolynomialFunction optimize();
    
    @Timed
    SimplePolynomialFunction deoptimize();
    
    @Timed
    List<SimplePolynomialFunction> split( int ... splitPoints );
    
    Monomial[] getMonomials();
    BitVector[] getContributions();
    int getTotalMonomialCount();
    int getMaximumMonomialOrder();
    boolean isParameterized();
}
