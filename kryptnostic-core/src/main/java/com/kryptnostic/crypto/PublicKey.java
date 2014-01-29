package com.kryptnostic.crypto;

import java.nio.ByteBuffer;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.kryptnostic.crypto.padding.PaddingStrategy;
import com.kryptnostic.crypto.padding.ZeroPaddingStrategy;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

import cern.colt.bitvector.BitVector;

public class PublicKey {
    private static final Logger logger = LoggerFactory.getLogger( PublicKey.class );
    //TODO: Replace with bouncy castle or real number generator.
    private static final Random r = new Random();
    private final PolynomialFunctionGF2 encrypter;
    private final PaddingStrategy paddingStrategy;
    private final int longsPerBlock;
    public PublicKey( PrivateKey privateKey ) {
        this( privateKey, new ZeroPaddingStrategy() );
    }
    public PublicKey( PrivateKey privateKey , PaddingStrategy paddingStrategy ) {
        this.paddingStrategy = paddingStrategy;
        int inputLen =  privateKey.getE1().cols();
        int outputLen = privateKey.getE1().rows();
        PolynomialFunctionGF2 m = PolynomialFunctionGF2.truncatedIdentity( inputLen , outputLen );
        logger.debug( "m: {} -> {}" , inputLen , outputLen );
        PolynomialFunctionGF2 R = PolynomialFunctionGF2.randomFunction( outputLen , inputLen );
        PolynomialFunctionGF2 F = PolynomialFunctionGF2.randomFunction( inputLen  , inputLen );
        PolynomialFunctionGF2 FofR = F.compose( R );
        
        /*
         * E(m) = E1(m + F( R(m,r)) ) + E2(R(m,r))
         */
        
        encrypter = privateKey.getE1().multiply( m.add( FofR ) ).add( privateKey.getE2().multiply( R ) );
        logger.debug("Required input length in bits: {}" , encrypter.getInputLength() );
        // 8 bits per byte, 8 bytes per long.
        longsPerBlock = encrypter.getInputLength() /  128;
    }
    
    public byte[] encrypt( byte[] plaintext ) {
        Preconditions.checkNotNull( plaintext , "Plaintext to be encrypted cannot be null." );
        
        /* 
         * 1) Pad the data so it aligns 
         */
        plaintext = paddingStrategy.pad( plaintext );
               
        ByteBuffer buffer = ByteBuffer.wrap( plaintext );
        ByteBuffer outBuf = ByteBuffer.allocate( plaintext.length << 1 );
        
        int blockLen = longsPerBlock << 1;
        while( buffer.remaining() > 0 ) {
            long[] lpt = new long[ blockLen ];
            
            for( int i = 0 ; i < longsPerBlock; ++i ) {
               lpt[i] = buffer.getLong();
            }
            
            for( int i = longsPerBlock; i < blockLen ; ++i ) {
               lpt[i] = r.nextLong();
            }
            
            long[] ciphertext = encrypt( lpt );
            for( long lct : ciphertext ) {
                outBuf.putLong( lct );
            }
        }
        
        return outBuf.array();
    }
    
    public long[] encrypt( long[] plaintext ) {
        logger.debug( "Expected plaintext block length: {}" , encrypter.getInputLength() );
        logger.debug( "Observed plaintext block length: {}" , plaintext.length * 8 * 8 );
        Preconditions.checkArgument( (plaintext.length<<3) == ( encrypter.getInputLength() >>> 3 ) , "Cannot directly encrypt block of incorrect length." );
        
        BitVector result = encrypter.evaluate( new BitVector( plaintext , encrypter.getInputLength() ) );
        return result.elements();
    }
    
}
