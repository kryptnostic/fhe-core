package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.parameterization.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.parameterization.ParameterizedPolynomialFunctions;

/**
 * This class is used for operating on and evaluating vector polynomial functions over GF(2). Functions are represented
 * an array of monomials along with corresponding BitVector lookup tables for each monomial's contribution to that
 * output bit.
 * 
 * The length of each Monomial is the number of input bits to the function. The length of each BitVector in the lookup
 * table is the number of output bits of the function.
 * 
 * 
 * 
 * @author Matthew Tamayo-Rios
 */
public class BasePolynomialFunction extends PolynomialFunctionRepresentationGF2 implements SimplePolynomialFunction {
    private static final String INPUT_LENGTH_PROPERTY = "input-length";
    private static final String OUTPUT_LENGTH_PROPERTY = "output-length";
    private static final String MONOMIALS_PROPERTY = "monomials";
    private static final String CONTRIBUTIONS_PROPERTY = "contributions";

    private static final Logger logger = LoggerFactory.getLogger(BasePolynomialFunction.class);
    private final Lock productLock = new ReentrantLock();
    protected static final Predicate<BitVector> notNilContributionPredicate = new Predicate<BitVector>() {
        @Override
        public boolean apply(BitVector v) {
            for (long l : v.elements()) {
                if (l != 0) {
                    return true;
                }
            }
            return false;
        }
    };

    public BasePolynomialFunction(@JsonProperty(INPUT_LENGTH_PROPERTY) int inputLength,
            @JsonProperty(OUTPUT_LENGTH_PROPERTY) int outputLength,
            @JsonProperty(MONOMIALS_PROPERTY) Monomial[] monomials,
            @JsonProperty(CONTRIBUTIONS_PROPERTY) BitVector[] contributions) {
        super(inputLength, outputLength, monomials, contributions);

    }

    public static class Builder extends PolynomialFunctionRepresentationGF2.Builder {
        public Builder(int inputLength, int outputLength) {
            super(inputLength, outputLength);
        }

        @Override
        protected PolynomialFunctionRepresentationGF2 make(int inputLength, int outputLength, Monomial[] monomials,
                BitVector[] contributions) {
            return new BasePolynomialFunction(inputLength, outputLength, monomials, contributions);
        }

        @Override
        public BasePolynomialFunction build() {
            Pair<Monomial[], BitVector[]> monomialsAndContributions = getMonomialsAndContributions();
            return new BasePolynomialFunction(inputLength, outputLength, monomialsAndContributions.getLeft(),
                    monomialsAndContributions.getRight());
        }
    }

    public static Builder builder(int inputLength, int outputLength) {
        return new Builder(inputLength, outputLength);
    }

    public int getOutputLength() {
        return outputLength;
    }

    public SimplePolynomialFunction xor(SimplePolynomialFunction rhs) {
        Preconditions.checkArgument(inputLength == rhs.getInputLength(),
                "Function being added must have the same input length.");
        Preconditions.checkArgument(outputLength == rhs.getOutputLength(),
                "Function being added must have the same output length.");

        if (isParameterized() || rhs.isParameterized()) {
            return ParameterizedPolynomialFunctions.xor(this, rhs);
        }

        Map<Monomial, BitVector> monomialContributionsMap = PolynomialFunctions.mapCopyFromMonomialsAndContributions(
                monomials, contributions);
        Monomial[] rhsMonomials = rhs.getMonomials();
        BitVector[] rhsContributions = rhs.getContributions();
        for (int i = 0; i < rhsMonomials.length; ++i) {
            // TODO: Make sure that monomials are immutable as extending
            // monomials without making a copy will cause hard to diagnose side
            // effects and bugs
            Monomial m = rhsMonomials[i];
            BitVector contribution = monomialContributionsMap.get(m);
            if (contribution == null) {
                contribution = new BitVector(outputLength);
                monomialContributionsMap.put(m, contribution);
            }
            contribution.xor(rhsContributions[i]);
        }
        return PolynomialFunctions.fromMonomialContributionMap(inputLength, outputLength, monomialContributionsMap);
    }

