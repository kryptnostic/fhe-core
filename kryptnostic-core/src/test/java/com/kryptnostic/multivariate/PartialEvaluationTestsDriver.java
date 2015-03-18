package com.kryptnostic.multivariate;

import org.junit.Test;

import com.kryptnostic.test.AbstractInstrumentedTest;

public class PartialEvaluationTestsDriver extends AbstractInstrumentedTest {
    private final PartialEvaluationTests tests;
    public PartialEvaluationTestsDriver() {
        super( PartialEvaluationTests.class );
        tests = getTestContext().getBean( PartialEvaluationTests.class );
    }
    
    @Test
    public void testMonomialPartialEvalRunner() {
        for( int i = 0 ; i < 1000 ; ++i ) {
            tests.testMonomialPartialEval();
        }
    }
    
    @Test
    public void testResolveRunner() {
        for( int i = 0 ; i < 1000 ; ++i ) {
            tests.testResolve();
        }
    }
}
