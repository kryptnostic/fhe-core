package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;

import cern.colt.bitvector.BitVector;

public class CompoundPolynomialFunctionGF2 implements CompoundPolynomialFunction {
    
    private final LinkedList<PolynomialFunction> functions;
    
    public CompoundPolynomialFunctionGF2() {
        this( ImmutableList.<PolynomialFunction>of() );
    }
    
    public CompoundPolynomialFunctionGF2( List<PolynomialFunction> functions ) {
        this.functions = Lists.newLinkedList( functions );
    }
    
    public CompoundPolynomialFunction compose( CompoundPolynomialFunction inner ) {
        validateForCompose( inner );
        CompoundPolynomialFunctionGF2 newCPF = new CompoundPolynomialFunctionGF2();
        
        newCPF.functions.addAll( inner.getFunctions() );
        newCPF.functions.addAll( functions );
        
        return newCPF;
    }
    
    @Override
    public CompoundPolynomialFunction compose( PolynomialFunction inner ) {
        validateForCompose( inner );
        CompoundPolynomialFunctionGF2 cpf = copy();
        cpf.functions.addFirst( inner );
        return cpf;
    }
    
    @Override
    public CompoundPolynomialFunctionGF2 copy() {
        CompoundPolynomialFunctionGF2 cpf = new CompoundPolynomialFunctionGF2();
        cpf.functions.addAll( this.functions );
        return cpf;
    }

    @Override
    public BitVector apply(BitVector input) {
        BitVector result = input;
        
        for( PolynomialFunction f : functions ) {
            result = f.apply( result );
        }
        
        return result;
    }
    
    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        Preconditions.checkArgument( 
                ( lhs.size() + rhs.size() ) == getInputLength() , 
                "Vectors provided for evaluation must have the same total length as the function expects as input."); 
        return apply( FunctionUtils.concatenate( lhs , rhs ) );
    }
    
    @Override
    public int getInputLength() {
        if( functions.isEmpty() ) {
            return 0;
        }
        return functions.getFirst().getInputLength();
    }

    @Override
    public int getOutputLength() {
        if( functions.isEmpty() ) {
            return 0;
        }
        return functions.getLast().getOutputLength();
    }

    public void validateForCompose( PolynomialFunction inner ) {
        if( getInputLength() != 0 ) { 
            Preconditions.checkArgument( 
                    getInputLength() == inner.getOutputLength() ,
                    "Input length of outer function must match output length of inner function it is being composed with"
                    );
        }
    }

    public static CompoundPolynomialFunctionGF2 fromFunctions( PolynomialFunction ... functions ) {
        if( functions.length == 0 ) { 
            return new CompoundPolynomialFunctionGF2();
        } else {
            return new CompoundPolynomialFunctionGF2( Arrays.asList( functions ) );
        }
    }

    @Override
    public List<PolynomialFunction> getFunctions() {
        return functions;
    }
}
