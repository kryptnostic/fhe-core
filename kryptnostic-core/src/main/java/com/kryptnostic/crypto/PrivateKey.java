package com.kryptnostic.crypto;

import java.security.InvalidParameterException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.MultivariatePolynomialFunction;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

public class PrivateKey {
    private static ObjectMapper mapper = new ObjectMapper();
    private final EnhancedBitMatrix D;
    private final EnhancedBitMatrix E1;
    private final EnhancedBitMatrix E2;
    private final MultivariatePolynomialFunction F;
    
    /**
     * Construct a private key instance that can be used for decrypting data encrypted with the public key.
     * @param cipherTextBlockLength Length of the ciphertext output block, should be multiples of 64 bits 
     * @param plainTextBlockLength Length of the ciphertext output block, should be multiples of 64 bits
     */
    public PrivateKey( int cipherTextBlockLength , int plainTextBlockLength ) {
        boolean initialized = false;
        int rounds = 100000;
        EnhancedBitMatrix e2gen = null ,dgen = null , e1gen = null;
        while( !initialized && ( (--rounds)!=0 ) ) {
            dgen = EnhancedBitMatrix.randomMatrix( plainTextBlockLength , cipherTextBlockLength );
            e1gen = dgen.getNullspaceBasis();
            /*
             * Loop until valid e1 has been generated.
             */
            if( e1gen.rows() != cipherTextBlockLength ) {
                continue;
            }
            
            /*
             * Loop until a D with right generalizedInverse is found
             */
            try {
                e2gen = dgen.rightGeneralizedInverse();
                initialized = true;
            } catch (SingularMatrixException e) {
                continue;
            }
        }
        
        if( !initialized ) {
            throw new InvalidParameterException("Unable to generate private key. Make sure cipherTextBlockLength > plainTextBlockLength ");
        }
        
        D = dgen;
        E1 = e1gen;
        E2 = e2gen;
        
        F = PolynomialFunctionGF2.randomFunction( cipherTextBlockLength , plainTextBlockLength );
    }
    
    public EnhancedBitMatrix getD() {
        return D;
    }

    public EnhancedBitMatrix getE1() {
        return E1;
    }

    public EnhancedBitMatrix getE2() {
        return E2;
    }

    public MultivariatePolynomialFunction getF() {
        return F;
    }

    public byte[] decrypt( byte[] ciphertext ) {
        return null;
    }
    
//    public abstract byte[] encryptObject( Object object );   
//    public abstract Object decryptObject( Object object ,  Class<?> clazz );
}
