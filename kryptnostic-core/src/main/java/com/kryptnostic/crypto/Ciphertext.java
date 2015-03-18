package com.kryptnostic.crypto;

import java.io.Serializable;
import java.util.Arrays;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( contents );
        result = prime * result + Arrays.hashCode( length );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!( obj instanceof Ciphertext )) {
            return false;
        }
        Ciphertext other = (Ciphertext) obj;
        if (!Arrays.equals( contents , other.contents )) {
            return false;
        }
        if (!Arrays.equals( length , other.length )) {
            return false;
        }
        return true;
    }
}
