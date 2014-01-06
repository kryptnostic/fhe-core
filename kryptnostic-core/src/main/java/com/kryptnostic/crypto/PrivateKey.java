package com.kryptnostic.crypto;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.PolynomialFunction;

public class PrivateKey {
    private final EnhancedBitMatrix D;
    private final EnhancedBitMatrix E1;
    private final EnhancedBitMatrix E2;
    private final PolynomialFunction F;
    
    public PrivateKey( int cipherTextBlockLength , int plainTextBlockLength ) {
        D = EnhancedBitMatrix.randomSqrMatrix(size)
    }
}
