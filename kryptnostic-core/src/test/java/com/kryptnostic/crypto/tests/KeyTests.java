package com.kryptnostic.crypto.tests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

import junit.framework.Assert;

public class KeyTests {
    private static final Logger logger = LoggerFactory.getLogger( KeyTests.class );
    private static final PrivateKey privKey = new PrivateKey( 256 , 128 );
    private static final PublicKey pubKey = new PublicKey( privKey );
    
    @Test
    public void testConstruction() {
        PrivateKey privKey = new PrivateKey( 256 , 128 );
        PublicKey pubKey = new PublicKey( privKey );
    }
    
    @Test 
    public void testEncryption() {
        String plaintext = "hey!1234hey!1234hey!1234hey!12";
        Assert.assertEquals( (plaintext.getBytes().length << 1) + 4 , pubKey.encrypt( plaintext.getBytes() ).length );
//        logger.info( "Bits available: {}" , bbuf.remaining()*8 );
//        long [] ciphertext = pubKey.encrypt( new long[] { bbuf.getLong() , bbuf.getLong() , bbuf.getLong() , bbuf.getLong() } );
//        logger.debug("A: {}" , ciphertext[0]);
//        logger.debug("B: {}" , ciphertext[1]);
//        logger.debug("C: {}" , ciphertext[2]);
//        logger.debug("D: {}" , ciphertext[3]);
    }
    
    @Test 
    public void testEncryptDecrypt() throws SingularMatrixException {
        String plaintext = "hey!1234hey!1234hey!1234hey!12";
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] ciphertext = pubKey.encrypt( plaintextBytes );
        logger.info( "Plaintext: {}", plaintext );
        logger.info( "Ciphertext: {}", new String( ciphertext ) );
        String decryptedPlaintext = new String( privKey.decrypt( ciphertext ) );
        logger.info( "Decrypted ciphertext: {}" , decryptedPlaintext );
        
    }
    
}
