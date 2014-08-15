package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.gf2.Monomial;

public class Monomials {
    private Monomials() {}
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
}
