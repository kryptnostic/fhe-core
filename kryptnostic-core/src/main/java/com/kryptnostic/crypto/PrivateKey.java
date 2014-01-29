package com.kryptnostic.crypto;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.security.InvalidParameterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

import cern.colt.bitvector.BitVector;

public class PrivateKey {
    private static final Logger logger = LoggerFactory.getLogger( PrivateKey.class );
    private static ObjectMapper mapper = new ObjectMapper();
    private final EnhancedBitMatrix D;
    private final EnhancedBitMatrix E1;
    private final EnhancedBitMatrix E2;
    private final PolynomialFunctionGF2 F;
    
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
            
            /*
             * Loop until valid matrices have been generated.
             */
            try {
                e1gen = EnhancedBitMatrix.randomMatrix( cipherTextBlockLength , plainTextBlockLength );
                dgen = e1gen.getLeftNullifyingMatrix();
                e2gen = dgen.rightGeneralizedInverse();
                
                Preconditions.checkState( dgen.multiply( e1gen ).isZero() , "Generated D matrix must nullify E1." );
                Preconditions.checkState( dgen.multiply( e2gen ).isIdentity(), "Generated D matrix must be left generalized inverse of E2." );
                
                initialized = true;
                
                logger.info("E1GEN: {} x {}" , e1gen.rows(), e1gen.cols() );
                logger.info("E2GEN: {} x {}" , e1gen.rows(), e1gen.cols() );
                logger.info("DGEN: {} x {}" , e1gen.rows(), e1gen.cols() );
            } catch (SingularMatrixException e1) {
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

    public PolynomialFunctionGF2 getF() {
        return F;
    }

    public byte[] decrypt( byte[] ciphertext ) throws SingularMatrixException {
        /*
         * DX = R( m , r )
         * Inv( E1 ) X = m +F( R( m , r ) ) + Inv( E1 ) E2 R(m, r) 
         * Inv( E1 ) X + Inv( E1 ) E2 D X = m + F( R( m , r ) )
         * Inv( E1 ) X + Inv( E1 ) E2 D X + F( DX ) = m
         * Inv( E1 ) ( I + E2 D ) X + F( DX ) = m    
         */
        EnhancedBitMatrix id = EnhancedBitMatrix.identity( E2.rows() );
        BitVector X  = fromCipherText( ciphertext );
        BitVector plaintextVector = E1.leftGeneralizedInverse().multiply( id.add( E2.multiply( D ) ).multiply( X ) );
        plaintextVector.xor( F.evaluate( D.multiply( X ) ) );;
        return toPlaintext( plaintextVector );
    }
    
    protected static byte[] toPlaintext( BitVector plaintextVector ) {
        ByteBuffer buf = ByteBuffer.allocate( plaintextVector.size() >>> 3 );
        for( long l : plaintextVector.elements() ) {
            buf.putLong( l );
        }
        return buf.array();
    }
    protected static BitVector fromCipherText( byte[] ciphertext ) {
        LongBuffer lBuf = LongBuffer.allocate( ciphertext.length /8 ).put( ByteBuffer.wrap( ciphertext ).asLongBuffer() );
        return new BitVector( lBuf.array() , ciphertext.length << 3 );
    }
//    public abstract byte[] encryptObject( Object object );   
//    public abstract Object decryptObject( Object object ,  Class<?> clazz );
}
