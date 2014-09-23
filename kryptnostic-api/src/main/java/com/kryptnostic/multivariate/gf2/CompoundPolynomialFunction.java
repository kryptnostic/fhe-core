package com.kryptnostic.multivariate.gf2;

import java.util.List;



/**
 * This funct 
 * @author Matthew Tamayo-Rios
 */
public interface CompoundPolynomialFunction extends PolynomialFunction {
    List<SimplePolynomialFunction> getFunctions();
    void composeHeadDirectly( SimplePolynomialFunction inner);
    CompoundPolynomialFunction compose( SimplePolynomialFunction inner );
    CompoundPolynomialFunction compose( CompoundPolynomialFunction inner );
    CompoundPolynomialFunction prefix( SimplePolynomialFunction inner );
    CompoundPolynomialFunction suffix( SimplePolynomialFunction inner );
    CompoundPolynomialFunction copy();
    /**
     * @return The number of polynomial functions in this compoud polynomial function.
     */
    int count();
//    public abstract SimplePolynomialFunction compose( SimplePolynomialFunction lhs , SimplePolynomialFunction rhs );
    
     
}
