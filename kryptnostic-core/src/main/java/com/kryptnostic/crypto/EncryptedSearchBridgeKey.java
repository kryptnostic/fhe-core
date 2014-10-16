package com.kryptnostic.crypto;

import com.kryptnostic.linear.EnhancedBitMatrix;


public class EncryptedSearchBridgeKey {
    private final EnhancedBitMatrix leftBridge, rightBridge;
    public EncryptedSearchBridgeKey( EncryptedSearchPrivateKey privateKey , EncryptedSearchSharingKey sharingKey ) {
        this.leftBridge = sharingKey.getLeftExpander().multiply( privateKey.getLeftQueryExpander() );
        this.rightBridge = privateKey.getRightQueryCollapser().multiply( sharingKey.getRightExpander() );
    }

    public EnhancedBitMatrix getLeftBridge() {
        return leftBridge;
    }
    public EnhancedBitMatrix getRightBridge() {
        return rightBridge;
    }
}
