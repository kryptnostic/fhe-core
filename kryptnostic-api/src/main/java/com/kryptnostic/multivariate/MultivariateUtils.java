package com.kryptnostic.multivariate;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Maps;
import com.kryptnostic.multivariate.gf2.Monomial;

public class MultivariateUtils {
	private static final int LONG_LENGTH = 64;
    private static final Random r = new SecureRandom();

    public static Map<Monomial, BitVector> mapViewFromMonomialsAndContributions( Monomial[] monomials, BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for( int i = 0 ; i < monomials.length ; ++i  ) {
            result.put( monomials[ i ] , contributions[ i ] );
        }
        return result;
    }
    
    public static BitVector randomVector( int length ) {
    	int numLongs = (length >>> 6) + ( length % LONG_LENGTH == 0 ? 0 : 1 );
    	long [] bits = new long[ numLongs ];
    	
    	for( int i = 0 ; i < bits.length ; ++i ) {
    		bits[ i ] = r.nextLong();
    	}
    			
        BitVector v = new BitVector( bits ,  numLongs << 6 );
        return v.partFromTo( 0, length - 1 );
    }
}
