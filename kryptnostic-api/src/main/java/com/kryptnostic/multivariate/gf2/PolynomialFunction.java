package com.kryptnostic.multivariate.gf2;

import com.google.common.base.Function;

import cern.colt.bitvector.BitVector;


public interface PolynomialFunction extends Function<BitVector,BitVector> {
    public abstract BitVector apply( BitVector lhs , BitVector rhs );
    public abstract int getInputLength();
    public abstract int getOutputLength();
}
