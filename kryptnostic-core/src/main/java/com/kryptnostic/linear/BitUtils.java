package com.kryptnostic.linear;

import java.util.Arrays;
import java.util.Random;

import com.google.common.base.Preconditions;
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
    
    public static BitVector extend(BitVector v, int newSize) {
        Preconditions.checkArgument(v.size() <= newSize, "New size must be greater than input vector size.");
        BitVector result = v.copy();
        result.setSize(newSize);
        return result;
    }
    
    public static BitVector extendAndShift(BitVector v, int newSize, int shiftSize) {
        return extendAndShift(v, newSize, 0, shiftSize);
    }
    
    public static BitVector extendAndShift(BitVector v, int newSize , int baseIndex, int shiftSize ) {
        Preconditions.checkArgument(newSize >= baseIndex + shiftSize, "Cannot shift to index greater than new max.");
        BitVector extended = extend(v, newSize);
        int indexShift = shiftSize >>> 6;
        int base = baseIndex >>> 6;
        for( int i = 0; i < indexShift; ++i ) {
            extended.elements()[ base + i ] = 0L;
            extended.elements()[ base + i + indexShift ] = v.elements()[ base + i ];
        }
        return extended;
    }
    
 }
