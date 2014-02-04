package com.kryptnostic.multivariate;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

public class CompoundPolynomialFunctionGF2 implements CompoundPolynomialFunction {
    
    private final LinkedList<SimplePolynomialFunction> functions;
    private final int inputLength;
    private final int outputLength;
    
    public CompoundPolynomialFunctionGF2( int inputLength , int outputLength ) {
        this( ImmutableList.<SimplePolynomialFunction>of() , inputLength , outputLength );
    }
    
    public CompoundPolynomialFunctionGF2( List<SimplePolynomialFunction> functions , int inputLength , int outputLength ) {
        this.functions = Lists.newLinkedList( functions );
        Preconditions.checkArgument( this.functions.getLast().getOutputLength() == outputLength , "Specified output length does not match actual output length.");
        Preconditions.checkArgument( this.functions.getFirst().getInputLength() == inputLength , "Specified output length does not match actual output length.");
        this.inputLength = inputLength;
        this.outputLength = outputLength;
    }
    
    public SimplePolynomialFunction compose( CompoundPolynomialFunctionGF2 inner ) {
        validateForCompose( inner );
        CompoundPolynomialFunctionGF2 newCPF = new CompoundPolynomialFunctionGF2( inner.inputLength , outputLength );
        
        newCPF.functions.addAll( inner.functions );
        newCPF.functions.addAll( functions );
        
        return newCPF;
    }
    
    @Override
    public SimplePolynomialFunction compose( SimplePolynomialFunction inner ) {
        validateForCompose( inner );
        if( inner.getClass().equals( CompoundPolynomialFunctionGF2.class ) ) {
            return compose( (CompoundPolynomialFunctionGF2) inner );
        } else {
            CompoundPolynomialFunctionGF2 newCPF = new CompoundPolynomialFunctionGF2( this.functions , inner.getInputLength() , outputLength );
            newCPF.functions.addFirst( inner );
            return newCPF;
        }
    }

    @Override
    public SimplePolynomialFunction xor( SimplePolynomialFunction input ) {
        
        return null;
    }

    @Override
    public SimplePolynomialFunction and(SimplePolynomialFunction input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BitVector apply(BitVector input) {
        BitVector result = input;
        
        for( SimplePolynomialFunction f : functions ) {
            result = f.apply( result );
        }
        
        return result;
    }

    @Override
    public int getInputLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getOutputLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Monomial[] getMonomials() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BitVector[] getContributions() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void validateForCompose( SimplePolynomialFunction inner ) {
        Preconditions.checkArgument( 
                inputLength == inner.getOutputLength() ,
                "Input length of outer function must match output length of inner function it is being composed with"
                );
    }
}
