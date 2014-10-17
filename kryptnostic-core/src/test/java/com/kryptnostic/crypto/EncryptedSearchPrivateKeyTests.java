package com.kryptnostic.crypto;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.PolynomialFunctions;

public class EncryptedSearchPrivateKeyTests {
    private static final HashFunction hf = Hashing.murmur3_128();
    private static EncryptedSearchPrivateKey privateKey;
    private static PrivateKey fhePrivateKey;
    private static PublicKey fhePublicKey;
    private static SimplePolynomialFunction globalHash;
    
    @BeforeClass
    public static void generateKey() throws SingularMatrixException {
        fhePrivateKey = new PrivateKey(128,64);
        fhePublicKey = new PublicKey( fhePrivateKey );
        privateKey = new EncryptedSearchPrivateKey(fhePrivateKey,fhePublicKey);
        globalHash = PolynomialFunctions.denseRandomMultivariateQuadratic( EncryptedSearchPrivateKey.getHashBits() , EncryptedSearchPrivateKey.getHashBits() >>> 1);
    }
    
    @Test
    public void testLeftIndexCollapser() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getLeftIndexCollapser();
        EnhancedBitMatrix right = left.rightInverse();
        EnhancedBitMatrix identity = right.multiply( left );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testLeftQueryExpander() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getLeftQueryExpander();
        EnhancedBitMatrix right = left.leftInverse();
        EnhancedBitMatrix identity = right.multiply( left );
        Assert.assertTrue( identity.isIdentity() );
    }
    
    @Test
    public void testRightQueryExpander() throws SingularMatrixException {
        EnhancedBitMatrix left = privateKey.getRightQueryExpander();
        EnhancedBitMatrix right = left.rightInverse();
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
    public void testQueryGeneration() throws SingularMatrixException {
        String term = "risefall";
        BitVector expected = privateKey.hash( term );
        BitVector intermediate = privateKey.prepareSearchToken( term );
        BitVector actual = fhePrivateKey.getDecryptor().apply( intermediate );

        Assert.assertEquals( expected , actual );
    }

    @Test
    public void testQueryHasherGeneration() throws SingularMatrixException {
        String term = "barbarian";
        
        BitVector searchHash = privateKey.hash( term ); 
        BitVector encryptedSearchHash = privateKey.prepareSearchToken( term );
        
        BitVector searchNonce = BitVectors.randomVector( 64 );
        BitVector encryptedSearchNonce =  fhePublicKey.getEncrypter().apply( BitVectors.concatenate( searchNonce , BitVectors.randomVector( 64 ) ) );
        
        BitVector expected = globalHash.apply( BitVectors.concatenate( searchHash, searchNonce ) );
        
        SimplePolynomialFunction h = privateKey.getQueryHasher( globalHash , fhePrivateKey );
        BitVector intermediate = h.apply( BitVectors.concatenate( encryptedSearchHash , encryptedSearchNonce ) );
        BitVector actual = BitVectors.fromSquareMatrix( privateKey.getLeftQueryExpander().leftInverse().multiply( EnhancedBitMatrix.squareMatrixfromBitVector( intermediate ) ).multiply( privateKey.getRightQueryExpander().rightInverse() ) );
        Assert.assertEquals( expected, actual );
        
        //Now let's test running a search
        EnhancedBitMatrix documentKey = privateKey.newDocumentKey();
        EncryptedSearchSharingKey sharingKey = EncryptedSearchSharingKey.fromPrivateKey( privateKey , documentKey );
//        EncryptedSearchBridgeKey bridgeKey = new EncryptedSearchBridgeKey( privateKey , sharingKey );
//        SimplePolynomialFunction f = privateKey.getDownmixingIndexer( documentKey );
    }
    
    @Test
    public void testQueryHasherPairGeneration() throws SingularMatrixException {
        String term = "barbarian";
        
        BitVector searchHash = privateKey.hash( term ); 
        BitVector encryptedSearchHash = privateKey.prepareSearchToken( term );
        
        BitVector searchNonce = BitVectors.randomVector( 64 );
        BitVector encryptedSearchNonce =  fhePublicKey.getEncrypter().apply( BitVectors.concatenate( searchNonce , BitVectors.randomVector( 64 ) ) );
        
        EnhancedBitMatrix expectedMatrix = EnhancedBitMatrix.squareMatrixfromBitVector( globalHash.apply( BitVectors.concatenate( searchHash, searchNonce ) ) );
        BitVector expected = BitVectors.fromMatrix( expectedMatrix.multiply( expectedMatrix ) );
        
        Pair<SimplePolynomialFunction,SimplePolynomialFunction> p = privateKey.getQueryHasherPair( globalHash , fhePrivateKey );
        SimplePolynomialFunction hL = p.getLeft();
        SimplePolynomialFunction hR = p.getRight();
        
        EnhancedBitMatrix intermediateL = EnhancedBitMatrix.squareMatrixfromBitVector( hL.apply( BitVectors.concatenate( encryptedSearchHash , encryptedSearchNonce ) ) );
        EnhancedBitMatrix intermediateR = EnhancedBitMatrix.squareMatrixfromBitVector( hR.apply( BitVectors.concatenate( encryptedSearchHash , encryptedSearchNonce ) ) );
        
        BitVector actual = BitVectors.fromSquareMatrix( intermediateL.multiply( intermediateR ) );
        Assert.assertEquals( expected, actual );
        
        //Now let's test running a search
        EnhancedBitMatrix documentKey = privateKey.newDocumentKey();
        EncryptedSearchSharingKey sharingKey = EncryptedSearchSharingKey.fromPrivateKey( privateKey , documentKey );
//        EncryptedSearchBridgeKey bridgeKey = new EncryptedSearchBridgeKey( privateKey , sharingKey );
//        SimplePolynomialFunction f = privateKey.getDownmixingIndexer( documentKey );
    }
    
}
