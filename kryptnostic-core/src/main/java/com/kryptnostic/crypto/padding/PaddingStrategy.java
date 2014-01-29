package com.kryptnostic.crypto.padding;

public interface PaddingStrategy {
    public abstract byte[] pad( byte[] unpadded );
}
