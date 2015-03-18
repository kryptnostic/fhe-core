package com.kryptnostic.multivariate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.test.AbstractInstrumentedTest;


public class PolynomialFunctionTestsDriver extends AbstractInstrumentedTest  {
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionTestsDriver.class );
    private final PolynomialFunctionTests tests;
    
    public PolynomialFunctionTestsDriver() {
        super( PolynomialFunctionTests.class );
        tests = getTestContext().getBean( PolynomialFunctionTests.class );
    }
    
    @BeforeClass
    public static void logTestStart() {
        logger.debug("Tests starting...");
    }
    
    @AfterClass
    public static void logTestStop() {
        logger.debug("Tests stopping...");
    }
    
    @Test
    public void runFastCompose() {
        tests.mvqCompostTest();
    }
    
    @Test
    public void testBucketing() {
        tests.bucketTest();
    }
    
    @Test
    public void runCompose() {
        for( int i = 0 ; i < 1 ; ++i ) { 
            tests.quadraticComposeTest();
            tests.generalComposeTest();
            tests.partialComposeTest();
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
            tests.testInterleaveFunctions();
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
    
    @Before
    public void testStart() {
        logger.debug( "Starting test." );
    }
    
    @After
    public void testStop() {
        logger.debug( "Finishing test." );
    }
}

