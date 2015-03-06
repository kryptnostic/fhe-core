package com.kryptnostic.crypto;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Stopwatch;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class EncryptedSearchPrivateKeyTests {
    private static EncryptedSearchPrivateKey privateKey;
    private static PrivateKey                fhePrivateKey;
    private static PublicKey                 fhePublicKey;
    private static SimplePolynomialFunction  globalHash;
    private static final Logger              logger = LoggerFactory.getLogger( EncryptedSearchPrivateKeyTests.class );

    @BeforeClass
    public static void generateKey() throws SingularMatrixException {
        fhePrivateKey = new PrivateKey( 32, 16 );
        fhePublicKey = new PublicKey( fhePrivateKey );
        privateKey = new EncryptedSearchPrivateKey( 8 );
        globalHash = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(
                EncryptedSearchPrivateKey.getHashBits(),
                EncryptedSearchPrivateKey.getHashBits() >>> 1 );
    }

    @Test
    public void testQueryGeneration() throws SingularMatrixException {
        String term = "risefall";
        BitVector expected = privateKey.hash( term );
        BitVector intermediate = privateKey.prepareSearchToken( fhePublicKey, term );
        BitVector actual = BitVectors.concatenate(
                fhePrivateKey.getDecryptor().apply( intermediate.partFromTo( 0, 127 ) ),
                fhePrivateKey.getDecryptor().apply( intermediate.partFromTo( 128, 255 ) ) );

        Assert.assertEquals( expected, actual );
    }

    @Test
    public void testQueryHasherPairGeneration() throws SingularMatrixException {
        String term = "barbarian";

        BitVector searchHash = privateKey.hash( term );
        BitVector encryptedSearchHash = privateKey.prepareSearchToken( fhePublicKey, term );

        EnhancedBitMatrix expectedMatrix = EnhancedBitMatrix.squareMatrixfromBitVector( globalHash.apply( searchHash ) );
        BitVector expected = BitVectors.fromMatrix( expectedMatrix.multiply( expectedMatrix ) );

        Pair<SimplePolynomialFunction, SimplePolynomialFunction> p = privateKey.getQueryHasherPair(
                globalHash,
                fhePrivateKey );
        SimplePolynomialFunction hL = p.getLeft();
        SimplePolynomialFunction hR = p.getRight();

        BitVector[] inputs = new BitVector[ 5000 ];
        for ( int i = 0; i < inputs.length; ++i ) {
            inputs[ i ] = BitVectors.randomVector( hL.getInputLength() );
        }

        Stopwatch s = Stopwatch.createStarted();
        for ( int i = 0; i < 5000; ++i ) {
            hL.apply( inputs[ i ] );
            hR.apply( inputs[ i ] );
        }
        logger.info( "Evaluation took: {} ms", s.elapsed( TimeUnit.MILLISECONDS ) );

        EnhancedBitMatrix intermediateL = EnhancedBitMatrix.squareMatrixfromBitVector( hL.apply( encryptedSearchHash ) );
        EnhancedBitMatrix intermediateR = EnhancedBitMatrix.squareMatrixfromBitVector( hR.apply( encryptedSearchHash ) );

        EnhancedBitMatrix documentKey = privateKey.newObjectKey();
        EncryptedSearchSharingKey sharingKey = new EncryptedSearchSharingKey( documentKey );
        EncryptedSearchBridgeKey bridgeKey = new EncryptedSearchBridgeKey( privateKey, sharingKey );

        BitVector actual = BitVectors.fromSquareMatrix( intermediateL.multiply(
                privateKey.getLeftSquaringMatrix().inverse() ).multiply(
                privateKey.getRightSquaringMatrix().inverse().multiply( intermediateR ) ) );
        Assert.assertEquals( expected, actual );

        // Now let's test running a search
        actual = BitVectors
                .fromSquareMatrix( intermediateL.multiply( bridgeKey.getBridge() ).multiply( intermediateR ) );
        expectedMatrix = EnhancedBitMatrix.squareMatrixfromBitVector( globalHash.apply( searchHash ) );
        expected = BitVectors.fromSquareMatrix( expectedMatrix.multiply( sharingKey.getMiddle() ).multiply(
                expectedMatrix ) );
        Assert.assertEquals( expected, actual );
    }

}
