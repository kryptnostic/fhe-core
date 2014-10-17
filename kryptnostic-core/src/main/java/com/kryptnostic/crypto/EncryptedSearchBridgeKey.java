package com.kryptnostic.crypto;

import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;


public class EncryptedSearchBridgeKey {
    private final EnhancedBitMatrix leftBridge, rightBridge;
    public EncryptedSearchBridgeKey( EncryptedSearchPrivateKey privateKey , EncryptedSearchSharingKey sharingKey ) throws SingularMatrixException {
        this.leftBridge = sharingKey.getLeftExpander().multiply( privateKey.getLeftQueryExpander().leftInverse() );
        this.rightBridge = privateKey.getRightQueryExpander().rightInverse().multiply( sharingKey.getRightExpander() );
    }

    public EnhancedBitMatrix getLeftBridge() {
        return leftBridge;
    }
    public EnhancedBitMatrix getRightBridge() {
        return rightBridge;
    }
}
