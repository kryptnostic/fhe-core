package com.kryptnostic.multivariate;

import java.util.Comparator;

public class HammingWeightComparator implements Comparator<Monomial> {

    @Override
    public int compare(Monomial o1, Monomial o2) {
        int lhs = o1.cardinality();
        int rhs = o2.cardinality();
        
        return Integer.compare( lhs , rhs );
    }

}
