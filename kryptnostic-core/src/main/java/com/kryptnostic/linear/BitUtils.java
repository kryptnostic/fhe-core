package com.kryptnostic.linear;


public final class BitUtils {
    private BitUtils() {
    }

    public static long parity(long l) {
        l ^= l >> 32;
        l ^= l >> 16;
        l ^= l >> 8;
        l ^= l >> 4;
        l ^= l >> 2;
        l ^= l >> 1;
        return l & 1L;
    }
}
