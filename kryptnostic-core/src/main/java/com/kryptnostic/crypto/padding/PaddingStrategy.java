package com.kryptnostic.crypto.padding;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface PaddingStrategy {
    public abstract byte[] pad( byte[] unpadded );
}
