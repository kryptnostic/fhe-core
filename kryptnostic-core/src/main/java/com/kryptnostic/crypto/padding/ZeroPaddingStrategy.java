package com.kryptnostic.crypto.padding;

import java.util.Arrays;

public class ZeroPaddingStrategy implements PaddingStrategy {
    @Override
    public byte[] pad( byte[] unpadded ) {
//        int mask = unpadded.length & 7;
//        if( mask == 0 && (~mask)!=0 ) {
        int leftover = unpadded.length % 8;
        if( leftover != 0 ) {
            return Arrays.copyOf( unpadded , unpadded.length + 8 - leftover );
        } else {
            return unpadded;
        }
    }
}
