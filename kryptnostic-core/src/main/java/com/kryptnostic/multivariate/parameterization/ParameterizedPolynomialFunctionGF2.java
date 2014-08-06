package com.kryptnostic.multivariate.parameterization;

import java.util.Collections;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Allows parameterization of inputs in terms of more complicated functions. For all other purposes
 * behave like a PolynomialFunctionGF2 
 * 
 * @author mtamayo
 */
public class ParameterizedPolynomialFunctionGF2 extends PolynomialFunctionGF2 {
    private final List<CompoundPolynomialFunction> pipelines;
    
    /**
     * @param inputLength
     * @param outputLength
     * @param monomials
     * @param contributions
     * @param pipeline
     */
    public ParameterizedPolynomialFunctionGF2(int inputLength, int outputLength,Monomial[] monomials, BitVector[] contributions, Iterable<CompoundPolynomialFunction> pipelines) {
        super( inputLength, outputLength, monomials, contributions);
        Preconditions.checkArgument( !Iterables.isEmpty( pipelines ) ,"There must be a least one function in the provided chain.");
        Preconditions.checkArgument( pipelines.iterator().next().getInputLength() == inputLength , "The input length to the pipeline must be the same as the input length to the overall function.");
        this.pipelines = ImmutableList.copyOf( pipelines );
    }
    
    /** 
     * @see com.kryptnostic.multivariate.PolynomialFunctionGF2#apply(cern.colt.bitvector.BitVector)
     * @param input a bitvector apply the input.
     */
    @Override
    public BitVector apply(BitVector input) {
        BitVector [] parameters = new BitVector[ pipelines.size() + 1 ];
        parameters[0] = input;
        for( int i = 0 ; i < pipelines.size() ; ++i ) {
            CompoundPolynomialFunction pipeline = pipelines.get( i ); 
            parameters[ i + 1 ] = pipeline.apply( input );
        }
        return super.apply( FunctionUtils.concatenate( parameters ) );
    }
    
    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction inner) {
        for( CompoundPolynomialFunction pipeline : pipelines ) {
            pipeline.composeHeadDirectly( inner );
        }
        return super.compose( inner );
    }
    
    @Override
    public boolean isParameterized() {
        return true;
    }
    
    public List<CompoundPolynomialFunction> getPipelines() {
        return Collections.unmodifiableList( pipelines );
    }
    
    public int getPipelineOutputLength() {
        int inputLength = 0;
        for( CompoundPolynomialFunction pipeline : pipelines ) {
            inputLength += pipeline.getOutputLength();
        }
        return inputLength;
    }
    
    public static ParameterizedPolynomialFunctionGF2 fromExistingViaXor( SimplePolynomialFunction f , SimplePolynomialFunction newXorVariables , SimplePolynomialFunction[] pipeline ) {
        /*
         * Need to create parameterized function by shifting newXorVariables 
         */
        int extendedSize = f.getInputLength() + newXorVariables.getInputLength();
        SimplePolynomialFunction shiftedRhs = ParameterizedPolynomialFunctions.extendAndShift( extendedSize , f.getInputLength() , newXorVariables );
        SimplePolynomialFunction extendedLhs = ParameterizedPolynomialFunctions.extend( extendedSize, f );
        SimplePolynomialFunction partialResult = shiftedRhs.xor( extendedLhs );
        if( f.isParameterized() ) {
            ParameterizedPolynomialFunctionGF2 ppf = (ParameterizedPolynomialFunctionGF2)f;
            return new ParameterizedPolynomialFunctionGF2( f.getInputLength() , f.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , ImmutableList.<CompoundPolynomialFunction>builder().addAll( ppf.getPipelines() ).add( CompoundPolynomialFunctions.fromFunctions( pipeline ) ).build() );
        }
        
        return new ParameterizedPolynomialFunctionGF2( f.getInputLength() , f.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , Lists.newArrayList( CompoundPolynomialFunctions.fromFunctions( pipeline ) ) );
    }
}
