package com.kryptnostic.crypto;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.NonSquareMatrixException;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.FunctionUtils;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class KeyTests {
    private static final Logger logger = LoggerFactory.getLogger( KeyTests.class );
    private static PrivateKey privKey;
    private static PublicKey pubKey;
	private static SimplePolynomialFunction decryptor;
    private static SimplePolynomialFunction encryptor;
    
    private static final Integer LENGTH = 64;

    @BeforeClass
    public static void generateKeys() {
        privKey = new PrivateKey( 128 , 64 );
        pubKey = new PublicKey( privKey );
        decryptor = privKey.getDecryptor();
        encryptor = pubKey.getEncrypter();
    }
    
    @Test
    public void testConstruction() throws SingularMatrixException {
        logger.info("Starting assumption tests...");

        SimplePolynomialFunction e = pubKey.getEncrypter();
        SimplePolynomialFunction LplusDX = privKey.getL().add( privKey.getD() ).multiply( e );
        SimplePolynomialFunction expected = privKey.getA().add( privKey.getB()  ).multiply( privKey.getG() ) ;
        SimplePolynomialFunction GofX =  (privKey.getA().add( privKey.getB() ) ).inverse().multiply( privKey.getL().add( privKey.getD() ).multiply( SimplePolynomialFunctions.identity( e.getOutputLength() ) ) );
        BitVector sample = BitVectors.randomVector( 128 );
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
        byte[] ciphertext = pubKey.encrypt(plaintextBytes);
        logger.trace("Plaintext: {}", plaintext);
        logger.trace("Ciphertext: {}", new String(ciphertext));
        byte[] decryptedBytes = privKey.decrypt(ciphertext);
        String decryptedPlaintext = new String(decryptedBytes);
        logger.trace("Decrypted ciphertext: {}", decryptedPlaintext);
        Assert.assertTrue(StringUtils.startsWith(decryptedPlaintext, plaintext));
    }

    @Test
    public void testEncryptDecryptWithEnvelope() {
        String plaintext = "hey!1234hey!1234hey!1234hey!1";
        byte[] plaintextBytes = plaintext.getBytes();
        Ciphertext ciphertext = pubKey.encryptIntoEnvelope(plaintextBytes);
        byte[] decryptedBytes = privKey.decryptFromEnvelope(ciphertext);
        String decryptedPlaintext = new String(decryptedBytes);
        logger.trace("Decrypted ciphertext: {}", decryptedPlaintext);
        Assert.assertEquals(decryptedPlaintext, plaintext);
        Assert.assertArrayEquals(decryptedPlaintext.getBytes(), plaintext.getBytes());
    }

    @Test
    public void testComputeHomomorphicFunctions() {
        SimplePolynomialFunction identity = SimplePolynomialFunctions.identity(LENGTH);
        SimplePolynomialFunction homomorphicFunction = privKey.computeHomomorphicFunction(identity);

        for (int i = 0; i < 100; i++) {
            BitVector plainText = BitVectors.randomVector(LENGTH);

            // pad the input to encryptor if necessary
            BitVector extendedPlainText = plainText.copy();
            if (encryptor.getInputLength() > plainText.size()) {
                extendedPlainText.setSize(encryptor.getInputLength());
            }
            BitVector cipherText = encryptor.apply(extendedPlainText);
            BitVector cipherResult = homomorphicFunction.apply(cipherText);
            BitVector found = decryptor.apply(cipherResult);

            Assert.assertEquals(found, plainText);
        }
    }

    @Test
    public void testComputePartialHomomorphism() {
        SimplePolynomialFunction mvq = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(256, 64);
        SimplePolynomialFunction composed = mvq.partialComposeLeft(decryptor);

        BitVector plaintext = BitVectors.randomVector(256);
        BitVector rhPlaintext = BitVectors.subVector(plaintext, 1, 4);
        long[] backingLhPlaintext = { plaintext.elements()[0] };
        BitVector lhPlaintext = new BitVector(backingLhPlaintext, 64);

        BitVector lhCipher = encryptor.apply(lhPlaintext, BitVectors.randomVector(64));

        BitVector expected = mvq.apply(plaintext);
        BitVector actual = composed.apply(FunctionUtils.concatenate(lhCipher, rhPlaintext));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testEncryptDecryptAgain() {
        BitVector plainText = BitVectors.randomVector(64);
        BitVector cipherText = encryptor.apply(plainText, BitVectors.randomVector(64));
        BitVector recovered = decryptor.apply(cipherText);
        Assert.assertEquals(plainText, recovered);
    }
    
    @Test
    public void testOrthogonalEmbedding() throws NonSquareMatrixException, SingularMatrixException {
        EnhancedBitMatrix E1 = EnhancedBitMatrix.randomMatrix( 128 , 64 );
        EnhancedBitMatrix E2 = E1.getLeftNullifyingMatrix().rightInverse();
        boolean notGenerated = true;
        while( notGenerated ) {
            try{ 
                E1.leftInverse();
                E2.leftInverse();
            } catch ( SingularMatrixException e ) {
                E1 = EnhancedBitMatrix.randomMatrix( 128 , 64 );
                E2 = E1.getLeftNullifyingMatrix().rightInverse();              
                continue;
            }
            notGenerated = false;
        }
//        Assert.assertTrue( EnhancedBitMatrix.determinant( E1.transpose().multiply( E2 ) ) );
        
        EnhancedBitMatrix L = PrivateKey.buildL( E1 , E2 );
    }
    // TODO uncomment when bug in concatenate ParameterizedPolynomialFunctionGF2 is fixed
    // @Test
    // public void testComputeBinaryHomomorphicFunction() {
    // SimplePolynomialFunction binaryXor = SimplePolynomialFunctions.BINARY_XOR( LENGTH );
    // SimplePolynomialFunction concatenated = FunctionUtils.concatenateInputsAndOutputs(decryptor, decryptor);
    // SimplePolynomialFunction composed = binaryXor.compose( concatenated );
    //
    // SimplePolynomialFunction homomorphicBinaryXor = privKey.encryptBinary( composed );
    // }

}
