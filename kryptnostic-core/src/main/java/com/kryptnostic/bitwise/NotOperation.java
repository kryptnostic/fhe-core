package com.kryptnostic.bitwise;

import cern.colt.bitvector.BitVector;

public class NotOperation extends AbstractBitwiseOperation {

    public NotOperation(int length) {
        super(length);
    }

    @Override
    public BitVector apply(BitVector input) {
        BitVector result = input.copy();
        result.not();
        return result;
    }
    
    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        throw new UnsupportedOperationException( "Binary operation is not supported on NOT. ");
    }
    
    @Override
    public BitVector evaluate(BitVector lhs, BitVector rhs) {
        //Unreachable, since apply( lhs , rhs ) always throws.
        return null;
    }

}
