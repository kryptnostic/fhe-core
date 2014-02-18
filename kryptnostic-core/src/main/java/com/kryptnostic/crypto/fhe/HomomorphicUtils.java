package com.kryptnostic.crypto.fhe;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class HomomorphicUtils {
    public static SimplePolynomialFunction leftEmbedding( EnhancedBitMatrix D , SimplePolynomialFunction f ) {
        return D.multiply( f );
    }
}
