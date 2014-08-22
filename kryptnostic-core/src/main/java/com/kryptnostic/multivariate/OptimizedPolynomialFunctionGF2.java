package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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

public class OptimizedPolynomialFunctionGF2 extends BasePolynomialFunction {
    private static final Logger logger = LoggerFactory.getLogger( OptimizedPolynomialFunctionGF2.class );
    protected static final int CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors();
    protected static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(CONCURRENCY_LEVEL));
    
    public OptimizedPolynomialFunctionGF2(int inputLength, int outputLength, Monomial[] monomials, BitVector[] contributions) {
        super( inputLength , outputLength , monomials , contributions );
    }

    /**
     * Evaluate function for input vector.
     * 
     * TODO: find and fix concurrency bug
     */
    public BitVector apply( final BitVector input ) {
        
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY_LEVEL);
        
        final BitVector result = new BitVector( outputLength );
        int blocks = (monomials.length / CONCURRENCY_LEVEL);
        int leftover = monomials.length % CONCURRENCY_LEVEL;
        
        
        for( int i=0; i<CONCURRENCY_LEVEL; i++ ) {
            final int fromIndex = i*blocks;
            int targetIndex = fromIndex + blocks;
            if (leftover != 0 && i == CONCURRENCY_LEVEL - 1) {
                targetIndex += leftover;
            }
            final int toIndex = targetIndex;
            
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    BitVector intermediary = new BitVector( outputLength);
                    for( int i = fromIndex; i < toIndex ; ++i ) {
                        Monomial term =  monomials[ i ];
                        if( term.eval( input ) ){
                            intermediary.xor( contributions[ i ] );
                        }
                    }
                    synchronized (result) {
                        result.xor(intermediary);
                    }
                    latch.countDown();
                }
            };
            executor.execute( r );
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Concurrent apply() latch interrupted.");
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

        return new BasePolynomialFunction(inner.getInputLength(), outputLength,
                filteredMonomials.toArray(new Monomial[0]), filteredContributions.toArray(new BitVector[0]));
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

}
