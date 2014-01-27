package com.kryptnostic.multivariate.gf2;

import com.kryptnostic.multivariate.Contribution;

import cern.colt.bitvector.BitVector;

public class ContributionGF2 extends BitVector implements Contribution {
    private static final long serialVersionUID = -8080209852159696453L;
    public ContributionGF2( int size ) {
        super( size );
    }
    
    public ContributionGF2( long[] elements , int size ) {
        super( elements, size );
    }

    @Override
    public Contribution inplaceAdd(Contribution contribution) {
        this.add( contribution );
        return this;
    }

    @Override
    public void add(Contribution contribution) {
        this.xor( ( BitVector ) contribution ); 
    }
}
