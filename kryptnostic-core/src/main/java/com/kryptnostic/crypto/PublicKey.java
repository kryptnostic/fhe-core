package com.kryptnostic.crypto;

import com.kryptnostic.multivariate.PolynomialFunction;

public class PublicKey {
    private final PolynomialFunction encrypter;
    
    public PublicKey( PrivateKey privateKey ) {
        int inputLen =  privateKey.getE1().cols();
        int outputLen = privateKey.getE1().rows();
        PolynomialFunction input = PolynomialFunction.identity( outputLen );
//        PolynomialFunction randInput  = PolynomialFunction.identity( inputLen  );
        PolynomialFunction R = PolynomialFunction.randomFunction( outputLen , inputLen );
        PolynomialFunction F = PolynomialFunction.randomFunction( inputLen  , inputLen );
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
