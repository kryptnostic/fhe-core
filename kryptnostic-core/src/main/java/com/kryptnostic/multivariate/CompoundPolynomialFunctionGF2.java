package com.kryptnostic.multivariate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

/**
 * Basic implementation of CompoundPolynomialFunction over GF2, consisting of a linked list of functions. The input to
 * the function is fed to first function in the list with the overall output of a compound function being determined by
 * traversing the linked list from the beginning and evaluating each function on the output of the previous function.
 * The output of the last function is the output of evaluating the function.
 * 
 * @author Matthew Tamayo-Rios
 */
public class CompoundPolynomialFunctionGF2 implements CompoundPolynomialFunction {
    private static final String FUNCTIONS_PROPERTY = "functions";
    private final LinkedList<PolynomialFunction> functions;

    public CompoundPolynomialFunctionGF2() {
        this(ImmutableList.<PolynomialFunction> of());
    }

    @JsonCreator
    public CompoundPolynomialFunctionGF2(
            @JsonProperty(FUNCTIONS_PROPERTY) List<? extends PolynomialFunction> functions) {
        this.functions = Lists.newLinkedList(functions);
    }

    public CompoundPolynomialFunction compose(CompoundPolynomialFunction inner) {
        validateForCompose(inner);
        CompoundPolynomialFunctionGF2 newCPF = new CompoundPolynomialFunctionGF2();

        newCPF.functions.addAll(inner.getFunctions());
        newCPF.functions.addAll(functions);
        return newCPF;
    }

    @Override
    public CompoundPolynomialFunction compose(PolynomialFunction inner) {
        validateForCompose(inner);
        CompoundPolynomialFunctionGF2 cpf = copy();
        cpf.functions.addFirst(inner);
        return cpf;
    }

    @Override
    public CompoundPolynomialFunction prefix(PolynomialFunction inner) {
        validateForCompose(inner);
        functions.addFirst(inner);
        return this;
    }

    @Override
    public CompoundPolynomialFunction suffix(PolynomialFunction inner) {
        Preconditions.checkArgument(getOutputLength() == inner.getInputLength(),
                "Function being appeneded must have the same length.");
        functions.addLast(inner);
        return this;
    }

    @Override
    public CompoundPolynomialFunctionGF2 copy() {
        CompoundPolynomialFunctionGF2 cpf = new CompoundPolynomialFunctionGF2();
        cpf.functions.addAll(this.functions);
        return cpf;
    }

    @Override
    public BitVector apply(BitVector input) {
        BitVector result = input;

        for (PolynomialFunction f : functions) {
            result = f.apply(result);
        }

        return result;
    }

    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        Preconditions.checkArgument(( lhs.size() + rhs.size() ) == getInputLength(),
                "Vectors provided for evaluation must have the same total length as the function expects as input.");
        return apply(BitVectors.concatenate(lhs, rhs));
    }

    @JsonIgnore
    @Override
    public int getInputLength() {
        if (functions.isEmpty()) {
            return 0;
        }
        return functions.getFirst().getInputLength();
    }

    @JsonIgnore
    @Override
    public int getOutputLength() {
        if (functions.isEmpty()) {
            return 0;
        }
        return functions.getLast().getOutputLength();
    }

    public void validateForCompose(PolynomialFunction inner) {
        if (getInputLength() != 0) {
            Preconditions
                    .checkArgument(getInputLength() == inner.getOutputLength(),
                            "Input length of outer function must match output length of inner function it is being composed with");
        }
    }

    @JsonProperty( FUNCTIONS_PROPERTY )
    @Override
    public List<PolynomialFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    @JsonIgnore
    @Override
    public int count() {
        return functions.size();
    }

    @Override
    public void composeHeadDirectly(SimplePolynomialFunction inner) {
        Preconditions.checkArgument(functions.getFirst() instanceof PolynomialFunction,
                "Cannot compose function that isn't of type SimplePolynomialFunction.");
        SimplePolynomialFunction outer = (SimplePolynomialFunction) functions.getFirst();
        functions.set(0, outer.compose(inner));
    }
}
