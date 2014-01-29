package com.kryptnostic.crypto.tests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class KeyTests {
    private static final Logger logger = LoggerFactory.getLogger( KeyTests.class );
    private static final PrivateKey privKey = new PrivateKey( 128 , 64 );
    private static final PublicKey pubKey = new PublicKey( privKey );
    
    @Test
    public void testConstruction() throws SingularMatrixException {
        PrivateKey privKey = new PrivateKey( 128 , 64 );
        PublicKey pubKey = new PublicKey( privKey );
        
        PolynomialFunctionGF2 e = pubKey.getEncrypter();
        PolynomialFunctionGF2 shouldBeR = privKey.getD().multiply( e );
        EnhancedBitMatrix e2null = privKey.getE2().getLeftNullifyingMatrix();
        EnhancedBitMatrix e2nulle1 = e2null.multiply( privKey.getE1() );
        EnhancedBitMatrix e2nulle1inv = e2nulle1.inverse();
        Preconditions.checkState( privKey.getD().multiply( privKey.getE1() ).isZero() , "Generated D matrix must nullify E1." );
        Preconditions.checkState( privKey.getD().multiply( privKey.getE2() ).isIdentity(), "Generated D matrix must be left generalized inverse of E2." );
        Assert.assertTrue( e2null.multiply( privKey.getE2() ).isZero() );
        functionValueEquals( shouldBeR , pubKey.getR() );
        functionValueEquals( e2null.multiply( e ) , e2null.multiply( pubKey.getE1mFoR() ) );
        functionValueEquals( pubKey.getmFoR() , pubKey.getM().add( pubKey.getFofR() ) );
        functionValueEquals( pubKey.getE1mFoR() , privKey.getE1().multiply( pubKey.getmFoR() ) );
        functionValueEquals( pubKey.getE1mFoR() , privKey.getE1().multiply( pubKey.getM().add( pubKey.getFofR() ) ) );
        functionValueEquals( e2null.multiply( pubKey.getE1mFoR() ) , e2nulle1.multiply( pubKey.getM().add( pubKey.getFofR() ) ) );
        functionValueEquals( e2nulle1inv.multiply(  e2null.multiply( e ) ) , pubKey.getM().add( pubKey.getFofR() )) ;
        BitVector v = BitUtils.randomBitVector( 64 );
//        Assert.assertEquals( privKey.getDecryptor().compose( pubKey.getEncrypter() ).evaluate( v ) , v );
        Preconditions.checkState( shouldBeR.equals( pubKey.getR() ) , "Applying nullifying matrix isn't working properly" );
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
        String plaintext = "hey!1234hey!1234hey!1234hey!1234";
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] ciphertext = pubKey.encrypt( plaintextBytes );
        logger.info( "Plaintext: {}", plaintext );
        logger.info( "Ciphertext: {}", new String( ciphertext ) );
        byte[] decryptedBytes = privKey.decrypt( ciphertext ); 
        String decryptedPlaintext = new String( decryptedBytes );
        logger.info( "Decrypted ciphertext: {}" , decryptedPlaintext );
    }
    
    public void functionValueEquals( PolynomialFunctionGF2 f, PolynomialFunctionGF2 g) {
        Assert.assertEquals( f.getInputLength() , g.getInputLength() );
        for( int i = 0 ; i < 1000 ; ++i ) {
            BitVector v = BitUtils.randomBitVector( f.getInputLength() );
            Assert.assertEquals( f.evaluate( v ) , g.evaluate( v ) );
            Preconditions.checkState( f.evaluate(v).equals(g.evaluate(v)) , "Compared functions are not equal.");
        }
    }
    
}
