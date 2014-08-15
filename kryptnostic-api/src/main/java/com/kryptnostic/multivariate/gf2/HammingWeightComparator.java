package com.kryptnostic.multivariate.gf2;

import java.util.Comparator;

public class HammingWeightComparator implements Comparator<Monomial> {

    @Override
    public int compare(Monomial o1, Monomial o2) {
        Integer lhs = o1.cardinality();
        Integer rhs = o2.cardinality();
        
        return lhs.compareTo(rhs);
    }

}