    public SimplePolynomialFunction and(SimplePolynomialFunction rhs) {
        Preconditions.checkArgument(inputLength == rhs.getInputLength(), "Functions must have the same input length.");
        Preconditions.checkArgument(outputLength == rhs.getOutputLength(),
                "Functions must have the same output length.");

        // TODO: Unit test and for parameterized functions.
        if (isParameterized() || rhs.isParameterized()) {
            return ParameterizedPolynomialFunctions.and(this, rhs);
        }

        Map<Monomial, BitVector> results = Maps.newHashMap();
        Monomial[] rhsMonomials = rhs.getMonomials();
        BitVector[] rhsContributions = rhs.getContributions();
        for (int i = 0; i < monomials.length; ++i) {
            for (int j = 0; j < rhsMonomials.length; ++j) {
                Monomial product = this.monomials[i].product(rhsMonomials[j]);
                BitVector contribution = this.contributions[i].copy();
                contribution.and(rhsContributions[j]);
                BitVector existingContribution = results.get(product);

                /*
                 * If we have no existing contribution just store the computed contribution. Otherwise, xor into the
                 * existing contribution
                 */
                if (existingContribution == null) {
                    results.put(product, contribution);
                } else {
                    existingContribution.xor(contribution);
                }
            }
        }

        removeNilContributions(results);
        Monomial[] newMonomials = new Monomial[results.size()];
        BitVector[] newContributions = new BitVector[results.size()];
        int index = 0;
        for (Entry<Monomial, BitVector> result : results.entrySet()) {
            BitVector contribution = result.getValue();
            if (contribution.cardinality() > 0) {
                newMonomials[index] = result.getKey();
                newContributions[index] = contribution;
                ++index;
            }
        }

        return new BasePolynomialFunction(inputLength, outputLength, newMonomials, newContributions);
    }

    public BitVector apply(BitVector input) {
        BitVector result = new BitVector(outputLength);

        for (int i = 0; i < monomials.length; ++i) {
            Monomial term = monomials[i];
            if (term.eval(input)) {
                result.xor(contributions[i]);
            }
        }

        return result;
    }

    @Override
    public BitVector apply(BitVector lhs, BitVector rhs) {
        return apply(FunctionUtils.concatenate(lhs, rhs));
    }

