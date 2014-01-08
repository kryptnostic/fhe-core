package com.kryptnostic.crypto;

import com.kryptnostic.multivariate.PolynomialFunction;

public class PublicKey {
    private final PolynomialFunction encrypter;
    
    public PublicKey( PrivateKey privateKey ) {
        int inputLen =  privateKey.getE1().cols();
        PolynomialFunction input = PolynomialFunction.identity( inputLen );
        PolynomialFunction randInput  = PolynomialFunction.identity( inputLen  );
//        encrypter = privateKey.getE1().multiply( input ).add( privateKey.getE2().multiply( ) );
        encrypter = null;
                
    }
    
    public byte[] encrypt( byte[] plaintext ) {
        return null;
    }
    
}
