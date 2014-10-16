package com.kryptnostic.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchSharingKey {
    private static final String LEFT_EXPANDER_FIELD = "leftExpander";
    private static final String RIGHT_EXPANDER_FIELD = "rightExpander";
    
    private final EnhancedBitMatrix leftExpander, rightExpander;
    
    @JsonCreator
    public EncryptedSearchSharingKey( 
            @JsonProperty(LEFT_EXPANDER_FIELD) EnhancedBitMatrix leftExpander , 
            @JsonProperty(RIGHT_EXPANDER_FIELD) EnhancedBitMatrix rightExpander ) {
        this.leftExpander = leftExpander;
        this.rightExpander = rightExpander;
    }
    
    @JsonProperty(LEFT_EXPANDER_FIELD)
    public EnhancedBitMatrix getLeftExpander() {
        return leftExpander;
    }
    
    @JsonProperty(RIGHT_EXPANDER_FIELD)
    public EnhancedBitMatrix getRightExpander() {
        return rightExpander;
    }
    
    public static EncryptedSearchSharingKey fromPrivateKey( EncryptedSearchPrivateKey privateKey , EnhancedBitMatrix documentKey ) throws SingularMatrixException {
        return new EncryptedSearchSharingKey( privateKey.getLeftIndexCollapser().rightInverse().multiply( documentKey ) , privateKey.getRightIndexCollapser().leftInverse() );
    }
}
