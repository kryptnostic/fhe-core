package com.kryptnostic.multivariate;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import com.kryptnostic.multivariate.gf2.Monomial;

import cern.colt.bitvector.BitVector;

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
        BitVector v = new BitVector( length );
        long[] bits = v.elements();
        for( int i = 0 ; i < bits.length ; ++i ) {
        	bits[ i ] = r.nextLong();
        }
        bits[ bits.length - 1 ] &= ~((~0L) << (length%LONG_LENGTH));
        return v;
    }
}
