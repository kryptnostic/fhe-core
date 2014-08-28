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
        for( int i = 0 ; i < 1 ; ++i ) { 
            tests.quadraticComposeTest();
            tests.generalComposeTest(); 
        }
    }
    
    @Test
    public void runPipelineTests() { 
        for( int i = 0 ; i < 1 ; ++i ) {
            tests.testPipelineStage();
            tests.testRandomlyPartitionMVQ();
            tests.testNonlinearPipeline();
        }
    }
    
    @Test
    public void runMiscellaneousTests() {
        for( int i = 0 ; i < 100 ; ++i ) {
            tests.testCombination();
            tests.testConcatenateInputsAndOutputs();
            tests.testConcatenateOutputs();
            tests.monomialSetProductTest();
            tests.testRowProduct();
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
        for( int i = 0 ; i < 1; ++i ) {
            tests.productTest();
        }
    }
    
    @Test
    public void runMostFrequentFactor() {
        for( int i = 0 ; i < 5; ++i ) {
            tests.mostFrequentFactorTest();
        }
    }
    
    @Test
    public void runEvaluationTests() {
        for( int i = 0 ; i < 50000 ; ++i ) { 
            tests.identityTest();
            tests.denseRandomMVQTest();
        }
    }
    
    @Test
    public void runSerializationTests() {
        for( int i = 0 ; i < 10; ++i ) {
            tests.testToFromString();
        }
    }
    
    @Test
    public void runParallelizationTests() {
        for( int i = 0 ; i < 1 ; ++i ) {
            tests.testParallelization();
        }
    }
    
    @Test
    public void runBuilderTests() {
        for( int i = 0 ; i < 1000; ++i ) {
            tests.builderTest();
        }
    }
}

