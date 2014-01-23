package com.kryptnostic.multivariate;

import java.util.Map.Entry;

import com.google.common.base.Function;

public class MonomialCountHolder implements Comparable<MonomialCountHolder> {
    private static Function<Entry<Monomial,Integer>, MonomialCountHolder> frequencyTransformer = new Function<Entry<Monomial,Integer> , MonomialCountHolder>() {
        @Override
        public MonomialCountHolder apply(Entry<Monomial, Integer> entry ) {
            return new MonomialCountHolder( entry.getKey(), entry.getValue() );
        }
    };
    private final Monomial monomial;
    private final int count;
    
    public MonomialCountHolder( Monomial monomial , int count ) {
        this.monomial = monomial;
        this.count = count;
    }
    
    public Monomial getMonomial() {
        return monomial;
    }
    
    public int getCount() {
        return count;
    }
    
    @Override
    public int compareTo(MonomialCountHolder o) {
        return Integer.compare( count , o.count );
    }
    
    public static Function<Entry<Monomial,Integer>, MonomialCountHolder> getFrequencyTransformer() {
        return frequencyTransformer; 
    }
}
