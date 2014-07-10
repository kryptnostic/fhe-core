package com.kryptnostic.multivariate;

import java.util.List;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * @author mtamayo
 */
public class ParameterizedPolynomialFunctionGF2 extends PolynomialFunctionGF2 {
    private final CompoundPolynomialFunction chain;
    private final int externalInputLength;
    
    public ParameterizedPolynomialFunctionGF2(int inputLength, int outputLength, Monomial[] monomials, BitVector[] contributions, SimplePolynomialFunction[] chain) {
        super(inputLength, outputLength, monomials, contributions);
        Preconditions.checkArgument( chain.length > 0,"There must be a least one function in the provided chain.");
        this.chain = CompoundPolynomialFunctions.fromFunctions( chain );
        externalInputLength = inputLength - chain[ chain.length - 1 ].getOutputLength();
    }

    public ParameterizedPolynomialFunctionGF2(SimplePolynomialFunction e, List<SimplePolynomialFunction> chain ) {
        super( e.getInputLength(),e.getOutputLength(), e.getMonomials(), e.getContributions() );
        Preconditions.checkArgument( chain.size() > 0,"There must be a least one function in the provided chain.");
        this.chain = new CompoundPolynomialFunctionGF2(chain);
        externalInputLength = inputLength - this.chain.getOutputLength();
    }

    @Override
    public BitVector apply(BitVector input) {
        Preconditions.checkArgument( input.size() == externalInputLength , "Input length must be the same as the number of non-parameterized variables" );
        return super.apply( FunctionUtils.concatenate( input, chain.apply(input) ) );
    }
}
