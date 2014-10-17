package com.kryptnostic.multivariate;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import cern.colt.bitvector.BitVector;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.util.FunctionUtils;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

@Configuration
public class PolynomialFunctionTests {
    private static final Logger logger = LoggerFactory.getLogger(PolynomialFunctionTests.class);
    private static final Random r = new Random(0);
    private static final int INPUT_LENGTH = 64;
    private static final int OUTPUT_LENGTH = 64;
    private static final int PIPELINE_LENGTH = 2;

    @Timed
    public void builderTest() {
        OptimizedPolynomialFunctionGF2.Builder builder = OptimizedPolynomialFunctionGF2.builder(256, 256);
        for (int i = 0; i < 1024; ++i) {
            BitVector contribution = BitVectors.randomVector(256);
            builder.setMonomialContribution(Monomial.randomMonomial(256, 4), contribution);
        }

        SimplePolynomialFunction f = builder.build();
        BitVector result = f.apply(BitVectors.randomVector(256));
        logger.trace("Result: {}", result);
        Assert.assertEquals(result.size(), 256);
    }

    @Timed
    public void denseRandomMVQTest() {
        SimplePolynomialFunction f = singletonDenseRandomFunction();
        Assert.assertEquals(INPUT_LENGTH, f.getInputLength());
        Assert.assertEquals(OUTPUT_LENGTH, f.getOutputLength());
        Assert.assertEquals(1 + f.getInputLength() + ( ( f.getInputLength() * ( f.getInputLength() - 1 ) ) >>> 1 ),
                f.getMonomials().length);

        for (Monomial m : f.getMonomials()) {
            Assert.assertTrue(m.cardinality() <= 2);
        }

        BitVector input = randomVector();

        BitVector result = f.apply(input);
        logger.trace("Result: {}", result);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), OUTPUT_LENGTH);
        Assert.assertEquals(f.getOutputLength(), result.size());
    }

    @Timed
    public void identityTest() {
        SimplePolynomialFunction f = optimizedIdentity();
        BitVector input = BitVectors.randomVector(INPUT_LENGTH);
        Assert.assertEquals(input, f.apply(input));
    }

    @Timed
    public void monomialSetProductTest() {
        Set<Monomial> mset = Sets.newHashSet();
        Set<BitVector> cset = Sets.newHashSet();
        while (mset.size() < 256) {
            if (mset.add(Monomial.randomMonomial(256, 4))) {
                cset.add(BitVectors.randomVector(256));
            }
        }
        List<Monomial> monomials = Lists.newArrayList(mset);
        List<BitVector> contributions = Lists.newArrayList(cset);
        Set<Monomial> rowA = Sets.newHashSet();
        Set<Monomial> rowB = Sets.newHashSet();

        for (int i = 0; i < contributions.size(); ++i) {
            BitVector contribution = contributions.get(i);
            if (contribution.get(25)) {
                rowA.add(monomials.get(i));
            }
            if (contribution.get(100)) {
                rowB.add(monomials.get(i));
            }
        }
        logger.trace("Row A: {}", rowA);
        logger.trace("Row B: {}", rowB);
        Set<Monomial> expected = Sets.newHashSet();

        for (Monomial mA : rowA) {
            for (Monomial mB : rowB) {
                Monomial product = mA.product(mB);
                if (!expected.add(product)) {
                    expected.remove(product);
                }
            }
        }
        logger.trace("Expected: {}", expected);
        Set<Monomial> actual = OptimizedPolynomialFunctionGF2.product(rowA, rowB);
        Assert.assertEquals(expected, actual);
    }

    @Timed
    public void productTest() {
        SimplePolynomialFunction lhs = randomFunction();
        SimplePolynomialFunction rhs = randomFunction();
        BitVector val = BitVectors.randomVector(INPUT_LENGTH);
        BitVector expected = lhs.apply(val);
        expected.and(rhs.apply(val));
        Assert.assertEquals(expected, lhs.and(rhs).apply(val));
    }

    @Timed
    public void quadraticComposeTest() {
        SimplePolynomialFunction outer = singletonDenseRandomFunction();
        SimplePolynomialFunction inner = linearFunction();

        Stopwatch watch = Stopwatch.createStarted();
        SimplePolynomialFunction composed = outer.compose(inner);
        logger.info("Compose took {} ms.", watch.elapsed(TimeUnit.MILLISECONDS));

        for (int i = 0; i < 25; ++i) {
            BitVector randomInput = BitVectors.randomVector(INPUT_LENGTH << 1);
            BitVector innerResult = inner.apply(randomInput);
            BitVector outerResult = outer.apply(innerResult);
            BitVector composedResult = composed.apply(randomInput);
            logger.trace("Random input: {}", randomInput);
            logger.trace("Inner result: {}", innerResult);
            logger.trace("Outer result: {}", outerResult);
            logger.trace("Composed result: {}", composedResult);
            Assert.assertEquals(outerResult, composedResult);
        }
    }

    @Timed
    public void generalComposeTest() {
        SimplePolynomialFunction outer = SimplePolynomialFunctions.randomFunction(INPUT_LENGTH, OUTPUT_LENGTH, 10, 3);
        SimplePolynomialFunction inner = SimplePolynomialFunctions.randomFunction(INPUT_LENGTH, INPUT_LENGTH, 10, 2);

        Stopwatch watch = Stopwatch.createStarted();
        SimplePolynomialFunction composed = outer.compose(inner);
        logger.info("Compose took {} ms.", watch.elapsed(TimeUnit.MILLISECONDS));

        for (int i = 0; i < 25; ++i) {
            BitVector randomInput = BitVectors.randomVector(INPUT_LENGTH);
            BitVector innerResult = inner.apply(randomInput);
            BitVector outerResult = outer.apply(innerResult);
            BitVector composedResult = composed.apply(randomInput);
            logger.trace("Random input: {}", randomInput);
            logger.trace("Inner result: {}", innerResult);
            logger.trace("Outer result: {}", outerResult);
            logger.trace("Composed result: {}", composedResult);
            Assert.assertEquals(outerResult, composedResult);
        }
    }
    
    @Timed
    public void testGeneralComposeParameterized() {
        SimplePolynomialFunction outer = SimplePolynomialFunctions.randomFunction(128, 64);
        SimplePolynomialFunction other = SimplePolynomialFunctions.randomFunction(128, 128);
        SimplePolynomialFunction[] pipelines = {SimplePolynomialFunctions.identity(128)};
        SimplePolynomialFunction inner = ParameterizedPolynomialFunctions.fromUnshiftedVariables(other.getInputLength(), other, pipelines);
        
        SimplePolynomialFunction composed = outer.compose(inner);
        
        BitVector input = BitVectors.randomVector(128);
        BitVector intermediate = inner.apply(input);
        BitVector expected = outer.apply(intermediate);
        BitVector actual = composed.apply(input);
        Assert.assertEquals(expected, actual);
    }

    @Timed
    public void partialComposeTest() {
        SimplePolynomialFunction outer = SimplePolynomialFunctions.randomFunction(INPUT_LENGTH, INPUT_LENGTH, 10, 2);
        SimplePolynomialFunction inner = SimplePolynomialFunctions.randomFunction(INPUT_LENGTH, INPUT_LENGTH >> 1);

        SimplePolynomialFunction composedLeft = outer.partialComposeLeft(inner);

        for (int i = 0; i < 25; ++i) {
            BitVector innerInput = BitVectors.randomVector(inner.getInputLength());
            BitVector remainderInput = BitVectors.randomVector(composedLeft.getInputLength() - inner.getInputLength());

            BitVector leftInnerResult = inner.apply(innerInput);
            BitVector composedLeftExpected = outer.apply(leftInnerResult, remainderInput);
            BitVector composedLeftFound = composedLeft.apply(innerInput, remainderInput);
            Assert.assertEquals(composedLeftExpected, composedLeftFound);
        }
    }

    @Timed
    public void testConcatenateOutputs() {
        final int inputLength = 256;
        final int outputLength = 128;

        SimplePolynomialFunction lhs = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(inputLength,
                outputLength);
        SimplePolynomialFunction rhs = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(inputLength,
                outputLength);

        SimplePolynomialFunction concatenated = SimplePolynomialFunctions.concatenate(lhs, rhs);

        BitVector input = BitVectors.randomVector(inputLength);

        BitVector concatenatedResult = concatenated.apply(input);
        BitVector lhsResult = lhs.apply(input);
        BitVector rhsResult = rhs.apply(input);
        BitVector expected = BitVectors.concatenate(lhsResult, rhsResult);

        Assert.assertEquals(expected, concatenatedResult);
    }

    @Timed
    public void testInterleaveFunctions() {
        SimplePolynomialFunction lhs = randomFunction();
        SimplePolynomialFunction rhs = randomFunction();

        Pair<int[], int[]> inputMaps = SimplePolynomialFunctions
                .getSplitMap(lhs.getInputLength(), rhs.getInputLength());
        Pair<int[], int[]> outputMaps = SimplePolynomialFunctions.getSplitMap(lhs.getOutputLength(),
                rhs.getOutputLength());

        SimplePolynomialFunction interleaved = SimplePolynomialFunctions.interleaveFunctions(lhs, rhs,
                inputMaps.getLeft(), inputMaps.getRight(), outputMaps.getLeft(), outputMaps.getRight());

        BitVector lhInput = BitVectors.randomVector(INPUT_LENGTH);
        BitVector rhInput = BitVectors.randomVector(INPUT_LENGTH);
        BitVector interleavedInput = FunctionUtils.concatenate(lhInput, rhInput);

        BitVector lhResult = lhs.apply(lhInput);
        BitVector rhResult = rhs.apply(rhInput);
        BitVector iResult = interleaved.apply(interleavedInput);

        Assert.assertEquals(iResult, BitVectors.concatenate(lhResult, rhResult));
    }

    @Timed
    public void testToFromString() {
        SimplePolynomialFunction f = randomFunction().optimize();
        String fString = f.toString();
        logger.trace("f = {}", fString);

        SimplePolynomialFunction fPrime = SimplePolynomialFunctions.fromString(f.getInputLength(), fString);
        Assert.assertEquals(f, fPrime);
    }

    @Timed
    public void testRandomlyPartitionMVQ() {
        SimplePolynomialFunction f = denseRandomFunction().optimize();
        Pair<SimplePolynomialFunction, SimplePolynomialFunction> gh = SimplePolynomialFunctions.randomlyPartitionMVQ(f);
        SimplePolynomialFunction g = gh.getLeft();
        SimplePolynomialFunction h = gh.getRight();

        Assert.assertEquals(f, h.xor(g));

        BitVector input = BitVectors.randomVector(f.getInputLength());
        BitVector expected = f.apply(input);
        BitVector result = g.apply(input);
        result.xor(h.apply(input));

        Assert.assertEquals(expected, result);
    }

    @Timed
    public void testPipelineStage() {
        final int inputLength = 128;
        final int outputLength = 128;

        long start = System.currentTimeMillis();
        SimplePolynomialFunction f = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(inputLength,
                outputLength);
        SimplePolynomialFunction inner = SimplePolynomialFunctions.randomManyToOneLinearCombination(inputLength);
        PolynomialFunctionPipelineStage stage = PolynomialFunctionPipelineStage.build(f, inner);
        long stop = System.currentTimeMillis();
        long millis = stop - start;
        logger.info("Non-linear pipeline stage generation took {} ms", millis);

        BitVector input = BitVectors.randomVector(inputLength << 1);
        BitVector inputLower = stage.getLower().apply(inner.apply(input));
        BitVector inputUpper = stage.getUpper().apply(inner.apply(input));
        BitVector expected = f.apply(inner.apply(input));

        BitVector actual = stage.getC1().multiply(inputLower);
        actual.xor(stage.getC2().multiply(inputUpper));
        Assert.assertEquals(expected, actual);

        BitVector concatenatedInput = BitVectors.concatenate(inputLower, inputUpper);

        Assert.assertEquals(inputLower,
                SimplePolynomialFunctions.lowerIdentity(inputLength << 1).apply(concatenatedInput));
        Assert.assertEquals(inputUpper,
                SimplePolynomialFunctions.upperIdentity(inputLength << 1).apply(concatenatedInput));

        BitVector combinationActual = stage.getCombination().apply(concatenatedInput);
        Assert.assertEquals(expected, combinationActual);

        BitVector overallActual = stage.getCombination().apply(stage.getStep().apply(input));
        Assert.assertEquals(concatenatedInput, stage.getStep().apply(input));
        Assert.assertEquals(expected, overallActual);
    }

    @Timed
    public void testCombination() {
        int inputLength = 128;
        BitVector inputLower = BitVectors.randomVector(inputLength);
        BitVector inputUpper = BitVectors.randomVector(inputLength);

        EnhancedBitMatrix c1 = EnhancedBitMatrix.randomInvertibleMatrix(inputLength);
        EnhancedBitMatrix c2 = EnhancedBitMatrix.randomInvertibleMatrix(inputLength);

        SimplePolynomialFunction combination = SimplePolynomialFunctions.linearCombination(c1, c2);

        BitVector expected = c1.multiply(inputLower);
        expected.xor(c2.multiply(inputUpper));

        BitVector concatenatedInput = BitVectors.concatenate(inputLower, inputUpper);

        Assert.assertEquals(inputLower,
                SimplePolynomialFunctions.lowerIdentity(inputLength << 1).apply(concatenatedInput));
        Assert.assertEquals(inputUpper,
                SimplePolynomialFunctions.upperIdentity(inputLength << 1).apply(concatenatedInput));

        SimplePolynomialFunction f = c1.multiply(SimplePolynomialFunctions.lowerIdentity(inputLength << 1));
        SimplePolynomialFunction g = c2.multiply(SimplePolynomialFunctions.upperIdentity(inputLength << 1));

        Assert.assertEquals(c1.multiply(inputLower), f.apply(concatenatedInput));
        Assert.assertEquals(c2.multiply(inputUpper), g.apply(concatenatedInput));

        SimplePolynomialFunction fg = f.xor(g);
        Assert.assertEquals(expected, fg.apply(concatenatedInput));
        Assert.assertEquals(expected, combination.apply(concatenatedInput));
    }

    @Timed
    public void testUnitPipeline() {
        final int inputLength = 128;
        final int outputLength = 128;

        SimplePolynomialFunction[] functions = SimplePolynomialFunctions.arrayOfRandomMultivariateQuadratics(
                inputLength, outputLength, PIPELINE_LENGTH);

        SimplePolynomialFunction inner = SimplePolynomialFunctions.randomManyToOneLinearCombination(inputLength);

        Stopwatch watch = Stopwatch.createStarted();
        Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipelineDescription = SimplePolynomialFunctions
                .buildNonlinearPipeline(inner, functions);
        logger.info("Non-linear unit pipeline generation took {} ms", PIPELINE_LENGTH,
                watch.elapsed(TimeUnit.MILLISECONDS));

        CompoundPolynomialFunction originalPipeline = CompoundPolynomialFunctions.fromFunctions(functions);
        CompoundPolynomialFunction newPipeline = CompoundPolynomialFunctions.fromFunctions(pipelineDescription
                .getRight());

        BitVector input = BitVectors.randomVector(inputLength << 1);
        BitVector expected = originalPipeline.apply(inner.apply(input));
        BitVector actual = pipelineDescription.getLeft().apply(newPipeline.apply(input));
        Assert.assertEquals(expected, actual);

    }

    @Timed
    public void testParallelization() {
        SimplePolynomialFunction outerFast = denseRandomFunction();
        SimplePolynomialFunction outerSlow = outerFast.deoptimize();
        SimplePolynomialFunction innerFast = optimizedLinearFunction();
        SimplePolynomialFunction innerSlow = innerFast.deoptimize();

        SimplePolynomialFunction fastComposed = outerFast.compose(innerFast);
        SimplePolynomialFunction slowComposed = outerSlow.compose(innerSlow);

        Assert.assertEquals(slowComposed, fastComposed);

        for (int i = 0; i < 100; ++i) {
            BitVector input = randomVector();
            BitVector innerInput = BitVectors.concatenate(randomVector(), randomVector());
            Assert.assertEquals(outerSlow.apply(input), outerFast.apply(input));
            Assert.assertEquals(innerSlow.apply(innerInput), innerFast.apply(innerInput));
            Assert.assertEquals(slowComposed.apply(innerInput), fastComposed.apply(innerInput));
        }
    }

    @Timed
    public void testTestAssumptions() {
        Assert.assertTrue(randomFunction() != randomFunction());
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Timed
    public SimplePolynomialFunction randomFunction() {
        return SimplePolynomialFunctions.lightRandomFunction(INPUT_LENGTH, OUTPUT_LENGTH);
    }

    @Bean
    @Timed
    public SimplePolynomialFunction identity() {
        return SimplePolynomialFunctions.identity(INPUT_LENGTH).deoptimize();
    }

    @Bean
    @Timed
    public SimplePolynomialFunction optimizedIdentity() {
        return SimplePolynomialFunctions.identity(INPUT_LENGTH);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Timed
    public SimplePolynomialFunction denseRandomFunction() {
        return SimplePolynomialFunctions.denseRandomMultivariateQuadratic(INPUT_LENGTH, OUTPUT_LENGTH);
    }

    @Bean
    @Timed
    public SimplePolynomialFunction singletonDenseRandomFunction() {
        return SimplePolynomialFunctions.denseRandomMultivariateQuadratic(INPUT_LENGTH, OUTPUT_LENGTH);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Timed
    public SimplePolynomialFunction linearFunction() {
        return SimplePolynomialFunctions.randomManyToOneLinearCombination(INPUT_LENGTH).deoptimize();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Timed
    public SimplePolynomialFunction optimizedLinearFunction() {
        return SimplePolynomialFunctions.randomManyToOneLinearCombination(INPUT_LENGTH);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Timed
    public BitVector randomVector() {
        return BitVectors.randomVector(INPUT_LENGTH);
    }

    @Timed
    public void addTest() {
        SimplePolynomialFunction lhs = randomFunction();
        SimplePolynomialFunction rhs = randomFunction();
        BitVector val = BitVectors.randomVector(INPUT_LENGTH);
        BitVector expected = lhs.apply(val);
        expected.xor(rhs.apply(val));
        Assert.assertEquals(expected, lhs.xor(rhs).apply(val));
    }

    @Timed
    public void testRowProduct() {
        SimplePolynomialFunction f = optimizedLinearFunction();
        Monomial[] monomials = f.getMonomials();
        List<BitVector> contribs = Lists.newArrayList(f.getContributions());
        ConcurrentMap<Monomial, Integer> indices = Maps.newConcurrentMap();

        for (int i = 0; i < monomials.length; ++i) {
            indices.put(monomials[i], i);
        }

        EnhancedBitMatrix.transpose(contribs, f.getContributions().length);

        BitVector lhs = contribs.get(110);
        BitVector rhs = contribs.get(100);
        List<Monomial> mList = Lists.newArrayList(monomials);
        OptimizedPolynomialFunctionGF2 function = new OptimizedPolynomialFunctionGF2(0, 0, null, null);
        BitVector p = function.product(lhs, rhs, mList, indices);

        for (int i = 0; i < lhs.size(); ++i) {
            if (lhs.get(i)) {
                for (int j = 0; j < rhs.size(); j++) {
                    if (rhs.get(j)) {
                        Assert.assertNotNull(indices.get(mList.get(i).product(mList.get(j))));
                    }

                    if (i != j && lhs.get(i) && rhs.get(j) && lhs.get(j) && rhs.get(i)) {
                        Assert.assertFalse(p.get(indices.get(mList.get(i).product(mList.get(j)))));
                    }
                }
            }
        }

    }

    @Timed
    public void mostFrequentFactorTest() {
        Monomial[] monomials = new Monomial[] { new Monomial(256).chainSet(0).chainSet(1),
                new Monomial(256).chainSet(0).chainSet(1).chainSet(2),
                new Monomial(256).chainSet(0).chainSet(1).chainSet(2).chainSet(3),
                new Monomial(256).chainSet(0).chainSet(1).chainSet(4), };
        BitVector[] contributions = new BitVector[] { BitVectors.randomVector(256), BitVectors.randomVector(256),
                BitVectors.randomVector(256), BitVectors.randomVector(256) };

        Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap(256,
                monomials, contributions);
        Map<Monomial, List<Monomial>> possibleProducts = OptimizedPolynomialFunctionGF2
                .allPossibleProduct(memoizedComputations.keySet()); // 1
        Monomial mostFrequent = OptimizedPolynomialFunctionGF2.mostFrequentFactor(monomials, possibleProducts.keySet(),
                ImmutableSet.<Monomial> of());
        logger.trace("Most frequent monomial found: {}", mostFrequent);
        Assert.assertEquals(new Monomial(256).chainSet(0).chainSet(1), mostFrequent);
    }

    @Timed
    public void testConcatenateInputsAndOutputs() {
        SimplePolynomialFunction lhs = SimplePolynomialFunctions.lightRandomFunction(256, 256);
        SimplePolynomialFunction rhs = SimplePolynomialFunctions.lightRandomFunction(128, 64);

        SimplePolynomialFunction concatenated = SimplePolynomialFunctions.concatenateInputsAndOutputs(lhs, rhs);
        long[] src = new long[] { r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong() };
        BitVector input = new BitVector(src, 384);
        BitVector lhsInput = new BitVector(new long[] { src[0], src[1], src[2], src[3] }, 256);
        BitVector rhsInput = new BitVector(new long[] { src[4], src[5] }, 128);

        BitVector concatenatedResult = concatenated.apply(input);
        BitVector lhsResult = lhs.apply(lhsInput);
        BitVector rhsResult = rhs.apply(rhsInput);

        Assert.assertEquals(lhsResult.elements()[0], concatenatedResult.elements()[0]);
        Assert.assertEquals(lhsResult.elements()[1], concatenatedResult.elements()[1]);
        Assert.assertEquals(lhsResult.elements()[2], concatenatedResult.elements()[2]);
        Assert.assertEquals(lhsResult.elements()[3], concatenatedResult.elements()[3]);
        Assert.assertEquals(rhsResult.elements()[0], concatenatedResult.elements()[4]);

    }

    @Timed
    public void testNonlinearPipeline() {
        final int inputLength = 128;
        final int outputLength = 128;

        SimplePolynomialFunction[] functions = SimplePolynomialFunctions.arrayOfRandomMultivariateQuadratics(
                inputLength, outputLength, PIPELINE_LENGTH);

        SimplePolynomialFunction inner = SimplePolynomialFunctions.randomManyToOneLinearCombination(inputLength);

        Stopwatch watch = Stopwatch.createStarted();
        Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipelineDescription = SimplePolynomialFunctions
                .buildNonlinearPipeline(inner, functions);
        logger.info("Non-linear pipeline generation of length {} took {} ms", PIPELINE_LENGTH,
                watch.elapsed(TimeUnit.MILLISECONDS));

        CompoundPolynomialFunction originalPipeline = CompoundPolynomialFunctions.fromFunctions(functions);
        CompoundPolynomialFunction newPipeline = CompoundPolynomialFunctions.fromFunctions(pipelineDescription
                .getRight());

        BitVector input = BitVectors.randomVector(inputLength << 1);
        BitVector expected = originalPipeline.apply(inner.apply(input));
        BitVector actual = pipelineDescription.getLeft().apply(newPipeline.apply(input));
        Assert.assertEquals(expected, actual);

    }
}
