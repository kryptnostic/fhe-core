package com.kryptnostic.crypto;

import java.nio.ByteBuffer;
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
    private final PolynomialFunctionGF2 decryptor;
    private final int longsPerBlock;
    
    /**
     * Construct a private key instance that can be used for decrypting data encrypted with the public key.
     * @param cipherTextBlockLength Length of the ciphertext output block, should be multiples of 64 bits 
     * @param plainTextBlockLength Length of the ciphertext output block, should be multiples of 64 bits
     * @throws SingularMatrixException 
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
                logger.info("E2GEN: {} x {}" , e2gen.rows(), e2gen.cols() );
                logger.info("DGEN: {} x {}" , dgen.rows(), dgen.cols() );
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
        F = PolynomialFunctionGF2.randomFunction( plainTextBlockLength , plainTextBlockLength );
        try {
            decryptor = buildDecryptor();
        } catch (SingularMatrixException e) {
            logger.error("Unable to generate decryptor function due to a singular matrix exception during generation process.");
            throw new InvalidParameterException("Unable to generate decryptor function for private key.");
        }
        longsPerBlock = cipherTextBlockLength >>> 6;
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
        ByteBuffer buffer = ByteBuffer.wrap( ciphertext );
        ByteBuffer decryptedBytes = ByteBuffer.allocate( ciphertext.length >>> 1);
        while( buffer.hasRemaining() ) {
            /*
             * DX = R( m , r )
             * Inv( E1 ) X = m +F( R( m , r ) ) + Inv( E1 ) E2 R(m, r) 
             * Inv( E1 ) X + Inv( E1 ) E2 D X = m + F( R( m , r ) )
             * Inv( E1 ) X + Inv( E1 ) E2 D X + F( DX ) = m
             * Inv( E1 ) ( I + E2 D ) X + F( DX ) = m    
             */
//            EnhancedBitMatrix id = EnhancedBitMatrix.identity( E2.rows() );
            BitVector X  = fromBuffer( buffer , longsPerBlock );
//            BitVector plaintextVector = E1.leftGeneralizedInverse().multiply( id.add( E2.multiply( D ) ).multiply( X ) );
//            plaintextVector.xor( F.evaluate( D.multiply( X ) ) );
            BitVector plaintextVector = decryptor.evaluate( X );
            toBuffer( decryptedBytes , plaintextVector );
        }
        return decryptedBytes.array();
    }
    
    public PolynomialFunctionGF2 getDecryptor() {
        return decryptor;
    }
    
    public PolynomialFunctionGF2 buildDecryptor() throws SingularMatrixException {
        EnhancedBitMatrix id = EnhancedBitMatrix.identity( E2.rows() );
        PolynomialFunctionGF2 X = PolynomialFunctionGF2.identity( E1.rows() );
        PolynomialFunctionGF2 DX = D.multiply( X );
//        return E1.leftGeneralizedInverse().multiply( X.add( E2.multiply( DX ) ) ).add( F.compose( DX ) );
        return E1.leftGeneralizedInverse().multiply( id.add( E2.multiply( D ) ).multiply( X ) ).add( F.compose( DX ) );
    }
    
    protected static void toBuffer( ByteBuffer output , BitVector plaintextVector ) {
        long[] plaintextLongs = plaintextVector.elements();
        for( long l : plaintextLongs ) {
            output.putLong( l );
        }
    }
    
    protected static BitVector fromBuffer( ByteBuffer buffer , int longsPerBlock ) {
        long [] cipherLongs = new long[ longsPerBlock ];
        for( int i = 0 ; i < longsPerBlock ; ++i ) {
            cipherLongs[i] = buffer.getLong();
            logger.debug("Read the following ciphertext: {}", cipherLongs[i]);
        }
        
        return new BitVector( cipherLongs , longsPerBlock << 6 );
    }
    
//    public abstract byte[] encryptObject( Object object );   
//    public abstract Object decryptObject( Object object ,  Class<?> clazz );
}
