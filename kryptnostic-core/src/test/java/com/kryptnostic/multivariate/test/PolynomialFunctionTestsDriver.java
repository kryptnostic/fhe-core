package com.kryptnostic.multivariate.test;

import org.junit.Test;

import com.kryptnostic.test.AbstractInstrumentedTest;


public class PolynomialFunctionTestsDriver extends AbstractInstrumentedTest  {
    private final PolynomialFunctionTests tests;
    public PolynomialFunctionTestsDriver() {
        super( PolynomialFunctionTests.class );
        tests = getTestContext().getBean( PolynomialFunctionTests.class );
    }
    
    @Test
    public void runCompose() {
        for( int i = 0 ; i < 10 ; ++i ) { 
            tests.composeTest();
        }
    }
    
    @Test
    public void runCombination() {
        for( int i = 0 ; i < 100 ; ++i ) {
            tests.testCombination();
        }
    }
    
    @Test
    public void runAssumptions() {
        for( int i = 0 ; i < 5 ; ++i ) { 
            tests.testTestAssumptions();
        }
    }
    
    @Test
    public void runFunctionXor() {
        for( int i = 0 ; i < 100; ++i ) {
            tests.addTest();
        }
    }

    @Test
    public void runFunctionAnd() {
        for( int i = 0 ; i < 100; ++i ) {
            tests.productTest();
        }
    }
    
    @Test
    public void runMostFrequentFactor() {
        for( int i = 0 ; i < 1000; ++i ) {
            tests.mostFrequentFactorTest();
        }
    }
    
    @Test
    public void runEvaluationTests() {
        for( int i = 0 ; i < 500000 ; ++i ) { 
            tests.identityTest();
            tests.evaluationTest();
            tests.denseEvaluationTest();
        }
    }
    
    @Test
    public void runSerializationTests() {
        for( int i = 0 ; i < 1000; ++i ) {
            tests.testToFromString();
        }
    }
    
    @Test
    public void runBuilderTests() {
        for( int i = 0 ; i < 1000; ++i ) {
            tests.builderTest();
        }
    }
}

