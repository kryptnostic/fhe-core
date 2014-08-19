package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.gf2.Monomial;

public class Monomials {
    private Monomials() {}
    
    private static final Comparator<Monomial> comparator = new Comparator<Monomial>() {
    	@Override
		public int compare(Monomial m1, Monomial m2) {
			if (m1.cardinality() != m2.cardinality()) {
				return m1.cardinality() > m2.cardinality() ? 1 : -1;
			}
    		for (int i = 0; i < m1.size(); i++) {
				if ( m1.get(i) != m2.get(i)) {
					return m1.get(i) ? -1 : 1;
				}
			}
			return 0;
		}
	};
    
    private static Function<Monomial,Monomial> cloner = new Function<Monomial,Monomial>() {
        @Override
        public Monomial apply(Monomial input) {
            return input.clone();
        }
    };

    public static Set<Monomial> deepCloneToImmutableSet( Iterable<Monomial> monomials ) {
        return ImmutableSet.copyOf( Iterables.transform( monomials , cloner  ) );
    }
    
    public static Set<Monomial> deepCloneToImmutableSet( Monomial ... monomials ) {
        return ImmutableSet.copyOf( Iterables.transform( Arrays.asList( monomials ) , cloner  ) );
    }
    
    
    public static Set<Monomial> deepClone( Set<Monomial> monomials ) {
        return Sets.newHashSet( Iterables.transform( monomials , cloner  ) );
    }
    
    public static Set<Monomial> concurrentDeepClone( Set<Monomial> monomials ) {
        return Sets.newConcurrentHashSet( Iterables.transform( monomials , cloner  ) );
    }

    public static Set<Monomial> deepCloneToMutableSet(Monomial ... monomials) {
        return Sets.newHashSet( Iterables.transform( Arrays.asList( monomials ) , cloner  ) );
    }
    
    /**
     * Generate a list of monomials corresponding to every unique term possible for monomials of this order, at 
     * or below the order given.
     * @param order
     * @return
     */
    public static Set<Monomial> allMonomials(int size, int maxOrder) {
    	Set<Monomial> subsets = Sets.newHashSet( Monomial.constantMonomial( size ) );
        for( int ss = 0 ; ss < maxOrder ; ++ss ) {
            Set<Monomial> nextSubsets = Sets.newHashSet();
            for( Monomial m : subsets ) {
                for( int i = 0 ; i < size ; ++i ) {
                    if( !m.get( i ) ) {
                        nextSubsets.add( m.clone().chainSet( i ) );
                    }
                }
            }
            subsets.addAll( nextSubsets );
        }
        
        return subsets;
    }
    
    /**
     * Sorts by monomial order, then by variable precedence.
     * @param monomials
     */
    public static void sort(List<Monomial> monomials) {
    	Collections.sort(monomials, comparator);
    }
    
    /**
     * Optimized lookup for complete monomial list in particular sort. 
     * @param monomials
     * @param m
     * @return
     */
    public static Integer indexOfSorted(List<Monomial> monomials, Monomial m) {
    	return Collections.binarySearch(monomials, m, comparator);
    }
}
