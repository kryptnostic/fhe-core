package com.kryptnostic.multivariate.predicates;

import com.google.common.base.Predicate;
import com.kryptnostic.multivariate.gf2.Monomial;

public class MonomialOrderHomogeneityPredicate implements Predicate<Monomial>{
    private final int order;
    public MonomialOrderHomogeneityPredicate( int order ) {
        this.order = order;
    }

    @Override
    public boolean apply(Monomial m) {
        return m.cardinality() == order;
    }
}
