package com.kryptnostic.crypto.padding;

import java.util.Arrays;

public class ZeroPaddingStrategy implements PaddingStrategy {
    @Override
    public byte[] pad( byte[] unpadded ) {
        return Arrays.copyOf( unpadded , unpadded.length + 8 - (unpadded.length % 8) );
    }
}
