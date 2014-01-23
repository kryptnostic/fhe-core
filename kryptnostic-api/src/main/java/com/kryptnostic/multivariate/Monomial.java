package com.kryptnostic.multivariate;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import cern.colt.bitvector.BitVector;

public class Monomial extends BitVector {
    private static final long serialVersionUID = -8751413919025034976L;

    public Monomial( int size ) {
        super( size );
    }
    
    public Monomial( long[] bits , int size ) {
        super( bits , size );
    }
    
    public boolean eval( BitVector input ) {
        if( size() == input.size() ) {
            BitVector check = copy();
            check.and( input );
            return check.equals( this ) ;
        } else {
            throw new InvalidParameterException("Number of terms in input doesn't not much number of terms in Monomial.");
        }
    }
    
    public Monomial product( Monomial monomial ) {
        Preconditions.checkArgument( this.size() == monomial.size() , "Cannot compute product due to polynomial ring mismatch.");
        Monomial result = clone();
        result.or( monomial );
        return result;
    }
    
    public Set<Monomial> subsets( int order ) {
        int len = size();
        Set<Monomial> subsets = Sets.newHashSet( Monomial.constantMonomial( len ) );
        for( int ss = 0 ; ss < order ; ++ss ) {
            Set<Monomial> nextSubsets = Sets.newHashSet();
            for( Monomial m : subsets ) {
                for( int i = 0 ; i < len ; ++i ) {
                    if( this.get( i ) && !m.get( i ) ) {
                        nextSubsets.add( m.clone().chainSet( i ) );
                    }
                }
            }
            subsets = nextSubsets;
        }
        return subsets;
    }
    
    public Monomial chainSet(int index) {
        super.set(index);
        return this;
    }
    @Override
    public Monomial clone() {
        long[] e = this.elements();
        return new Monomial( Arrays.copyOf( e, e.length ) , this.size() );
    }
    
    public static Monomial randomMonomial( int size , int maxOrder ) {
        Random r = new Random( System.currentTimeMillis() );
        int order = r.nextInt( maxOrder - 1 ) + 1;
        Monomial monomial = new Monomial( size );
        
        Set<Integer> terms = Sets.newHashSet();
        while( terms.size() < order ) {
            terms.add( r.nextInt( size ) );
        }
        
        for( int term : terms ) {
            monomial.set( term );
        }
        
        return monomial;
    }
    
    public static Monomial constantMonomial( int size ) {
        return new Monomial( size );
    }
    
    public static Monomial linearMonomial( int size , int term ) {
        Monomial m = new Monomial( size );
        m.set( term );
        return m;
    }

}
