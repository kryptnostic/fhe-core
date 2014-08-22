package com.kryptnostic.multivariate;

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
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class OptimizedPolynomialFunctionGF2 extends BasePolynomialFunction {
    private static final Logger logger = LoggerFactory.getLogger( OptimizedPolynomialFunctionGF2.class );
    protected static final int CONCURRENCY_LEVEL = Runtime.getRuntime().availableProcessors();
    protected static final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(CONCURRENCY_LEVEL));
    
    public OptimizedPolynomialFunctionGF2(int inputLength, int outputLength, Monomial[] monomials, BitVector[] contributions) {
        super( inputLength , outputLength , monomials , contributions );
    }

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
        Preconditions.checkArgument(inputLength == inner.getOutputLength(),
                "Input length of outer function must match output length of inner function it is being composed with");
        
        ComposePreProcessResults prereqs = preProcessCompose( inner );
        
        BitVector[] results = expandOuterMonomials(prereqs.monomialsList, prereqs.innerRows, prereqs.indices);
        
        return postProcessCompose(prereqs.monomialsList, prereqs.indices, results, inner);
    }

	@Override
	protected  BitVector[] expandOuterMonomials(final List<Monomial> mList, final BitVector[] innerRows, final ConcurrentMap<Monomial, Integer> indices) {
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
        return results;
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
            logger.error("Concurrent outer monomial expansion interrupted.");
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
