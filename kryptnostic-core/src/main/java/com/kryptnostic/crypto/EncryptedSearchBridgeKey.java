package com.kryptnostic.crypto;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;


public class EncryptedSearchBridgeKey {
    private final EnhancedBitMatrix bridge;
    public EncryptedSearchBridgeKey( EncryptedSearchPrivateKey privateKey , EncryptedSearchSharingKey sharingKey ) throws SingularMatrixException {
        this.bridge = privateKey.getSquaringMatrix().inverse().multiply( sharingKey.getMiddle() ).multiply( privateKey.getSquaringMatrix() );
    }

    public EnhancedBitMatrix getBridge() {
        return bridge;
    }
}
