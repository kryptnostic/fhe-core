package com.kryptnostic.multivariate;

import java.util.Arrays;

import cern.colt.bitvector.BitVector;

public class FunctionUtils {
    public static <T> T[] mergeArrays( T[] lhs, T[] rhs ) {
        T[] newArray = Arrays.copyOf( lhs , lhs.length + rhs.length );
        for( int i = lhs.length ; i < rhs.length ; ++i ) {
            newArray[ i ] = rhs[ i - lhs.length ];
        }
        
        return newArray;
    }
    
    public static BitVector subVector( BitVector v , int from , int to ) {
        int len = to - from;
        return new BitVector( Arrays.copyOfRange( v.elements() , from , to ) , len << 3 );
    }
    
    public static BitVector concatenate( BitVector lhs, BitVector rhs ) {
        BitVector concatenated = new BitVector( Arrays.copyOf( lhs.elements() , lhs.elements().length + rhs.elements().length ) , lhs.size() + rhs.size() );
        for( int i = 0 ; i < rhs.elements().length ; ++i ) {
            concatenated.elements()[ i + lhs.elements().length ] = rhs.elements()[ i ];
        }
        return concatenated;
    }
}
