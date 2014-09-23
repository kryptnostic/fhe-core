package com.kryptnostic.multivariate;

import java.util.Arrays;

import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class CompoundPolynomialFunctions {
    private CompoundPolynomialFunctions(){}

    public static CompoundPolynomialFunction fromFunctions( SimplePolynomialFunction ... functions ) {
        if( functions.length == 0 ) { 
            return new CompoundPolynomialFunctionGF2();
        } else {
            return new CompoundPolynomialFunctionGF2( Arrays.asList( functions ) );
        }
    }
    
}
