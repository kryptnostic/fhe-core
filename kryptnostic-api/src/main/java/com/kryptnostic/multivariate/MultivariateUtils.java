package com.kryptnostic.multivariate;

import java.util.Random;

import cern.colt.bitvector.BitVector;

public class MultivariateUtils {
    private static final Random r = new Random( System.currentTimeMillis() );
    public static BitVector randomVector( int size ) {
        BitVector v = new BitVector( size );
        for( int i = 0 ; i < size ; ++i ) {
            if( r.nextBoolean() ) { 
                v.set( i );
            }
        }
        return v;
    }
}
