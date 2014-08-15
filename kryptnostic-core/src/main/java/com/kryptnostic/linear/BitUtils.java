package com.kryptnostic.linear;

import java.util.Arrays;
import java.util.Random;

import com.kryptnostic.multivariate.MultivariateUtils;

import cern.colt.bitvector.BitVector;

public final class BitUtils {
    private BitUtils() {}
    //TODO:Re-enable seeding.
    private static final Random r = new Random( 0 );//System.currentTimeMillis() );
    
    public static long parity( long l ) {
        l ^= l>>32;
        l ^= l>>16;
        l ^= l>>8;
        l ^= l>>4;
        l ^= l>>2;
        l ^= l>>1;
        return l & 1L;
    }
    
    public static int getFirstSetBit( BitVector v ) {
        //TODO: Optimize
        for( int i = 0 ; i < v.size() ; ++i ) {
            if( v.get( i ) ) {
                return i;
            }
        }
        return -1;
    }
    
    public static BitVector randomVector( int length ) {
        return MultivariateUtils.randomVector(length);
    }
    
    public static BitVector subVector( BitVector v , int from , int to ) {
        return new BitVector( Arrays.copyOfRange( v.elements() , from , to ) , (to - from) << 6 ); 
    }
    
    public static BitVector randomVector( int length , int desiredHammingWeight ) {
        BitVector v = new BitVector( length );
        /*
         * In theory optimized popcnt instruction is going 
         * to be faster than bit twiddling to check individual bits.
         */
        while( v.cardinality() < desiredHammingWeight ) {
            v.set( r.nextInt( length ) );
        }
        return v;
    }
 }
