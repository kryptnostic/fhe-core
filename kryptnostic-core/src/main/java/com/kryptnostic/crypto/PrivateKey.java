package com.kryptnostic.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * Private key class for decrypting data.
 * @author Matthew Tamayo-Rios
 */
public class PrivateKey {
    private static final Logger logger = LoggerFactory.getLogger( PrivateKey.class );
    private static ObjectMapper mapper = new ObjectMapper();
    private final EnhancedBitMatrix D;
//    private final EnhancedBitMatrix L;
    private final EnhancedBitMatrix E1;
    private final EnhancedBitMatrix E2;
    private final PolynomialFunctionGF2 F;
    private final SimplePolynomialFunction decryptor;
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
        EnhancedBitMatrix e2gen = null ,dgen = null , e1gen = null , lgen = null;
        while( !initialized && ( (--rounds)!=0 ) ) {
            
            /*
             * Loop until valid matrices have been generated.
             */
            try {
                e1gen = EnhancedBitMatrix.randomMatrix( cipherTextBlockLength , plainTextBlockLength );
                dgen = e1gen.getLeftNullifyingMatrix();
                e2gen = dgen.rightGeneralizedInverse();
//                lgen = e2gen.getLeftNullifyingMatrix();
//                lgen = lgen.multiply( e1gen ).inverse().multiply( lgen );  //Normalize
                
                Preconditions.checkState( dgen.multiply( e1gen ).isZero() , "Generated D matrix must nullify E1." );
//                Preconditions.checkState( lgen.multiply( e2gen ).isZero() , "Generated L matrix must nullify E2." );
                Preconditions.checkState( dgen.multiply( e2gen ).isIdentity(), "Generated D matrix must be left generalized inverse of E2." );
//                Preconditions.checkState( lgen.multiply( e1gen ).isIdentity(), "Generated D matrix must be left generalized inverse of E2." );
                
                initialized = true;
                
                logger.info("E1GEN: {} x {}" , e1gen.rows(), e1gen.cols() );
                logger.info("E2GEN: {} x {}" , e2gen.rows(), e2gen.cols() );
                logger.info("DGEN: {} x {}" , dgen.rows(), dgen.cols() );
//                logger.info("LGEN: {} x {}" , lgen.rows(), lgen.cols() );
            } catch (SingularMatrixException e1) {
                continue;
            }
        }

        if( !initialized ) {
            throw new InvalidParameterException("Unable to generate private key. Make sure cipherTextBlockLength > plainTextBlockLength ");
        }
        
        D = dgen;
//        L = lgen;
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
    
    public SimplePolynomialFunction encrypt( SimplePolynomialFunction plaintextFunction ) {
        int plaintextLen =  E1.cols();
        int ciphertextLen = E1.rows();
        PolynomialFunctionGF2 R = PolynomialFunctionGF2.randomFunction( ciphertextLen , plaintextLen );
        
        return E1
                .multiply( plaintextFunction.xor( F.compose( R ) ) )
                .xor( E2.multiply( R ) );
    }
    
    public SimplePolynomialFunction computeHomomorphicFunction( SimplePolynomialFunction f ) {
        return encrypt( f.compose( decryptor ) );
    }
    
    public EnhancedBitMatrix getD() {
        return D;
    }

//    public EnhancedBitMatrix getL() {
//        return L;
//    }

    public EnhancedBitMatrix getE1() {
        return E1;
    }

    public EnhancedBitMatrix getE2() {
        return E2;
    }

    public PolynomialFunctionGF2 getF() {
        return F;
    }

    public byte[] decrypt( byte[] ciphertext ) {
        ByteBuffer buffer = ByteBuffer.wrap( ciphertext );
        ByteBuffer decryptedBytes = ByteBuffer.allocate( ciphertext.length >>> 1);
        while( buffer.hasRemaining() ) {
            BitVector X  = fromBuffer( buffer , longsPerBlock );
            BitVector plaintextVector = decryptor.apply( X );
            toBuffer( decryptedBytes , plaintextVector );
        }
        return decryptedBytes.array();
    }
    
    public SimplePolynomialFunction getDecryptor() {
        return decryptor;
    }
    
    public SimplePolynomialFunction buildDecryptor() throws SingularMatrixException {
        /*
         * DX = R( m , r )
         * Inv( E1 ) X = m +F( R( m , r ) ) + Inv( E1 ) E2 R(m, r) 
         * Inv( E1 ) X + Inv( E1 ) E2 D X = m + F( R( m , r ) )
         * Inv( E1 ) X + Inv( E1 ) E2 D X + F( DX ) = m
         * Inv( E1 ) ( I + E2 D ) X + F( DX ) = m    
         */
        PolynomialFunctionGF2 X = PolynomialFunctionGF2.identity( E1.rows() );
        return E1.leftGeneralizedInverse()
                .multiply( EnhancedBitMatrix.identity( E2.rows() ).add( E2.multiply( D ) ) )
                .multiply( X )
                .xor( F.compose( D.multiply( X ) ) );
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

    public byte[] decryptFromEnvelope(Ciphertext ciphertext) {
        /*
         * Decrypt using the message length to discard unneeded bytes.
         */
        return Arrays.copyOf( 
                decrypt( ciphertext.getContents() ) , 
                (int) decryptor.apply( new BitVector( ciphertext.getLength() , longsPerBlock << 6 ) ).elements()[0] );
    }
       
//    public abstract Object decryptObject( Object object ,  Class<?> clazz );
}
