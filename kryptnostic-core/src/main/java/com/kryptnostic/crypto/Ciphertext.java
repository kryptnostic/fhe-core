package com.kryptnostic.crypto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ciphertext envelope for encrypted data.
 * 
 * @author Matthew Tamayo-Rios
 */
public class Ciphertext implements Serializable {
    private static final long serialVersionUID = -2800319660607639843L;
    protected static final String FIELD_CONTENTS = "contents";
    protected static final String FIELD_LENGTH = "length";

    protected final byte[] contents; // Encrypted field
    protected final long[] length; // Encrypted field

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
