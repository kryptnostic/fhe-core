package com.kryptnostic.multivariate.test;

import org.junit.Test;

import com.kryptnostic.test.AbstractInstrumentedTest;


public class PolynomialFunctionTests extends AbstractInstrumentedTest  {
    private final PolynomialFunctionTestsConfiguration tests;
    public PolynomialFunctionTests() {
        super( PolynomialFunctionTestsConfiguration.class );
        tests = getTestContext().getBean( PolynomialFunctionTestsConfiguration.class );
    }
    
    @Test
    public void testCompose() {
        for( int i = 0 ; i < 10 ; ++i ) { 
            tests.composeTest();
        }
    }
    
    @Test
    public void testCombination() {
        for( int i = 0 ; i < 100 ; ++i ) {
            tests.testCombination(); 
        }
    }
    
    @Test
    public void testApply() {
        for( int i = 0 ; i < 500 ; ++i ) { 
            tests.identityTest();
        }
    }
    
    @Test
    public void testAssumptions() {
        for( int i = 0 ; i < 5 ; ++i ) { 
            tests.testTestAssumptions();
        }
    }
    
}

