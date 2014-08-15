package com.kryptnostic.bitwise;

import cern.colt.bitvector.BitVector;

public class OrOperation extends AbstractBitwiseOperation {

    public OrOperation(int length) {
        super(length);
    }

    @Override
    public BitVector evaluate(BitVector lhs, BitVector rhs) {
        BitVector result = lhs.copy();
        result.or( rhs );
        return result;
    }

}
