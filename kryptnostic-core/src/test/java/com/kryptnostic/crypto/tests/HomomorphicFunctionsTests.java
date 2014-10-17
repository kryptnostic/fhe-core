package com.kryptnostic.crypto.tests;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;
import com.kryptnostic.crypto.fhe.HomomorphicFunctions;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;


public class HomomorphicFunctionsTests {
	private static final PrivateKey privKey = new PrivateKey( 128 , 64 );
	private static final SimplePolynomialFunction decryptor = privKey.getDecryptor();
	private static final PublicKey pubKey = new PublicKey( privKey );
	private static final SimplePolynomialFunction encryptor = pubKey.getEncrypter();
    private static final Integer LENGTH = 64;
	private static final Integer N_TESTS = 1000;
	
	@Test
	public void homomorphicXorTest() {
		SimplePolynomialFunction homomorphicXor = HomomorphicFunctions.HomomorphicXor(LENGTH, privKey);
		SimplePolynomialFunction xor = PolynomialFunctions.XOR(LENGTH);
		
		testHomomorphicFunction(xor, homomorphicXor);
	}
	
	@Test
	public void homomorphicAndTest() {
		SimplePolynomialFunction homomorphicAnd = HomomorphicFunctions.HomomorphicAnd(LENGTH, privKey);
		SimplePolynomialFunction and = PolynomialFunctions.AND( LENGTH );
		
		testHomomorphicFunction(and, homomorphicAnd);
	}
	
	@Test
	public void homomorphicLshTest() {
		SimplePolynomialFunction homomorphicLsh = HomomorphicFunctions.HomomorphicLsh(LENGTH, privKey);
		SimplePolynomialFunction lsh = PolynomialFunctions.LSH( LENGTH, 1);
		
		testHomomorphicFunction(lsh, homomorphicLsh);
	}
	// TODO Complete binary h-function testing when bug in composeBinaryFunction is fixed.
//	@Test
	public void homomorphicBinaryXorTest() {
		SimplePolynomialFunction homomorphicBinaryXor = HomomorphicFunctions.BinaryHomomorphicXor(LENGTH, privKey);
		SimplePolynomialFunction binaryXor = PolynomialFunctions.BINARY_XOR(LENGTH);
		
		testBinaryHomomorphicFunction(binaryXor, homomorphicBinaryXor);
	}
	
	private void testHomomorphicFunction(SimplePolynomialFunction function, 
			SimplePolynomialFunction homomorphicFunction) {
		
		for (int i = 0; i < N_TESTS; i++) {
			BitVector plainText = BitVectors.randomVector(LENGTH);
			
			// pad the input to encryptor if necessary
			BitVector extendedPlainText = plainText.copy();
			if (encryptor.getInputLength() > plainText.size()) {
				extendedPlainText.setSize( encryptor.getInputLength() );
			}
			BitVector cipherText = encryptor.apply(extendedPlainText);
			
			BitVector expected = function.apply(plainText);
			BitVector cipherResult = homomorphicFunction.apply(cipherText);
			BitVector found = decryptor.apply(cipherResult);
			
			Assert.assertEquals(found, expected);
		}
	}
	
	private void testBinaryHomomorphicFunction(SimplePolynomialFunction function,
			SimplePolynomialFunction homomorphicFunction) {
		for (int i = 0; i < N_TESTS; i++) {
			BitVector plainText1 = BitVectors.randomVector(LENGTH);
			BitVector extendedPlainText1 = plainText1.copy();
			extendedPlainText1.setSize( LENGTH << 1);
			BitVector cipherText1 = encryptor.apply(extendedPlainText1);
			
			BitVector plainText2 = BitVectors.randomVector(LENGTH);
			BitVector extendedPlainText2 = plainText2.copy();
			extendedPlainText2.setSize( LENGTH << 1);
			BitVector cipherText2 = encryptor.apply(extendedPlainText2);
			
			BitVector expected = function.apply(plainText1, plainText2);
			BitVector cipherResult = homomorphicFunction.apply(cipherText1, cipherText2);
			BitVector found = decryptor.apply(cipherResult);
			found.setSize( LENGTH );
			
			Assert.assertEquals(found, expected);
		}
	}
}