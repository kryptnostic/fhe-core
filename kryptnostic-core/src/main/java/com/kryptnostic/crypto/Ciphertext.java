package com.kryptnostic.crypto;

/**
 * Ciphertext envelope for encrypted data. 
 * @author Matthew Tamayo-Rios
 */
public class Ciphertext {
    private final byte[] contents; //Encrypted field
    private final long[] length;   //Encrypted field
    
    public Ciphertext( byte[] contents , long [] length ) {
        this.length = length;
        this.contents = contents;
    }

    public byte[] getContents() {
        return contents;
    }
    
    public long[] getLength() {
        return length;
    }
}
