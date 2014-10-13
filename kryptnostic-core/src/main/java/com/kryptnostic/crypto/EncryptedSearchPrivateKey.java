package com.kryptnostic.crypto;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchPrivateKey {
    private static final HashFunction hf = Hashing.sha256();
    /*
     * Query collapsers are used for collapsing the expanded search tokens submitted to the server (E^+)(T)(X^+) 
     */
    private final EnhancedBitMatrix leftQueryCollapser,rightQueryCollapser;
    /*
     * Index collapsers are used for computing the actual index location of a shared document.
     */
    private final EnhancedBitMatrix leftIndexCollapser,rightIndexCollapser;
    
    public EncryptedSearchPrivateKey() throws SingularMatrixException {
        this( 256, 512, 256, 512 );
    }
    
    public EncryptedSearchPrivateKey( int collapsedQueryBits, int expandedQeuryBits, int collpasedIndexBits, int expandedIndexBits ) throws SingularMatrixException {
        leftQueryCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( collpasedIndexBits , expandedIndexBits , 25 );
        rightQueryCollapser = EnhancedBitMatrix.randomLeftInvertibleMatrix( expandedIndexBits , collpasedIndexBits , 25 );
        
        leftIndexCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( collpasedIndexBits , expandedIndexBits , 25 );
        rightIndexCollapser = EnhancedBitMatrix.randomLeftInvertibleMatrix( expandedIndexBits, collpasedIndexBits , 25 );
    }
    
    public EnhancedBitMatrix prepareSearchToken( String token ) throws SingularMatrixException {
        return leftQueryCollapser.rightGeneralizedInverse().multiply( EnhancedBitMatrix.squareMatrixfromBitVector( BitVectors.fromBytes( leftQueryCollapser.rows() , hf.hashString( token , Charsets.UTF_8 ).asBytes() ) ).multiply( rightQueryCollapser.leftGeneralizedInverse() ) );
    }

    public EnhancedBitMatrix getLeftQueryCollapser() {
        return leftQueryCollapser;
    }

    public EnhancedBitMatrix getRightQueryCollapser() {
        return rightQueryCollapser;
    }

    public EnhancedBitMatrix getLeftIndexCollapser() {
        return leftIndexCollapser;
    }

    public EnhancedBitMatrix getRightIndexCollapser() {
        return rightIndexCollapser;
    }
    
}
