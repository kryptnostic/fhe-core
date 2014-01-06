package com.kryptnostic.crypto;

import java.util.List;

import com.kryptnostic.multivariate.Monomial;
import com.kryptnostic.multivariate.PolynomialFunction;

import cern.colt.bitvector.BitVector;

public class PublicKey extends PolynomialFunction {

    public PublicKey( PrivateKey privateKey ) {
        super(inputLength, outputLength, monomials, contributions);
        // TODO Auto-generated constructor stub
    }

}
