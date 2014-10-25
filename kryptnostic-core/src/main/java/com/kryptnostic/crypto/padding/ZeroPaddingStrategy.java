package com.kryptnostic.crypto.padding;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ZeroPaddingStrategy implements PaddingStrategy {
    private static final String INPUT_BYTE_LENGTH_PROPERTY = "input-byte-length";
    public final int inputByteLength;
    
    @JsonCreator
    public ZeroPaddingStrategy( @JsonProperty(INPUT_BYTE_LENGTH_PROPERTY) int inputByteLength ) {
        this.inputByteLength = inputByteLength;
    }

    @Override
    public byte[] pad( byte[] unpadded ) {
        int leftover = unpadded.length % inputByteLength;
        if( leftover != 0 ) {
            return Arrays.copyOf( unpadded , unpadded.length + inputByteLength - leftover );
        } else {
            return unpadded;
        }
    }
   
    @JsonProperty(INPUT_BYTE_LENGTH_PROPERTY)
    public int getInputByteLength() {
        return inputByteLength;
    }
}
