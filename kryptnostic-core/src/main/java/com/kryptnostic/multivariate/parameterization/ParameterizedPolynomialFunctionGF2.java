package com.kryptnostic.multivariate.parameterization;

import java.util.Collections;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Allows parameterization of inputs in terms of more complicated functions. For all other purposes behave like a
 * PolynomialFunctionGF2
 * 
 * @author mtamayo
 */
public class ParameterizedPolynomialFunctionGF2 extends OptimizedPolynomialFunctionGF2 implements
        SimplePolynomialFunction {
    private static final String PIPELINES_PROPERTY = "property";

    private final List<CompoundPolynomialFunction> pipelines;

    /**
     * @param inputLength
     * @param outputLength
     * @param monomials
     * @param contributions
     * @param pipelines
     */
    @JsonCreator
    public ParameterizedPolynomialFunctionGF2(int inputLength, int outputLength, Monomial[] monomials,
            BitVector[] contributions, @JsonProperty(PIPELINES_PROPERTY) Iterable<CompoundPolynomialFunction> pipelines) {
        super(inputLength, outputLength, monomials, contributions);
        Preconditions.checkArgument(!Iterables.isEmpty(pipelines),
                "There must be a least one function in the provided chain.");
        for (CompoundPolynomialFunction pipeline : pipelines) {
            Preconditions.checkArgument(pipeline.getInputLength() == inputLength,
                    "The input length of each pipeline must be the same as the input length to the overall function.");
        }
        this.pipelines = ImmutableList.copyOf(pipelines);
    }

    /**
     * @see com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2#apply(cern.colt.bitvector.BitVector)
     * @param input
     *            a bitvector apply the input.
     */
    @Override
    public BitVector apply(BitVector input) {
        BitVector[] parameters = new BitVector[pipelines.size() + 1];
        parameters[0] = input;
        for (int i = 0; i < pipelines.size(); ++i) {
            CompoundPolynomialFunction pipeline = pipelines.get(i);
            parameters[i + 1] = pipeline.apply(input);
        }
        return super.apply(FunctionUtils.concatenate(parameters));
    }

    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction inner) {
        for (CompoundPolynomialFunction pipeline : pipelines) {
            pipeline.composeHeadDirectly(inner);
        } 
        SimplePolynomialFunction newBase;
        if (!inner.isParameterized()) {
            SimplePolynomialFunction extended = ParameterizedPolynomialFunctions.extend(inner.getInputLength() << 1, inner);
            newBase = super.compose(new OptimizedPolynomialFunctionGF2(inner.getInputLength(), inner.getOutputLength(), extended.getMonomials(), extended.getContributions()));
        } else {
            newBase =  super.compose(inner);
        }
        return new ParameterizedPolynomialFunctionGF2(inner.getInputLength(), outputLength, newBase.getMonomials(), newBase.getContributions(), pipelines);
    }

    @Override
    public boolean isParameterized() {
        return true;
    }

    @JsonProperty(PIPELINES_PROPERTY)
    public List<CompoundPolynomialFunction> getPipelines() {
        return Collections.unmodifiableList(pipelines);
    }

    @JsonIgnore
    public int getPipelineOutputLength() {
        int inputLength = 0;
        for (CompoundPolynomialFunction pipeline : pipelines) {
            inputLength += pipeline.getOutputLength();
        }
        return inputLength;
    }
}
