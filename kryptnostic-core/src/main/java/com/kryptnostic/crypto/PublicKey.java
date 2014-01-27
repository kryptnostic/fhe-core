package com.kryptnostic.crypto;

import com.kryptnostic.multivariate.MultivariatePolynomialFunction;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

public class PublicKey {
    private final MultivariatePolynomialFunction encrypter;
    
    public PublicKey( PrivateKey privateKey ) {
        int inputLen =  privateKey.getE1().cols();
        int outputLen = privateKey.getE1().rows();
        MultivariatePolynomialFunction input = PolynomialFunctionGF2.identity( outputLen );
//        PolynomialFunction randInput  = PolynomialFunction.identity( inputLen  );
        MultivariatePolynomialFunction R = PolynomialFunctionGF2.randomFunction( outputLen , inputLen );
        MultivariatePolynomialFunction F = PolynomialFunctionGF2.randomFunction( inputLen  , inputLen );
//        E1( F.compose( R )
        /*
         * E(m) = E1(m + F( R(m,r)) ) + E2(R(m,r))
         */
        
//        encrypter = privateKey.getE1().multiply( input ).add( privateKey.getE2().multiply( ) );
        
        encrypter = null;
                
    }
    
    public byte[] encrypt( byte[] plaintext ) {
        return null;
    }
    
}