    @Override
    public SimplePolynomialFunction resolve(BitVector input) {
        Map<Monomial, BitVector> contributionsMap = Maps.newHashMapWithExpectedSize(monomials.length);
        for (int i = 0; i < monomials.length; ++i) {
            Monomial resolved = monomials[i].partialEval(input);
            if (resolved != null) {
                BitVector contribution = contributionsMap.get(resolved);
                if (contribution == null) {
                    contribution = new BitVector(outputLength);
                    contributionsMap.put(resolved, contribution);
                }
                contribution.xor(contributions[i]);
            }
        }
        return PolynomialFunctions.fromMonomialContributionMap(inputLength - input.size(), outputLength,
                contributionsMap);
    }

    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction lhs, SimplePolynomialFunction rhs) {
        return this.compose(PolynomialFunctions.concatenate(lhs, rhs));
    }

    @Override
    public SimplePolynomialFunction optimize() {
        return new OptimizedPolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
    }

    @Override
    public SimplePolynomialFunction concatenatingCompose(SimplePolynomialFunction lhs, SimplePolynomialFunction rhs) {
        return this.compose(FunctionUtils.concatenateInputsAndOutputs(lhs, rhs));
    }

    @Override
    public SimplePolynomialFunction deoptimize() {
        return new BasePolynomialFunction(inputLength, outputLength, monomials, contributions);
    }

    public BasePolynomialFunction extend(int length) {
        // TODO: Add re-entrant read/write lock for updating contributions.
        Monomial[] newMonomials = new Monomial[monomials.length];
        BitVector[] newContributions = new BitVector[monomials.length];

        for (int i = 0; i < contributions.length; ++i) {
            BitVector current = contributions[i];
            newMonomials[i] = monomials[i].clone();
            newContributions[i] = new BitVector(Arrays.copyOf(current.elements(), current.elements().length << 1),
                    current.size() << 1);
        }

        return new BasePolynomialFunction(length, length, newMonomials, newContributions);
    }

    public BasePolynomialFunction clone() {
        Monomial[] newMonomials = new Monomial[monomials.length];
        BitVector[] newContributions = new BitVector[monomials.length];

        for (int i = 0; i < monomials.length; ++i) {
            newMonomials[i] = monomials[i].clone();
            newContributions[i] = contributions[i].copy();
        }

        return new BasePolynomialFunction(inputLength, outputLength, newMonomials, newContributions);
    }

    @JsonIgnore
    @Override
    public int getTotalMonomialCount() {
        int count = 0;
        for (int i = 0; i < monomials.length; ++i) {
            count += contributions[i].cardinality();
        }
        return count;
    }

    @JsonIgnore
    @Override
    public int getMaximumMonomialOrder() {
        int maxOrder = 0;
        for (Monomial m : monomials) {
            maxOrder = Math.max(maxOrder, m.cardinality());
        }
        return maxOrder;
    }

    public static Map<Monomial, Set<Monomial>> initializeMemoMap(int outerInputLength, Monomial[] monomials,
            BitVector[] contributions) {
        Map<Monomial, Set<Monomial>> memoizedComputations = Maps.newHashMap();
        for (int i = 0; i < outerInputLength; ++i) {
            memoizedComputations.put(Monomial.linearMonomial(outerInputLength, i),
                    contributionsToMonomials(i, monomials, contributions));
        }

        return memoizedComputations;
    }

    public static Monomial mostFrequentFactor(Monomial[] toBeComputed, Set<Monomial> readyToCompute,
            Set<Monomial> alreadyComputed) {
        Monomial result = null;
        int max = -1;
        for (Monomial ready : readyToCompute) {
            if (!alreadyComputed.contains(ready)) {
                int count = 0;
                for (Monomial onDeck : toBeComputed) {
                    if (onDeck.hasFactor(ready)) {
                        count++;
                    }
                }
                if (count > max) {
                    max = count;
                    result = ready;
                }
            }
        }
        return result;
    }

    // TODO: Decide whether its worth unit testing this.
    public static Map<Monomial, List<Monomial>> allPossibleProduct(final Set<Monomial> monomials) {
        Map<Monomial, List<Monomial>> result = Maps
                .newHashMapWithExpectedSize(( monomials.size() * ( monomials.size() - 1 ) ) >>> 1);

        for (final Monomial lhs : monomials) {
            for (Monomial rhs : monomials) {
                // Skip identical monomials
                if (!lhs.equals(rhs)) {
                    Monomial product = lhs.product(rhs);
                    // Don't bother adding it to the list of possible products,
                    // if we've already seen it before.
                    if (!monomials.contains(product)) {
                        result.put(product, ImmutableList.of(lhs, rhs));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Given contributions of outer and inner polynomials as well as the list of inner monomials, computes the product,
     * updating the list of monomials, the map of monomials and returning the resultant contributions.
     * 
     * @param lhs
     * @param rhs
     * @param monomials
     * @param indices
     * @return
     */
    public BitVector product(BitVector lhs, BitVector rhs, List<Monomial> monomials,
            ConcurrentMap<Monomial, Integer> indices) {
        BitVector result = new BitVector(monomials.size());
        for (int i = 0; i < lhs.size(); ++i) {
            if (lhs.getQuick(i)) {
                for (int j = 0; j < rhs.size(); ++j) {
                    if (rhs.getQuick(j)) {
                        Monomial p = monomials.get(i).product(monomials.get(j));

                        Integer indexObj = indices.get(p);
                        int index;
                        if (indexObj == null) {
                            productLock.lock();
                            index = monomials.size();
                            indexObj = indices.putIfAbsent(p, index);
                            if (indexObj == null) {
                                monomials.add(p);
                                result.setSize(index);
                                indexObj = index;
                            }
                            productLock.unlock();
                        }

                        if (indexObj >= result.size()) {
                            result.setSize(result.size() << 1);
                        }

                        if (result.getQuick(indexObj)) {
                            result.clear(indexObj);
                        } else {
                            result.set(indexObj);
                        }
                    }
                }
            }
        }
        return result;
    }

    // TODO: Figure out whether this worth unit testing.
    public static Set<Monomial> product(Set<Monomial> lhs, Set<Monomial> rhs) {
        Set<Monomial> result = Sets.newHashSetWithExpectedSize(lhs.size() * rhs.size() / 2);
        for (Monomial mlhs : lhs) {
            for (Monomial mrhs : rhs) {
                Monomial product = mlhs.product(mrhs);
                if (!result.add(product)) {
                    result.remove(product);
                }
            }
        }
        return result;
    }

    /**
     * Filters monomials and contributions, which do not contribute to any output bits.
     * 
     * @param monomialContributionMap
     * @return A filtered map {@link Maps#filterValues(Map, Predicate)} created using
     *         {@link BasePolynomialFunction#notNilContributionPredicate}
     */
    public static Map<Monomial, BitVector> filterNilContributions(Map<Monomial, BitVector> monomialContributionMap) {
        return Maps.filterValues(monomialContributionMap, notNilContributionPredicate);
    }

    /**
     * Removes monomials and contributions, which do not contribute to any output bits.
     * 
     * @param monomialContributionMap
     *            The map from which to remove entries.
     */
    public static void removeNilContributions(Map<Monomial, BitVector> monomialContributionMap) {
        Set<Monomial> forRemoval = Sets.newHashSet();
        for (Entry<Monomial, BitVector> monomialContribution : monomialContributionMap.entrySet()) {
            if (!notNilContributionPredicate.apply(monomialContribution.getValue())) {
                forRemoval.add(monomialContribution.getKey());
            }
        }
        for (Monomial m : forRemoval) {
            monomialContributionMap.remove(m);
        }
    }

    public static Set<Monomial> contributionsToMonomials(int row, Monomial[] monomials, BitVector[] contributions) {
        /*
         * Converts a single row of contributions into monomials.
         */
        Set<Monomial> result = Sets.newHashSetWithExpectedSize(contributions.length / 2);
        for (int i = 0; i < contributions.length; ++i) {
            if (contributions[i].get(row)) {
                result.add(monomials[i]);
            }
        }
        return result;
    }

    public static BasePolynomialFunction truncatedIdentity(int outputLength, int inputLength) {
        return truncatedIdentity(0, outputLength - 1, inputLength);
    }

    public static BasePolynomialFunction truncatedIdentity(int startMonomial, int stopMonomial, int inputLength) {
        int outputLength = stopMonomial - startMonomial + 1;
        Monomial[] monomials = new Monomial[outputLength];
        BitVector[] contributions = new BitVector[outputLength];

        for (int i = 0; i < outputLength; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(outputLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new BasePolynomialFunction(inputLength, outputLength, monomials, contributions);
    }

    public static BasePolynomialFunction prepareForLhsOfBinaryOp(BasePolynomialFunction lhs) {
        Monomial[] monomials = new Monomial[lhs.monomials.length];
        BitVector[] contributions = new BitVector[lhs.contributions.length];
        for (int i = 0; i < lhs.monomials.length; ++i) {
            long[] elements = monomials[i].elements();
            monomials[i] = new Monomial(Arrays.copyOf(elements, elements.length << 1), lhs.getInputLength() << 1);
            contributions[i] = contributions[i].copy();
        }

        return new BasePolynomialFunction(monomials[0].size(), contributions.length, monomials, contributions);
    }

    public static BasePolynomialFunction prepareForRhsOfBinaryOp(BasePolynomialFunction rhs) {
        Monomial[] monomials = new Monomial[rhs.monomials.length];
        BitVector[] contributions = new BitVector[rhs.contributions.length];
        for (int i = 0; i < rhs.monomials.length; ++i) {
            long[] elements = monomials[i].elements();
            long[] newElements = new long[elements.length << 1];
            for (int j = 0; j < elements.length; ++j) {
                newElements[j] = elements[j];
            }
            monomials[i] = new Monomial(newElements, rhs.getInputLength() << 1);
            contributions[i] = contributions[i].copy();
        }

        return new BasePolynomialFunction(monomials[0].size(), contributions.length, monomials, contributions);
    }

    @JsonIgnore
    @Override
    public boolean isParameterized() {
        return false;
    }

    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction inner) {
        Preconditions.checkArgument(inputLength == inner.getOutputLength(),
                "Input length of outer function must match output length of inner function it is being composed with");

        ComposePreProcessResults prereqs = preProcessCompose(inner);

        logger.debug("Expanding outer monomials.");
        BitVector[] results = expandOuterMonomials(prereqs.monomialsList, prereqs.innerRows, prereqs.indices);

        return postProcessCompose(prereqs.monomialsList, prereqs.indices, results, inner);

    }

    /**
     * Abstracts some of the preliminary work of the compose routine. If the composition is of a quadratic with a
     * linear, pre-computes all of the monomial products.
     * 
     * @param inner
     * @return
     */
    protected ComposePreProcessResults preProcessCompose(SimplePolynomialFunction inner) {
        EnhancedBitMatrix contributionRows = new EnhancedBitMatrix(Arrays.asList(inner.getContributions()));
        EnhancedBitMatrix.transpose(contributionRows);

        List<Monomial> mList = Lists.newArrayList(inner.getMonomials());
        ConcurrentMap<Monomial, Integer> indices = Maps.newConcurrentMap();
        Map<Monomial, Integer> indicesResults = Maps
                .newHashMapWithExpectedSize(mList.size() * ( mList.size() - 1 ) / 2);
        for (int i = 0; i < mList.size(); ++i) {
            indices.put(mList.get(i), i);
        }

        if (this.getMaximumMonomialOrder() == 2 && inner.getMaximumMonomialOrder() == 1) {
            Monomial[] linearMonomials = inner.getMonomials();
            for (int i = 0; i < linearMonomials.length; i++) {
                for (int j = i + 1; j < linearMonomials.length; j++) {
                    Monomial p = mList.get(i).product(mList.get(j));
                    if (indices.get(p) == null) {
                        indices.put(p, mList.size());
                        mList.add(p);
                    }
                }
            }
        }

        Monomial[] linearMonomials = new Monomial[inputLength];
        BitVector[] innerRows = new BitVector[inputLength];

        for (int i = 0; i < inputLength; ++i) {
            Monomial linearMonomial = Monomial.linearMonomial(inputLength, i);
            linearMonomials[i] = linearMonomial;
            innerRows[i] = contributionRows.getRow(i);
        }
        for (int i = 0; i < monomials.length; ++i) {
            indicesResults.put(monomials[i], i);
        }

        ComposePreProcessResults results = new ComposePreProcessResults();
        results.indices = indices;
        results.monomialsList = mList;
        results.innerRows = innerRows;

        return results;
    }

    protected BitVector[] expandOuterMonomials(List<Monomial> mList, BitVector[] innerRows,
            ConcurrentMap<Monomial, Integer> indices) {
        BitVector[] results = new BitVector[monomials.length];
        for (int k = 0; k < monomials.length; ++k) {
            Monomial m = monomials[k];
            BitVector lhs = null;
            if (m.isZero()) {
                lhs = new BitVector(mList.size());
            } else {
                for (int i = Long.numberOfTrailingZeros(m.elements()[0]); i < inputLength; ++i) {
                    if (m.get(i)) {
                        if (lhs == null) {
                            lhs = innerRows[i];
                        } else {
                            lhs = product(lhs, innerRows[i], mList, indices);
                        }
                    }
                }
            }
            results[k] = lhs;
        }
        return results;
    }

    protected SimplePolynomialFunction postProcessCompose(List<Monomial> mList, Map<Monomial, Integer> indices,
            BitVector[] results, SimplePolynomialFunction inner) {
        Optional<Integer> constantOuterMonomialIndex = Optional.absent();
        Optional<Integer> constantInnerMonomialIndex = Optional.fromNullable(indices.get(Monomial
                .constantMonomial(inner.getInputLength())));
        // Now lets fix the contributions so they're all the same length.
        for (int i = 0; i < results.length; ++i) {
            BitVector contribution = results[i];
            if (contribution.size() != mList.size()) {
                contribution.setSize(mList.size());
            }
        }

        /*
         * Each monomial that has been computed in terms of the inner function contributes a set of monomials to each
         * row of output of the output, i.e proudctCache.get( monomials[ i ] ) We have to compute the resulting set of
         * contributions in terms of the new monomial basis for the polynomials ( mList )
         */

        BitVector[] outputContributions = new BitVector[outputLength];

        for (int row = 0; row < outputLength; ++row) {
            outputContributions[row] = new BitVector(mList.size());
            for (int i = 0; i < contributions.length; ++i) {
                if (contributions[i].get(row)) {
                    if (monomials[i].isZero()) {
                        constantOuterMonomialIndex = Optional.of(i);
                    } else {
                        outputContributions[row].xor(results[i]);
                    }
                }
            }
        }

        /*
         * After we have computed the contributions in terms of the new monomial basis we transform from row to column
         * form of contributions to match up with each monomial in mList
         */
        List<BitVector> unfilteredContributions = Lists.newArrayList(outputContributions);
        EnhancedBitMatrix.transpose(unfilteredContributions, mList.size());

        /*
         * If the outer monomial has constant terms and the unfiltered contributions have a constant term, than we xor
         * them together to get the overall constant contributions.
         */

        if (constantOuterMonomialIndex.isPresent()) {
            if (constantInnerMonomialIndex.isPresent()) {
                unfilteredContributions.get(constantInnerMonomialIndex.get()).xor(
                        contributions[constantOuterMonomialIndex.get()]);
            } else {
                // Don't use the outer monomial directly since it maybe the wrong size.
                // mList.add( monomials[ constantOuterMonomialIndex.get() ] );
                mList.add(Monomial.constantMonomial(inner.getMonomials()[0].size()));
                unfilteredContributions.add(contributions[constantOuterMonomialIndex.get()]);
            }
        }

        /*
         * Now we filter out any monomials, which have nil contributions.
         */

        List<BitVector> filteredContributions = Lists.newArrayListWithCapacity(unfilteredContributions.size());
        List<BitVector> filteredMonomials = Lists.newArrayListWithCapacity(mList.size());
        for (int i = 0; i < mList.size(); ++i) {
            BitVector contrib = unfilteredContributions.get(i);
            if (notNilContributionPredicate.apply(contrib)) {
                filteredContributions.add(contrib);
                filteredMonomials.add(mList.get(i));
            }
        }

        if (inner.isParameterized()) {
            ParameterizedPolynomialFunctionGF2 ppf = (ParameterizedPolynomialFunctionGF2) inner;
            return new ParameterizedPolynomialFunctionGF2(inner.getInputLength(), outputLength,
                    filteredMonomials.toArray(new Monomial[0]), filteredContributions.toArray(new BitVector[0]),
                    ppf.getPipelines());
        }

        return new BasePolynomialFunction(inner.getInputLength(), outputLength,
                filteredMonomials.toArray(new Monomial[0]), filteredContributions.toArray(new BitVector[0]));
    }

    /**
     * Filter out monomials with empty contributions.
     * 
     * @param unfilteredContributions
     * @param mList
     * @return
     */
    protected BitVectorFunction filterFunction(List<BitVector> unfilteredContributions, List<Monomial> mList) {
        BitVectorFunction function = new BitVectorFunction();
        function.contributions = Lists.newArrayListWithCapacity(unfilteredContributions.size());
        function.monomials = Lists.newArrayListWithCapacity(mList.size());

        for (int i = 0; i < mList.size(); ++i) {
            BitVector contrib = unfilteredContributions.get(i);
            if (notNilContributionPredicate.apply(contrib)) {
                function.contributions.add(contrib);
                function.monomials.add(mList.get(i));
            }
        }
        return function;
    }

    /**
     * Class to package results.
     *
     */
    protected class BitVectorFunction {
        public List<BitVector> contributions;
        public List<BitVector> monomials;
    }

    protected class ComposePreProcessResults {
        public List<Monomial> monomialsList;
        public ConcurrentMap<Monomial, Integer> indices;
        public BitVector[] innerRows;

    }

    @Override
    public List<SimplePolynomialFunction> split(int... splitPoints) {
        Preconditions.checkArgument(splitPoints.length > 0);
        List<SimplePolynomialFunction> functions = Lists.newArrayListWithCapacity(splitPoints.length + 1);
        int last = 0;
        for (int i = 0; i < splitPoints.length; ++i) {
            Preconditions.checkArgument(splitPoints[i] < inputLength);
            List<Monomial> newMonomials = Lists.newArrayListWithExpectedSize(monomials.length);
            List<BitVector> newContributions = Lists.newArrayListWithExpectedSize(contributions.length);
            for (int j = 0; j < contributions.length; ++j) {
                BitVector newContribution = contributions[j].partFromTo(last, splitPoints[i]);
                if (newContribution.cardinality() != 0) {
                    newMonomials.add(monomials[j]);
                    newContributions.add(newContribution);
                }
            }
            functions.add(new OptimizedPolynomialFunctionGF2(inputLength, outputLength, newMonomials
                    .toArray(new Monomial[0]), newContributions.toArray(new BitVector[0])));
            last = splitPoints[i] + 1;
        }

        return functions;
    }

    @Override
    public SimplePolynomialFunction partialComposeLeft(SimplePolynomialFunction inner) {
        Preconditions.checkArgument(inner.getOutputLength() <= getInputLength(),
                "Inner function output length cannot be larger than outer function input length.");
        SimplePolynomialFunction prepared;
        if (inner.isParameterized()) {
            prepared = partialComposePackInner(inner);

        } else {
            SimplePolynomialFunction identity = PolynomialFunctions.identity(getInputLength() - inner.getOutputLength());
            prepared = FunctionUtils.concatenateInputsAndOutputs(inner, identity);
        }
         
        return this.compose(prepared);
    }

    private SimplePolynomialFunction partialComposePackInner(SimplePolynomialFunction inner) {
        int innerPipelineOutputLength = ( (ParameterizedPolynomialFunctionGF2) inner ).getPipelineOutputLength();
        int identityLength = inputLength - inner.getOutputLength();
        int innerInputLength = inner.getInputLength() + innerPipelineOutputLength;
        int paramInputLength = inner.getInputLength() + identityLength;
        int newInputLength = innerInputLength + identityLength;

        Monomial[] unshiftedInnerMonomials = inner.getMonomials();
        BitVector[] unshiftedInnerContributions = inner.getContributions();

        Monomial[] shiftedInnerMonomials = new Monomial[unshiftedInnerContributions.length + inputLength
                - inner.getOutputLength()];
        BitVector[] shiftedInnerContributions = new BitVector[unshiftedInnerContributions.length + inputLength
                - inner.getOutputLength()];

        for (int i = 0; i < unshiftedInnerMonomials.length; ++i) {
            shiftedInnerMonomials[i] = unshiftedInnerMonomials[i].extendAndMapRanges(newInputLength, new int[] { 0,
                    inner.getInputLength() }, new int[][] { { 0, inner.getInputLength() - 1 },
                    { paramInputLength, newInputLength - 1 } });
            BitVector contributionToExtend = unshiftedInnerContributions[i].copy();
            contributionToExtend.setSize(inputLength);
            shiftedInnerContributions[i] = contributionToExtend;
        }

        for (int i = 0; i < identityLength; ++i) {
            shiftedInnerMonomials[unshiftedInnerMonomials.length + i] = Monomial.linearMonomial(newInputLength, i
                    + inner.getInputLength());
            BitVector identityContribution = new BitVector(inputLength);
            identityContribution.set(inner.getOutputLength() + i);
            shiftedInnerContributions[unshiftedInnerMonomials.length + i] = identityContribution;
        }

        List<CompoundPolynomialFunction> cpfs = ( (ParameterizedPolynomialFunctionGF2) inner ).getPipelines();
        List<CompoundPolynomialFunction> newCpfs = Lists.newArrayListWithCapacity(cpfs.size());

        for (CompoundPolynomialFunction cpf : cpfs) {
            newCpfs.add(cpf.copy().prefix(
                    PolynomialFunctions.lowerTruncatingIdentity(paramInputLength, inner.getInputLength())));
        }

        return new ParameterizedPolynomialFunctionGF2(paramInputLength, inputLength, shiftedInnerMonomials,
                shiftedInnerContributions, newCpfs);
    }
}
