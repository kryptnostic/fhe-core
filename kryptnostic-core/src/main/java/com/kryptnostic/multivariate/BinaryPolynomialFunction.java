package com.kryptnostic.multivariate;

import com.google.common.base.Preconditions;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * This class allows combining the outputs of two polynomial functions into a single polynomial function
 * with lazy compose and recursive evaluation.
 * @author Matthew Tamayo-Rios
 */
public class BinaryPolynomialFunction implements PolynomialFunction {
    private final PolynomialFunction lhs;
    private final PolynomialFunction rhs;
    private final PolynomialFunction op;    
    public BinaryPolynomialFunction( PolynomialFunction lhs , PolynomialFunction op , PolynomialFunction rhs ) {
        Preconditions.checkArgument( 
                ( lhs.getOutputLength() + rhs.getOutputLength() )  == op.getInputLength() , 
                "Output of functions being combined must be compatibe with operation.");
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public BitVector apply( BitVector lhs, BitVector rhs ) {
        return op.apply( this.lhs.apply( lhs ) , this.rhs.apply( rhs ) ); 
    }
    
    @Override
    public BitVector apply(BitVector input) {
        BitVector lhsInput = new BitVector( lhs.getInputLength() );
        BitVector rhsInput = new BitVector( rhs.getInputLength() );
        
        long[] lhsElements = lhsInput.elements();
        long[] rhsElements = rhsInput.elements();
        long[] inputElements = input.elements();
        
        for( int i = 0 ; i < lhsElements.length ; ++i ) {
            lhsElements[ i ] = inputElements[ i ];
        }
        
        for( int i = 0 ; i < rhsElements.length ; ++i ) {
            rhsElements[ i ] = inputElements[ i + lhsElements.length ];
        }
        
        return apply( lhsInput , rhsInput ); 
    }

    @Override
    public int getInputLength() {
        return lhs.getInputLength() + rhs.getInputLength();
    }

    @Override
    public int getOutputLength() {
        return op.getOutputLength();
    }
    
    
}
