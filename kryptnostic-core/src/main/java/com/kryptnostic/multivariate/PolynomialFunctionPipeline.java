package com.kryptnostic.multivariate;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

//TODO: Make a builder for this class
public class PolynomialFunctionPipeline implements PolynomialFunction {
    private SimplePolynomialFunction [] pipeline;
    
    public PolynomialFunctionPipeline( SimplePolynomialFunction[] pipeline ) {
        pipeline = pipeline;
    }
    
    @Override
    public BitVector apply(BitVector input) {
        return null;
    }

    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getInputLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getOutputLength() {
        // TODO Auto-generated method stub
        return 0;
    }

}
