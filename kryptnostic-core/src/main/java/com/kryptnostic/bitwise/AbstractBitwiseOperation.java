package com.kryptnostic.bitwise;

import com.google.common.base.Preconditions;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;

import cern.colt.bitvector.BitVector;

public abstract class AbstractBitwiseOperation implements PolynomialFunction {
    private final int length; 
    
    public AbstractBitwiseOperation( int length ) {
        this.length = length >>> 6;
    }

    @Override
    public BitVector apply( BitVector input ) {
        return apply( 
                BitUtils.subVector( input , 0 , input.elements().length >>> 1 ) ,
                BitUtils.subVector( input , input.elements().length >>> 1 , input.elements().length ) );
    }

    @Override
    public BitVector apply( BitVector lhs, BitVector rhs ) {
        Preconditions.checkArgument( 
                lhs.elements().length == length ,
                "Left hand argument has invalid input length.");
        Preconditions.checkArgument( 
                lhs.elements().length == length ,
                "Right hand argument has invalid input length.");
         
        return evaluate( lhs , rhs );
    }
    
    public abstract BitVector evaluate( BitVector lhs, BitVector rhs );
    
    @Override
    public int getInputLength() {
        return length << 1;
    }

    @Override
    public int getOutputLength() {
        return length;
    }
    
}
