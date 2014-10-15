package com.kryptnostic.crypto;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.NonSquareMatrixException;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class EncryptedSearchPrivateKeyTests {
    private static final HashFunction hf = Hashing.murmur3_128();
    private static EncryptedSearchPrivateKey privateKey;
    private static PrivateKey fhePrivateKey;
    private static PublicKey fhePublicKey;
    private static SimplePolynomialFunction globalHash = PolynomialFunctions.denseRandomMultivariateQuadratic( 128 + 64  , 64 );
    
    @BeforeClass
    public static void generateKey() throws SingularMatrixException {
        privateKey = new EncryptedSearchPrivateKey();
        fhePrivateKey = new PrivateKey(128,64);
        fhePublicKey = new PublicKey( fhePrivateKey );
    }
    
    @Test
    public void testLeftIndexCollapser() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getLeftIndexCollapser();
        EnhancedBitMatrix right = left.rightInverse();
        EnhancedBitMatrix identity = right.multiply( left );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testLeftQueryCollapser() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getLeftQueryCollapser();
        EnhancedBitMatrix right = left.rightInverse();
        EnhancedBitMatrix identity = right.multiply( left );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testRightQueryCollapser() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getRightQueryCollapser();
        EnhancedBitMatrix right = left.leftInverse();
        EnhancedBitMatrix identity = left.multiply( right );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testRightIndexCollapser() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getRightIndexCollapser();
        EnhancedBitMatrix right = left.leftInverse();
        EnhancedBitMatrix identity = left.multiply( right );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testBitVectorToFromMatrix() {
        byte[] test = Arrays.copyOf( hf.hashString( "risefall" , Charsets.UTF_8 ).asBytes() , 8 );
        BitVector expected = BitVectors.fromBytes( test.length<<3 , test );
        BitVector actual = BitVectors.fromMatrix( EnhancedBitMatrix.squareMatrixfromBitVector( expected ) );
        Assert.assertEquals( expected, actual);
    }
    
    @Test
    public void testQueryMixer() throws NonSquareMatrixException {
        EnhancedBitMatrix queryMixer = privateKey.getQueryMixer();
        Assert.assertEquals( queryMixer.rows() , queryMixer.cols() );
        Assert.assertTrue( EnhancedBitMatrix.determinant( queryMixer ) );
    }
    
    @Test
    public void testQueryGeneration() throws SingularMatrixException {
        byte[] test =  hf.hashString( "this is a test" , Charsets.UTF_8 ).asBytes();
        BitVector expected = BitVectors.fromBytes( test.length<<3 , test );
        BitVector intermediate = privateKey.prepareSearchToken( "this is a test" );
        BitVector actual = privateKey.getQueryMixer().inverse().multiply( intermediate ).partFromTo( 0 , 127 );
        
//        EnhancedBitMatrix actualMatrix = privateKey.getLeftQueryCollapser().multiply( intermediate ).multiply( privateKey.getRightQueryCollapser() );
//        BitVector actual = BitVectors.fromSquareMatrix( actualMatrix );
        Assert.assertEquals( expected , actual );
        
    }
    
    @Test
    public void testQueryHasherGeneration() throws SingularMatrixException {
        SimplePolynomialFunction h = privateKey.getQueryHasher( globalHash , fhePrivateKey.getDecryptor() );
        byte[] testBytes =  hf.hashString( "test" , Charsets.UTF_8 ).asBytes();
//        byte[] halvedBytes = Arrays.copyOf( testBytes , testBytes.length>>>1 );
//        for( int i = testBytes.length>>>1 ; i < testBytes.length ; ++i ) {
//            halvedBytes[ i ] ^= testBytes[ i ];
//        }
        BitVector testBits = BitVectors.fromBytes( testBytes.length<<3 , testBytes );
        BitVector nonce = BitVectors.randomVector( 128 );
        BitVector query = privateKey.prepareSearchToken( "test" );
        BitVector expected = globalHash.apply( BitVectors.concatenate( nonce.partFromTo( 0 , 63 ) , testBits ) );
        BitVector intermediate = h.apply( BitVectors.concatenate( fhePublicKey.getEncrypter().apply(  nonce  ) , query ) );
        BitVector actual = BitVectors.fromSquareMatrix( privateKey.getLeftQueryCollapser().multiply( EnhancedBitMatrix.squareMatrixfromBitVector( intermediate ) ).multiply( privateKey.getRightQueryCollapser() ) );
        Assert.assertEquals( expected,actual );
    }
}
