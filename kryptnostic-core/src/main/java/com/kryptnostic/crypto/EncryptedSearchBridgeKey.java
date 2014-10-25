package com.kryptnostic.crypto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchBridgeKey implements Serializable {
    private static final long serialVersionUID = 2683500138486944653L;

    public static final String FIELD_BRIDGE = "bridge";

    private final EnhancedBitMatrix bridge;

    public EncryptedSearchBridgeKey(EncryptedSearchPrivateKey privateKey, EncryptedSearchSharingKey sharingKey)
            throws SingularMatrixException {
        this.bridge = privateKey.getLeftSquaringMatrix().inverse().multiply(sharingKey.getMiddle())
                .multiply(privateKey.getRightSquaringMatrix().inverse());
    }

    @JsonCreator
    public EncryptedSearchBridgeKey(@JsonProperty(FIELD_BRIDGE) EnhancedBitMatrix bridge) {
        this.bridge = bridge;
    }

    @JsonProperty(FIELD_BRIDGE)
    public EnhancedBitMatrix getBridge() {
        return bridge;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( bridge == null ) ? 0 : bridge.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( !( obj instanceof EncryptedSearchBridgeKey ) ) {
            return false;
        }
        EncryptedSearchBridgeKey other = (EncryptedSearchBridgeKey) obj;
        if ( bridge == null ) {
            if ( other.bridge != null ) {
                return false;
            }
        } else if ( !bridge.equals( other.bridge ) ) {
            return false;
        }
        return true;
    }
}
