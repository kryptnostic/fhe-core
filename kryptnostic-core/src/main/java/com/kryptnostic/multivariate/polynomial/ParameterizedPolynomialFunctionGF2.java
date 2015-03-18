package com.kryptnostic.multivariate.polynomial;

import java.util.Collections;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;

/**
 * Allows parameterization of inputs in terms of more complicated functions. For all other purposes behave like a
 * PolynomialFunctionGF2
 * 
 * @author mtamayo
 */
public class ParameterizedPolynomialFunctionGF2 extends OptimizedPolynomialFunctionGF2 implements
        SimplePolynomialFunction {
    private static final long                      serialVersionUID   = 3192164840103405264L;

    private static final String                    PIPELINES_PROPERTY = "property";

    private final List<CompoundPolynomialFunction> pipelines;

    public ParameterizedPolynomialFunctionGF2() {
        super();
        this.pipelines = null;
    }

    /**
     * @param inputLength
     * @param outputLength
     * @param monomials
     * @param contributions
     * @param pipelines
     */
    @JsonCreator
    public ParameterizedPolynomialFunctionGF2(
            @JsonProperty( INPUT_LENGTH_PROPERTY ) int inputLength,
            @JsonProperty( OUTPUT_LENGTH_PROPERTY ) int outputLength,
            @JsonProperty( MONOMIALS_PROPERTY ) Monomial[] monomials,
            @JsonProperty( CONTRIBUTIONS_PROPERTY ) BitVector[] contributions,
            @JsonProperty( PIPELINES_PROPERTY ) List<CompoundPolynomialFunction> pipelines ) {
        super( inputLength, outputLength, monomials, contributions );
        Preconditions.checkArgument(
                !Iterables.isEmpty( pipelines ),
                "There must be a least one function in the provided chain." );
        for ( CompoundPolynomialFunction pipeline : pipelines ) {
            Preconditions.checkArgument(
                    pipeline.getInputLength() == inputLength,
                    "The input length of each pipeline must be the same as the input length to the overall function." );
        }
        this.pipelines = ImmutableList.copyOf( pipelines );
    }

    /**
     * @see com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2#apply(cern.colt.bitvector.BitVector)
     * @param input a bitvector apply the input.
     */
    @Override
    public BitVector apply( BitVector input ) {
        BitVector[] parameters = new BitVector[ pipelines.size() + 1 ];
        parameters[ 0 ] = input;
        for ( int i = 0; i < pipelines.size(); ++i ) {
            CompoundPolynomialFunction pipeline = pipelines.get( i );
            parameters[ i + 1 ] = pipeline.apply( input );
        }
        return super.apply( BitVectors.concatenate( parameters ) );
    }

    @Override
    public SimplePolynomialFunction compose( SimplePolynomialFunction inner ) {
        for ( CompoundPolynomialFunction pipeline : pipelines ) {
            pipeline.composeHeadDirectly( inner );
        }
        SimplePolynomialFunction newBase;
        if ( !inner.isParameterized() ) {
            SimplePolynomialFunction extended = ParameterizedPolynomialFunctions.extend(
                    inner.getInputLength() << 1,
                    inner );
            newBase = super.compose( new OptimizedPolynomialFunctionGF2( inner.getInputLength(), inner
                    .getOutputLength(), extended.getMonomials(), extended.getContributions() ) );
        } else {
            newBase = super.compose( inner );
        }
        return new ParameterizedPolynomialFunctionGF2(
                inner.getInputLength(),
                outputLength,
                newBase.getMonomials(),
                newBase.getContributions(),
                pipelines );
    }

    @Override
    public boolean isParameterized() {
        return true;
    }

    @JsonProperty( PIPELINES_PROPERTY )
    public List<CompoundPolynomialFunction> getPipelines() {
        return Collections.unmodifiableList( pipelines );
    }

    @JsonIgnore
    public int getPipelineOutputLength() {
        int inputLength = 0;
        for ( CompoundPolynomialFunction pipeline : pipelines ) {
            inputLength += pipeline.getOutputLength();
        }
        return inputLength;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof SimplePolynomialFunction ) {
            SimplePolynomialFunction other = (SimplePolynomialFunction) obj;
            if ( this.getInputLength() != other.getInputLength() ) {
                return false;
            }
            for ( int i = 0; i < 10000; ++i ) {
                BitVector input = BitVectors.randomVector( getInputLength() );
                if ( !this.apply( input ).equals( other.apply( input ) ) ) {
                    return false;
                }
            }
        }
        return true;
    }
}
