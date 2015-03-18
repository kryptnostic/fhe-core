package com.kryptnostic.multivariate;

import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Stopwatch;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class BenchmarkingTest {
    @Test
    @Ignore
    public void benchmark() {
        int iterations = 100000;
        SimplePolynomialFunction f = SimplePolynomialFunctions.denseRandomMultivariateQuadratic( 128, 128 ).deoptimize();
        BitVector [] inputs = new BitVector[iterations];
        for( int i = 0 ; i < inputs.length ; ++i ) {
            inputs[ i ] = BitVectors.randomVector( 128 );
        }
        Stopwatch w = Stopwatch.createStarted();
        for( int i = 0 ; i < inputs.length ; ++i ) {
            f.apply( inputs[i] );
        }
        long elapsed = w.elapsed( TimeUnit.MILLISECONDS );
        
        System.out.println("Total millis: elapsed: " + elapsed + " ms");
        System.out.println("Mean millis: elapsed: " + ((double)elapsed)/iterations + " ms");
    }
}
