fhe-core
===========

Libraries for fully homomorphic encryption.


Overview
--------
These are our core client side cryptography classes in Java. The following is included:

- <code>EnhancedBitMatrix</code>, an alternate to the COLT bit matrix implementing commong linear operations over GF(2).

- <code>Monomial</code>, an extension of the COLT <code>BitVector</code> representation of an individual monomial term over GF(2), such as x<sub>1</sub>x<sub>3</sub>x<sub>4</sub>. For a monomial of length n, each bit 0 <= i < n represents the presence of x<sub>i</sub> in the monomial. This implementations extends the CERN bitvector class.

- <code>PolynomialFunctionRepresentationGF2</code>, a representation of a vectorial polynomial function over GF(2). Also contains Jackson serialization for marshalling the object as JSON.


- <code>PolynomialFunctionGF2</code>, an extendsion of <code>PolynomialFunctionRepresentationGF2</code>, contains the logic for evaluating, composing, adding, and producting polynomial functions over GF(2).

- <code>PrivateKey</code> contains the logic for generating private keys and encrypting other polynomial functions efficiently, as compose is a very expensive operations.

Getting Started
---------------
Clone the project and build it.  This will also run unit tests to make sure nothing is broken.

	> git clone https://github.com/kryptnostic/fhe-core
	> ./gradlew build

Setup for your IDE of choice:
	
	> ./gradlew eclipse
	
Alternatively, if you like IntelliJ:

	> ./gradlew idea	
	
Enjoy!

Usage
-----

###Key Generation
		
	int ciphertextLength = 128 ,
		plaintextLength  = 64; 
			
	PrivateKey privateKey = new PrivateKey( ciphertextLength , plaintextLength );
	PublicKey publicKey   = new PublicKey( privateKey );

####Usage Notes

<code>BitVector</code>'s internal representation is a long[] array and we've taken an implementation dependency on that internal representation. Use 

###Encryption and Decryption
		
####Raw 		

	byte[] plaintext  = "This is a test plaintext.".getBytes();
	byte[] ciphertext = publicKey.encrypt( ciphertext );
		
	String decryptedText = new String( privateKey.decrypt( ciphertext ) );
		
####Enveloped

	byte[] plaintext  = "This is a test plaintext.".getBytes();
	Ciphertext ciphertext = publicKey.encryptIntoEnvelope( ciphertext );
	
	String decryptedText = new String( privateKey.decryptFromEnvelope( ciphertext ) ); 


####Usage Notes
Raw encryption automatically pads on encryption, as required by the underlying implementation.  Currently the only implemented and this the default padding scheme is zero padding. This padding is not removed.

When you encrypt enveloped data it also encrypted the length of the data along with the data itself. This enables things like string concatenation and automatically recovering the original bytestream exactly.

###Homomorphic Operations

In order to do homomorphic operations you first have to represent the desired function a vector function over GF(2). A simple example we have include is for homomorphic xor.

	PolynomialFunctionGF2 xor = PolynomialFunctionGF2.XOR( 64 );
    PolynomialFunctionGF2 hXor = privateKey.encrypt( xor.compose( privKey.getDecry) );
	BitVector v = BitUtils.randomBitVector( 64 );
    BitVector vConcatR = new BitVector( new long[] { 
                v.elements()[ 0 ] ,
                r.nextLong() } ,  
                128 );
        
    BitVector cv = publicKey.getEncrypter().evaluate( vConcatR );
    BitVector hResult = privateKey.getDecryptor().evaluate( hXor.evaluate( cv ) );
    BitVector result = xor.evaluate( v );
       
    Assert.assertEquals( hResult, result );

####Usage 		

See `src/test/java/com/kryptnostic/multivariate/test/SpecialPolynomialFunctionsTests.java`.
Notes that this evaluates the homomorphic circuits in a unary closed fashion.  It XORs the 32 lower bits with the 32 higher bits. We are working on binary implementations, as well as efficient implementations for AND ( which requires some algebriac tricks to reduce ~ 8 XORs ), left shift, right shift, and negation.  Once these are released they form a complete set of operators allowing any boolean circuit to be evaluated homomorphically by composing the circuits.


Not Production Ready
--------------------
This code isn't production ready. As we're moving very quickly to get this out so other could play with it, we took some shortcuts, which we will be remedying.  A few of our "anti-patterns":

- Rolling our own crypto

- Rolling our own numerics. Couldn't find any GF(2) numerics and we'd like to contribute our extensions back to COLT once the implementations are cleaner.
	
- Not using a cryptographically secure PRNG. We are planning on moving to BouncyCastle ASAP.

- Current monomial count in randomized polynomial function generating code is set low.  The more monomials and the higher the order of the monomials the longer operations take.  This means running unit tests can take 20 seconds instead of 2 seconds.  We're planning on refactoring these classes, so we can control aggressiveness of random polynomial function generation so unit tests don't take forever to run.

- Not every function has unit tests, but we have fairly decent coverage on the most important functions.




More Information
----------------

http://blog.kryptnostic.com/
