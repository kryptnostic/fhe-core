package com.kryptnostic.crypto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ciphertext envelope for encrypted data.
 * 
 * @author Matthew Tamayo-Rios
 */
public class Ciphertext {
    public static final String FIELD_CONTENTS = "contents";
    public static final String FIELD_LENGTH = "length";

    private final byte[] contents; // Encrypted field
    private final long[] length; // Encrypted field

    @JsonCreator
    public Ciphertext(@JsonProperty(FIELD_CONTENTS) byte[] contents, @JsonProperty(FIELD_LENGTH) long[] length) {
        this.length = length;
        this.contents = contents;
    }

    @JsonProperty(FIELD_CONTENTS)
    public byte[] getContents() {
        return contents;
    }

    @JsonProperty(FIELD_LENGTH)
    public long[] getLength() {
        return length;
    }
}
