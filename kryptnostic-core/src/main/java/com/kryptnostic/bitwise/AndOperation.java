package com.kryptnostic.bitwise;

import cern.colt.bitvector.BitVector;

public class AndOperation extends AbstractBitwiseOperation {

    public AndOperation(int length) {
        super(length);
    }

    @Override
    public BitVector evaluate(BitVector lhs, BitVector rhs) {
        BitVector result = lhs.copy();
        result.and( rhs );
        return result;
    }

}
