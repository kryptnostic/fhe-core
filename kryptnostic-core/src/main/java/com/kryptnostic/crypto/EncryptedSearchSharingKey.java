package com.kryptnostic.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kryptnostic.linear.EnhancedBitMatrix;

/**
 * This class is for the non-collusion resistant scheme utilizing matrix products
 * as bridge functions to search another user's index.
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt;
 */
public class EncryptedSearchSharingKey {
    private static final String MIDDLE_FIELD = "middle";
    
    private final EnhancedBitMatrix middle;
    
    @JsonCreator
    public EncryptedSearchSharingKey( 
            @JsonProperty(MIDDLE_FIELD) EnhancedBitMatrix middle 
            ) {
        this.middle = middle;
    }
    
    @JsonProperty(MIDDLE_FIELD)
    public EnhancedBitMatrix getMiddle() {
        return middle;
    }
}
