package com.kryptnostic.multivariate;

import com.kryptnostic.bitwise.AbstractBitwiseOperation;

import cern.colt.bitvector.BitVector;

public class XorOperation extends AbstractBitwiseOperation {
    public XorOperation(int length) {
        super(length);
    }

    @Override
    public BitVector evaluate(BitVector lhs, BitVector rhs) {
        BitVector result = lhs.copy();
        result.xor( rhs  );
        return result;
    }
}
