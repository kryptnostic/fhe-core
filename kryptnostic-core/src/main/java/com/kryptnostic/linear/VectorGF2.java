package com.kryptnostic.linear;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorGF2 {
    private static final Logger logger = LoggerFactory.getLogger( VectorGF2.class );
    
    private long[] vector;
    private int length;
    
    public VectorGF2( int length , long[] vector ){
        this.vector = vector;
        this.length = length;
    }
    
    public VectorGF2( long[] vector ) {
        this.vector = vector;
        length = vector.length;
    }
    
    public VectorGF2( int length ) {
        vector = new long[length];
    }
    
}
