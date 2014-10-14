package com.kryptnostic.crypto;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchPrivateKeyTests {
    private static EncryptedSearchPrivateKey privateKey;
    private static HashFunction hf = Hashing.sha256();
    
    @BeforeClass
    public static void generateKey() throws SingularMatrixException {
        privateKey = new EncryptedSearchPrivateKey();
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
        byte[] test =  hf.hashString( "this is a test" , Charsets.UTF_8 ).asBytes();
        BitVector expected = BitVectors.fromBytes( test.length<<3 , test );
        BitVector actual = BitVectors.fromSquareMatrix( EnhancedBitMatrix.squareMatrixfromBitVector( expected ) );
        Assert.assertEquals( expected, actual);
    }
    
    @Test
    public void testQueryGeneration() throws SingularMatrixException {
        byte[] test =  hf.hashString( "this is a test" , Charsets.UTF_8 ).asBytes();
        BitVector expected = BitVectors.fromBytes( test.length<<3 , test );
        EnhancedBitMatrix intermediate = privateKey.prepareSearchToken( "this is a test" );
        EnhancedBitMatrix actualMatrix = privateKey.getLeftQueryCollapser().multiply( intermediate ).multiply( privateKey.getRightQueryCollapser() );
        BitVector actual = BitVectors.fromSquareMatrix( actualMatrix );
        Assert.assertEquals( expected , actual );
        
    }
}
