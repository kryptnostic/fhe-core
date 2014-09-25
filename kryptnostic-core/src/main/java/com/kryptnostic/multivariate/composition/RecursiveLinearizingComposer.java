package com.kryptnostic.multivariate.composition;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class RecursiveLinearizingComposer extends RecursiveLinearizingComposerTree {
    private BitVector constantContribution;
    
    public RecursiveLinearizingComposer(SimplePolynomialFunction outer ) {
        super( 0 , outer.getInputLength() , initializeTreeFromOuter( outer ) );
        Monomial [] monomials = outer.getMonomials();
        BitVector [] contributions = outer.getContributions();
        
        for (int i = 0; i < monomials.length; ++i) {
            this.bucket( monomials[i].clone() , contributions[i].copy() );
        }
    }
    
    @Override
    public void bucket(Monomial m, BitVector contribution) {
        if( m.isZero() ) {
            constantContribution = contribution;
        } else {
            super.bucket( m , contribution );
        }
    }

    public SimplePolynomialFunction compose( SimplePolynomialFunction inner ) {
        Preconditions.checkArgument(outerInputLength == inner.getOutputLength() , "Outer input length must equal inner output length" );
        List<SimplePolynomialFunction> pieces = evaluate( null , Arrays.asList( inner.getMonomials() ), Lists.newArrayList( inner.getMonomials() ), rowContributions( inner ) , indices( inner.getMonomials() ) );
        SimplePolynomialFunction result = null;
        for( SimplePolynomialFunction piece : pieces ) {
            if( result == null ) { 
                result = piece;
            } else {
                result = piece.xor( result );
            }
        }
        if( constantContribution!=null ) {
            result = result.xor( getConstantFunction( inner.getInputLength() ) );
        }
        return result;
    }
    
    public SimplePolynomialFunction getConstantFunction( int innerInputLength) {
        return new OptimizedPolynomialFunctionGF2( innerInputLength, constantContribution.size() , new Monomial[] { Monomial.constantMonomial( innerInputLength ) } , new BitVector[] { constantContribution } );
    }
    
    private static ConcurrentMap<Monomial,Integer> indices( Monomial[] innerMonomials ) {
        ConcurrentMap<Monomial, Integer> indices = Maps.newConcurrentMap();
        
        for (int i = 0; i < innerMonomials.length; ++i) {
            indices.put( innerMonomials[i] , i );
        }
        
        return indices;
    }
    
    private static List<BitVector> rowContributions( SimplePolynomialFunction inner ) {
        return new EnhancedBitMatrix( Arrays.asList( inner.getContributions() ) ).transpose().getRows();
    }

    private static RecursiveLinearizingComposerTree[] initializeTreeFromOuter( SimplePolynomialFunction outer ) {
        return new RecursiveLinearizingComposerTree[ outer.getInputLength() ];
    }
}
