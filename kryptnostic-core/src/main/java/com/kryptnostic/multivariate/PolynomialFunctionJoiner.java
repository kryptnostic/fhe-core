package com.kryptnostic.multivariate;

import com.google.common.base.Preconditions;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;

import cern.colt.bitvector.BitVector;

/**
 * This class allows combining the outputs of two polynomial functions into a single polynomial function
 * with lazy compose and recursive evaluation.
 * @author Matthew Tamayo-Rios
 */
public class PolynomialFunctionJoiner implements PolynomialFunction {
    private final PolynomialFunction lhs;
    private final PolynomialFunction rhs;
    private final PolynomialFunction op;    
    public PolynomialFunctionJoiner( PolynomialFunction lhs , PolynomialFunction op , PolynomialFunction rhs ) {
        Preconditions.checkArgument( 
                ( lhs.getOutputLength() + rhs.getOutputLength() )  == op.getInputLength() , 
                "Output of functions being combined must be compatibe with operation.");
        Preconditions.checkArgument( 
                lhs.getInputLength() == rhs.getInputLength() , 
                "Joined functions must have the same input length.");
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public BitVector apply( BitVector lhs, BitVector rhs ) {
        return apply( FunctionUtils.concatenate( lhs , rhs ) );
    }
    
    @Override
    public BitVector apply(BitVector input) {
        return op.apply( lhs.apply( input ) , rhs.apply( input ) );
    }

    @Override
    public int getInputLength() {
        return lhs.getInputLength();
    }

    @Override
    public int getOutputLength() {
        return op.getOutputLength();
    }
    
    
}
