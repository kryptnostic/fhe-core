package com.kryptnostic.multivariate;

public interface Contribution {
    public abstract Contribution inplaceAdd( Contribution contribution );
    public abstract void add( Contribution contribution );
}
