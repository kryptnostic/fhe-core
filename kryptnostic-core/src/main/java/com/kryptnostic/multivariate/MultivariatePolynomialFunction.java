package com.kryptnostic.multivariate;

import java.util.List;

import com.kryptnostic.multivariate.gf2.Monomial;

import cern.colt.bitvector.BitVector;
//TODO: Make this actually work generically for future GF(2^8) or other implementations.
/*
 * Limitation is that contributions will have to be their own class, for example byte[] for GF(2^*8)
 * Will have to extend BitVector base class and define an interface, which has add or combine contribution
 * function that respects the field / polynomial ring operations.
 */
public interface MultivariatePolynomialFunction {

    /**
     * Computes the direct sum of two multivariate polynomial functions.
     * @param rhs The function to add to the current function.
     * @return The resulting function for adding these two together.
     */
    public abstract MultivariatePolynomialFunction add(PolynomialFunctionGF2 rhs);

    /**
     * Computes the direct product of two polynomial functions.
     * @param rhs The function to perform the product with.
     * @return A multivariate function representing the product.
     */
    public abstract MultivariatePolynomialFunction product( PolynomialFunctionGF2 rhs );
    
    /**
     * Evaluates the current polynomial function.
     * @param input The polynomial function to evaluate.
     * @return The result of evaluating the polynomial function.
     */
    public abstract BitVector evalute(BitVector input);

    public abstract MultivariatePolynomialFunction compose(MultivariatePolynomialFunction inner);
    
    public abstract List<Monomial> getMonomials();
    public abstract List<BitVector> getContributions();
    
}