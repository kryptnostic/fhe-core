package com.kryptnostic.multivariate.gf2;

import java.util.List;



/**
 * This funct 
 * @author Matthew Tamayo-Rios
 */
public interface CompoundPolynomialFunction extends PolynomialFunction {
    public abstract List<PolynomialFunction> getFunctions();
    public abstract CompoundPolynomialFunction compose( PolynomialFunction inner );
    public abstract CompoundPolynomialFunction compose( CompoundPolynomialFunction inner );
    public abstract CompoundPolynomialFunction prefix( PolynomialFunction inner );
    public abstract CompoundPolynomialFunction suffix( PolynomialFunction inner );
    public abstract CompoundPolynomialFunction copy();
//    public abstract SimplePolynomialFunction compose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    
     
}
