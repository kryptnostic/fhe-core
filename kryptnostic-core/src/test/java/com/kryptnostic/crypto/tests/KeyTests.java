package com.kryptnostic.crypto.tests;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.crypto.Ciphertext;
import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class KeyTests {
    private static final Logger logger = LoggerFactory.getLogger( KeyTests.class );
    private static final PrivateKey privKey = new PrivateKey( 128 , 64 );
	private static final SimplePolynomialFunction decryptor = privKey.getDecryptor();
    private static final PublicKey pubKey = new PublicKey( privKey );
    private static final SimplePolynomialFunction encryptor = pubKey.getEncrypter();
    
    private static final Integer LENGTH = 64;

    
    @Test
    public void testConstruction() throws SingularMatrixException {
        PrivateKey privKey = new PrivateKey( 128 , 64 );
        PublicKey pubKey = new PublicKey( privKey );
        logger.info("Finished generating key pair. Starting assumption tests...");

        SimplePolynomialFunction e = pubKey.getEncrypter();
        SimplePolynomialFunction LplusDX = privKey.getL().add( privKey.getD() ).multiply( e );
        SimplePolynomialFunction expected = privKey.getA().add( privKey.getB()  ).multiply( privKey.getG() ) ;
        SimplePolynomialFunction GofX =  (privKey.getA().add( privKey.getB() ) ).inverse().multiply( privKey.getL().add( privKey.getD() ).multiply( PolynomialFunctions.identity( e.getOutputLength() ) ) );
        BitVector sample = BitUtils.randomVector( 128 );
        BitVector enc = e.apply( sample );
        BitVector aV = privKey.getL().add( privKey.getD() ).multiply( e ).apply( sample );
        BitVector eV = expected.apply( sample ); 
        Assert.assertEquals( eV , aV );
        Assert.assertEquals( privKey.getG().apply( sample ) , GofX.apply( enc ) );
        
        BitVector dec = privKey.getDecryptor().apply( enc );
        sample.setSize( dec.size() );
        Assert.assertEquals(  dec , sample );
    }
    
    @Test 
    public void testEncryptDecrypt() throws SingularMatrixException {
        String plaintext = "hey!1234hey!1234hey!1234hey!12";
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] ciphertext = pubKey.encrypt( plaintextBytes );
        logger.trace( "Plaintext: {}", plaintext );
        logger.trace( "Ciphertext: {}", new String( ciphertext ) );
        byte[] decryptedBytes = privKey.decrypt( ciphertext ); 
        String decryptedPlaintext = new String( decryptedBytes );
        logger.trace( "Decrypted ciphertext: {}" , decryptedPlaintext );
        Assert.assertTrue( StringUtils.startsWith( decryptedPlaintext , plaintext ) );
    }
    
    @Test 
    public void testEncryptDecryptWithEnvelope() {
        String plaintext = "hey!1234hey!1234hey!1234hey!1";
        byte[] plaintextBytes = plaintext.getBytes();
        Ciphertext ciphertext = pubKey.encryptIntoEnvelope( plaintextBytes );
        byte[] decryptedBytes = privKey.decryptFromEnvelope( ciphertext ); 
        String decryptedPlaintext = new String( decryptedBytes );
        logger.trace( "Decrypted ciphertext: {}" , decryptedPlaintext );
        Assert.assertEquals( decryptedPlaintext , plaintext );
    }
    
    @Test
    public void testComputeHomomorphicFunctions() {
    	SimplePolynomialFunction identity = PolynomialFunctions.identity( LENGTH );
    	SimplePolynomialFunction homomorphicFunction = privKey.computeHomomorphicFunction( identity );
    	
    	for (int i = 0; i < 100; i++) {
			BitVector plainText = BitUtils.randomVector(LENGTH);
			
			// pad the input to encryptor if necessary
			BitVector extendedPlainText = plainText.copy();
			if (encryptor.getInputLength() > plainText.size()) {
				extendedPlainText.setSize( encryptor.getInputLength() );
			}
			BitVector cipherText = encryptor.apply(extendedPlainText);
			BitVector cipherResult = homomorphicFunction.apply(cipherText);
			BitVector found = decryptor.apply(cipherResult);
			
			Assert.assertEquals(found, plainText);
		}
    }
    
    // TODO uncomment when bug in concatenate ParameterizedPolynomialFunctionGF2 is fixed
//    @Test
//    public void testComputeBinaryHomomorphicFunction() {
//    	SimplePolynomialFunction binaryXor = PolynomialFunctions.BINARY_XOR( LENGTH );
//    	SimplePolynomialFunction concatenated = FunctionUtils.concatenateInputsAndOutputs(decryptor, decryptor);
//    	SimplePolynomialFunction composed = binaryXor.compose( concatenated );
//    	
//    	SimplePolynomialFunction homomorphicBinaryXor = privKey.encryptBinary( composed );
//    }

}
