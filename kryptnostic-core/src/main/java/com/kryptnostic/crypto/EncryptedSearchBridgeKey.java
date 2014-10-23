package com.kryptnostic.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchBridgeKey {
    public static final String FIELD_BRIDGE = "bridge";

    private final EnhancedBitMatrix bridge;

    public EncryptedSearchBridgeKey(EncryptedSearchPrivateKey privateKey, EncryptedSearchSharingKey sharingKey)
            throws SingularMatrixException {
        this.bridge = privateKey.getSquaringMatrix().inverse().multiply(sharingKey.getMiddle())
                .multiply(privateKey.getSquaringMatrix());
    }

    @JsonCreator
    public EncryptedSearchBridgeKey(@JsonProperty(FIELD_BRIDGE) EnhancedBitMatrix bridge) {
        this.bridge = bridge;
    }

    @JsonProperty(FIELD_BRIDGE)
    public EnhancedBitMatrix getBridge() {
        return bridge;
    }
}
