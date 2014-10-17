package com.kryptnostic.multivariate.polynomial;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class OptimizedPolynomialFunctionGF2 extends BasePolynomialFunction {
    private static final Logger logger = LoggerFactory.getLogger(OptimizedPolynomialFunctionGF2.class);
    protected static final int CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors() - 1;
    protected static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors
            .newFixedThreadPool(CONCURRENCY_LEVEL));

    @JsonCreator
    public OptimizedPolynomialFunctionGF2(@JsonProperty(INPUT_LENGTH_PROPERTY) int inputLength,
            @JsonProperty(OUTPUT_LENGTH_PROPERTY) int outputLength,
            @JsonProperty(MONOMIALS_PROPERTY) Monomial[] monomials,
            @JsonProperty(CONTRIBUTIONS_PROPERTY) BitVector[] contributions) {
        super(inputLength, outputLength, monomials, contributions);
    }

    public BitVector apply(final BitVector input) {

        final CountDownLatch latch = new CountDownLatch(CONCURRENCY_LEVEL);

        final BitVector result = new BitVector(outputLength);
        int blocks = ( monomials.length / CONCURRENCY_LEVEL );
        int leftover = monomials.length % CONCURRENCY_LEVEL;

        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            final int fromIndex = i * blocks;
            int targetIndex = fromIndex + blocks;
            if (leftover != 0 && i == CONCURRENCY_LEVEL - 1) {
                targetIndex += leftover;
            }
            final int toIndex = targetIndex;

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    BitVector intermediary = new BitVector(outputLength);
                    for (int i = fromIndex; i < toIndex; ++i) {
                        Monomial term = monomials[i];
                        if (term.eval(input)) {
                            intermediary.xor(contributions[i]);
                        }
                    }
                    synchronized (result) {
                        result.xor(intermediary);
                    }
                    latch.countDown();
                }
            };
            executor.execute(r);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Concurrent apply() latch interrupted.");
        }
        return result;
    }

    /**
     * Composes an outer function with the inner function.
     * 
     */
    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction inner) {
        Preconditions.checkArgument(inputLength == inner.getOutputLength(),
                "Input length of outer function must match output length of inner function it is being composed with");

        ComposePreProcessResults prereqs = preProcessCompose(inner);

        BitVector[] results = expandOuterMonomials(prereqs.monomialsList, prereqs.innerRows, prereqs.indices);

        return postProcessCompose(prereqs.monomialsList, prereqs.indices, results, inner);
    }

    @Override
    protected BitVector[] expandOuterMonomials(final List<Monomial> mList, final BitVector[] innerRows,
            final ConcurrentMap<Monomial, Integer> indices) {
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY_LEVEL);
        final BitVector[] results = new BitVector[monomials.length];
        int blocks = monomials.length / CONCURRENCY_LEVEL;
        int leftover = monomials.length % CONCURRENCY_LEVEL;

        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            final int fromIndex = i * blocks;
            int targetIndex = fromIndex + blocks;
            if (leftover != 0 && i == CONCURRENCY_LEVEL - 1) {
                targetIndex += leftover;
            }
            final int toIndex = targetIndex;

            executor.execute(new Runnable() {

                @Override
                public void run() {
                    for (int j = fromIndex; j < toIndex; j++) {
                        Monomial outerMonomial = monomials[j];
                        BitVector newContributions = null;
                        if (outerMonomial.isZero()) {
                            newContributions = new BitVector(mList.size());
                        } else {
                            for (int i = Long.numberOfTrailingZeros(outerMonomial.elements()[0]); i < outerMonomial
                                    .size(); ++i) {
                                if (outerMonomial.get(i)) {
                                    if (newContributions == null) {
                                        newContributions = innerRows[i];
                                    } else {
                                        newContributions = product(newContributions, innerRows[i], mList, indices);
                                    }
                                }
                            }
                        }
                        results[j] = newContributions;
                    }
                    latch.countDown();
                }
            });

        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return results;
    }
}
