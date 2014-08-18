package com.kryptnostic.multivariate;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.multivariate.gf2.Monomial;

public class OptimizedPolynomialFunctionGF2 extends BasePolynomialFunction {
    private static final Logger logger = LoggerFactory.getLogger( OptimizedPolynomialFunctionGF2.class );
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
}
