package com.kryptnostic.multivariate;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;
import com.kryptnostic.multivariate.gf2.Monomial;

import cern.colt.bitvector.BitVector;

public class MultivariateUtils {
    //TODO: Re-enable seeding.
    private static final Random r = new Random( 0 );//System.currentTimeMillis() );

    public static Map<Monomial, BitVector> mapViewFromMonomialsAndContributions( Monomial[] monomials, BitVector[] contributions ) {
        Map<Monomial, BitVector> result = Maps.newHashMapWithExpectedSize( monomials.length );
        for( int i = 0 ; i < monomials.length ; ++i  ) {
            result.put( monomials[ i ].clone() , contributions[ i ].copy() );
        }
        return result;
    }
    
    public static BitVector randomVector( int length ) {
        //TODO: Optimize this
        BitVector v = new BitVector( length );
        for( int i = 0 ; i < length ; ++i ) {
            if( r.nextBoolean() ) {
                v.set( i );
            }
        }
        return v;
    }
}
