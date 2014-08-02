package com.kryptnostic.multivariate.parameterization;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public final class ParameterizedPolynomialFunctions {
    private ParameterizedPolynomialFunctions(){}
    
    public static SimplePolynomialFunction extend( int extendedSize , SimplePolynomialFunction f ) {
        Preconditions.checkArgument( extendedSize%64 == 0, "Extension size must be multiple of 64." );
        Monomial[] originalMonomials = f.getMonomials();
        BitVector[] originalContributions = f.getContributions();
        Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
        BitVector[] newContributions = new BitVector[ originalContributions.length ];
        
        for( int i = 0 ; i < newMonomials.length ; ++i ) {
            newMonomials[ i ] = originalMonomials[ i ].extend( extendedSize );
            newContributions[ i ] = originalContributions[ i ].copy();
        }
        
        return new PolynomialFunctionGF2( extendedSize , f.getOutputLength() , newMonomials , newContributions );
    }
    
    public static SimplePolynomialFunction extendAndShift( int extendedSize, int shiftSize , SimplePolynomialFunction f ) {
        Preconditions.checkArgument( shiftSize%64 == 0, "Shift size must be multiple of 64." );
        Monomial[] originalMonomials = f.getMonomials();
        BitVector[] originalContributions = f.getContributions();
        Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
        BitVector[] newContributions = new BitVector[ originalContributions.length ];
        
        for( int i = 0 ; i < newMonomials.length ; ++i ) {
            newMonomials[ i ] = originalMonomials[ i ].extendAndShift( extendedSize , shiftSize );
            newContributions[ i ] = originalContributions[ i ].copy();
        }
        
        return new PolynomialFunctionGF2( extendedSize , f.getOutputLength() , newMonomials , newContributions );
    }
}
