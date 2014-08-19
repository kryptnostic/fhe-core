package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kryptnostic.linear.EnhancedBitMatrix;
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
public class PolynomialFunctionGF2 extends PolynomialFunctionRepresentationGF2 implements SimplePolynomialFunction {
    private static final Logger logger = LoggerFactory.getLogger(PolynomialFunctionGF2.class);
    private static final int CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors();
    private static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors
            .newFixedThreadPool(CONCURRENCY_LEVEL));
    private final Lock productLock = new ReentrantLock();
    private static final Predicate<BitVector> notNilContributionPredicate = new Predicate<BitVector>() {
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

    public PolynomialFunctionGF2(int inputLength, int outputLength, Monomial[] monomials, BitVector[] contributions) {
        super(inputLength, outputLength, monomials, contributions);

    }

    public static class Builder extends PolynomialFunctionRepresentationGF2.Builder {
        public Builder(int inputLength, int outputLength) {
            super(inputLength, outputLength);
        }

        @Override
        protected PolynomialFunctionRepresentationGF2 make(int inputLength, int outputLength, Monomial[] monomials,
                BitVector[] contributions) {
            return new PolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
        }

        @Override
        public PolynomialFunctionGF2 build() {
            Pair<Monomial[], BitVector[]> monomialsAndContributions = getMonomialsAndContributions();
            return new PolynomialFunctionGF2(inputLength, outputLength, monomialsAndContributions.getLeft(),
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

        return new PolynomialFunctionGF2(inputLength, outputLength, newMonomials, newContributions);
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
    public SimplePolynomialFunction compose(SimplePolynomialFunction lhs, SimplePolynomialFunction rhs) {
        return this.compose(PolynomialFunctions.concatenate(lhs, rhs));

    }

    public PolynomialFunctionGF2 extend(int length) {
        // TODO: Add re-entrant read/write lock for updating contributions.
        Monomial[] newMonomials = new Monomial[monomials.length];
        BitVector[] newContributions = new BitVector[monomials.length];

        for (int i = 0; i < contributions.length; ++i) {
            BitVector current = contributions[i];
            newMonomials[i] = monomials[i].clone();
            newContributions[i] = new BitVector(Arrays.copyOf(current.elements(), current.elements().length << 1),
                    current.size() << 1);
        }

        return new PolynomialFunctionGF2(length, length, newMonomials, newContributions);
    }

    public PolynomialFunctionGF2 clone() {
        Monomial[] newMonomials = new Monomial[monomials.length];
        BitVector[] newContributions = new BitVector[monomials.length];

        for (int i = 0; i < monomials.length; ++i) {
            newMonomials[i] = monomials[i].clone();
            newContributions[i] = contributions[i].copy();
        }

        return new PolynomialFunctionGF2(inputLength, outputLength, newMonomials, newContributions);
    }

    @Override
    public int getTotalMonomialCount() {
        int count = 0;
        for (int i = 0; i < monomials.length; ++i) {
            count += contributions[i].cardinality();
        }
        return count;
    }

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

    public static Set<Monomial> getCandidatesForProducting(final Set<Monomial> monomials,
            final Set<Monomial> requiredMonomials) {
        /*
         * Only consider products that can be formed from existing monomials and will divide something that can be
         * computed.
         */
        final Set<Monomial> candidates = Sets.newConcurrentHashSet();
        final CountDownLatch latch = new CountDownLatch(requiredMonomials.size());
        for (final Monomial required : requiredMonomials) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    for (Monomial m : monomials) {
                        Optional<Monomial> result = required.divide(m);
                        if (result.isPresent()) {
                            candidates.add(result.get());
                        }
                    }
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting on candidates for producting.");
        }

        return candidates;
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

    // TODO: Decide whether its worth unit testing this.
    public static Map<Monomial, List<Monomial>> allPossibleProductParallelEx(final Set<Monomial> monomials) {
        final ConcurrentMap<Monomial, List<Monomial>> result = new MapMaker().concurrencyLevel(CONCURRENCY_LEVEL)
                .initialCapacity(( monomials.size() * ( monomials.size() - 1 ) ) >>> 1).makeMap();
        final Monomial[] monomialArray = monomials.toArray(new Monomial[0]);
        final CountDownLatch latch = new CountDownLatch(monomials.size());
        for (int i = 0; i < monomialArray.length; ++i) {
            final Monomial lhs = monomialArray[i];
            final int currentIndex = i;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    for (int j = currentIndex; j < monomialArray.length; ++j) {
                        Monomial rhs = monomialArray[j];
                        // Skip identical monomials
                        if (!lhs.equals(rhs)) {
                            Monomial product = lhs.product(rhs);
                            // The only way we see an already existing product
                            // is x1 x2 * x2 * x3
                            if (!monomials.contains(product)) {
                                result.putIfAbsent(product, ImmutableList.of(lhs, rhs));
                            }
                        }
                    }
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Thread interrupted while waiting on all possible products.");
        }
        return result;
    }

    public static Monomial mostFrequentFactorParallel(final Set<Monomial> monomials,
            final Set<Monomial> remainingMonomials) {
        final class MostFrequentFactorResult {
            int count = 0;
            Monomial mostFrequentMonomial = null;
        }
        ;
        final MostFrequentFactorResult result = new MostFrequentFactorResult();
        final Lock updateLock = new ReentrantLock();
        final CountDownLatch latch = new CountDownLatch(monomials.size());
        for (final Monomial m : monomials) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    for (Monomial remainingMonomial : remainingMonomials) {
                        if (remainingMonomial.hasFactor(m)) {
                            ++count;
                        }
                    }

                    try {
                        updateLock.lock();
                        if (count > result.count) {
                            result.count = count;
                            result.mostFrequentMonomial = m;
                        }
                    } finally {
                        updateLock.unlock();
                    }

                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Set<Monomial> sharesFactor = Sets.newHashSet();

        for (Monomial rM : remainingMonomials) {
            if (rM.hasFactor(result.mostFrequentMonomial)) {
                sharesFactor.add(rM);
            }
        }

        for (Monomial sF : sharesFactor) {
            remainingMonomials.remove(sF);
            sF.xor(result.mostFrequentMonomial);
            if (!sF.isZero()) {
                remainingMonomials.add(sF);
            }
        }

        return result.mostFrequentMonomial;
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
            if (lhs.get(i)) {
                for (int j = 0; j < rhs.size(); ++j) {
                    if (rhs.get(j)) {
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

                        if (result.get(indexObj)) {
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
     *         {@link PolynomialFunctionGF2#notNilContributionPredicate}
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

    public static PolynomialFunctionGF2 truncatedIdentity(int outputLength, int inputLength) {
        return truncatedIdentity(0, outputLength - 1, inputLength);
    }

    public static PolynomialFunctionGF2 truncatedIdentity(int startMonomial, int stopMonomial, int inputLength) {
        int outputLength = stopMonomial - startMonomial + 1;
        Monomial[] monomials = new Monomial[outputLength];
        BitVector[] contributions = new BitVector[outputLength];

        for (int i = 0; i < outputLength; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(outputLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new PolynomialFunctionGF2(inputLength, outputLength, monomials, contributions);
    }

    public static PolynomialFunctionGF2 prepareForLhsOfBinaryOp(PolynomialFunctionGF2 lhs) {
        Monomial[] monomials = new Monomial[lhs.monomials.length];
        BitVector[] contributions = new BitVector[lhs.contributions.length];
        for (int i = 0; i < lhs.monomials.length; ++i) {
            long[] elements = monomials[i].elements();
            monomials[i] = new Monomial(Arrays.copyOf(elements, elements.length << 1), lhs.getInputLength() << 1);
            contributions[i] = contributions[i].copy();
        }

        return new PolynomialFunctionGF2(monomials[0].size(), contributions.length, monomials, contributions);
    }

    public static PolynomialFunctionGF2 prepareForRhsOfBinaryOp(PolynomialFunctionGF2 rhs) {
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

        return new PolynomialFunctionGF2(monomials[0].size(), contributions.length, monomials, contributions);
    }

    @Override
    public boolean isParameterized() {
        return false;
    }

    /**
     * Composes an outer function with the inner function.
     * 
     */
    @Override
    public SimplePolynomialFunction compose(SimplePolynomialFunction inner) {
        // Verify the functions are composable
        Preconditions.checkArgument(inputLength == inner.getOutputLength(),
                "Input length of outer function must match output length of inner function it is being composed with");
        
        Optional<Integer> constantOuterMonomialIndex = Optional.absent();
        EnhancedBitMatrix contributionRows = new EnhancedBitMatrix(Arrays.asList(inner.getContributions()));
        EnhancedBitMatrix.transpose(contributionRows);

        final List<Monomial> mList = Lists.newArrayList(inner.getMonomials());
        final ConcurrentMap<Monomial, Integer> indices = Maps.newConcurrentMap();
        for (int i = 0; i < mList.size(); ++i) {
            indices.put(mList.get(i), i);
        }
        
        if ( this.getMaximumMonomialOrder() == 2 && inner.getMaximumMonomialOrder() == 1) {
        	Monomial[] linearMonomials = inner.getMonomials();
         	for (int i = 0 ; i < linearMonomials.length; i++) {
        		for (int j = i+1; j < linearMonomials.length; j++) {
        			Monomial p = mList.get(i).product(mList.get(j));
        			if (indices.get(p) == null) {
        				indices.put(p, mList.size());
        				mList.add(p);
        			}
        		}
        	}
        }

        Optional<Integer> constantInnerMonomialIndex = Optional.fromNullable(indices.get(Monomial
                .constantMonomial(inner.getInputLength())));

        final BitVector[] innerRows = new BitVector[inputLength];
        

        for (int i = 0; i < inputLength; ++i) {
            innerRows[i] = contributionRows.getRow(i);
        }

        // Expand the outer monomials concurrently
        
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY_LEVEL);
        final BitVector[] results = new BitVector[monomials.length];
        int blocks = monomials.length / CONCURRENCY_LEVEL;
        int leftover = monomials.length % CONCURRENCY_LEVEL;
        
        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            final int fromIndex = i*blocks;
            int targetIndex = fromIndex + blocks;
            if (leftover != 0 && i == CONCURRENCY_LEVEL - 1) {
            	targetIndex += leftover;
            }
        	final int toIndex = targetIndex;
        	
        	executor.execute( new Runnable() {
				
				@Override
				public void run() {
					for (int j = fromIndex; j < toIndex; j++) {
						Monomial outerMonomial = monomials[j];
						BitVector newContributions = null;
						if (outerMonomial.isZero()) {
			                newContributions = new BitVector(mList.size());
			            } else {
			                for (int i = Long.numberOfTrailingZeros(outerMonomial.elements()[0]); i < outerMonomial.size(); ++i) {
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
                // Don't use the outer monomial directly since it maybe the
                // wrong size.
                // mList.add( monomials[ constantOuterMonomialIndex.get() ] );
                mList.add(Monomial.constantMonomial(inner.getInputLength()));
                unfilteredContributions.add(contributions[constantOuterMonomialIndex.get()]);
            }
        }

        BitVectorFunction filteredFunction = filterFunction(unfilteredContributions, mList);
        List<BitVector> filteredContributions = filteredFunction.contributions;
        List<BitVector> filteredMonomials = filteredFunction.monomials;

        if (inner.isParameterized()) {
            ParameterizedPolynomialFunctionGF2 ppf = (ParameterizedPolynomialFunctionGF2) inner;
            return new ParameterizedPolynomialFunctionGF2(inner.getInputLength(), outputLength,
                    filteredMonomials.toArray(new Monomial[0]), filteredContributions.toArray(new BitVector[0]),
                    ppf.getPipelines());
        }

        return new PolynomialFunctionGF2(inner.getInputLength(), outputLength,
                filteredMonomials.toArray(new Monomial[0]), filteredContributions.toArray(new BitVector[0]));
    }

	/**
     * Filter out monomials with empty contributions.
     * 
     * @param unfilteredContributions
     * @param mList
     * @return
     */
    private BitVectorFunction filterFunction(List<BitVector> unfilteredContributions, List<Monomial> mList) {
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
    private class BitVectorFunction {
        public List<BitVector> contributions;
        public List<BitVector> monomials;
    }
}
