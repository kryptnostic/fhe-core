package com.kryptnostic.multivariate.learning.test;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.learning.MultivariateLearning;


public class MultivariateLearningTests {
	private static final Logger logger = LoggerFactory.getLogger( MultivariateLearningTests.class );
	/**
	 * Tests that learning is accurate when the learning function is given an accurate test polynomial order. 
	 */
	@Test
	public void learnInverseTest() {
		Integer testPolynomialOrder = 1;
		Integer testPolynomialInputLength = 4;
		
		logger.info("Generating function to invert.");
		PolynomialFunction function =  PolynomialFunctions.identity( testPolynomialInputLength );
		
		logger.info("Learning inverse function.");
		Pair<SimplePolynomialFunction, List<BitVector>> learnedInfo = MultivariateLearning.learnInverse(function, testPolynomialOrder);
		SimplePolynomialFunction inverse = learnedInfo.getLeft();
		List<BitVector> inputs = learnedInfo.getRight();
		
		Assert.assertEquals( inputs.get( 0 ) , inverse.apply( function.apply( inputs.get( 0 ) ) ) );
	}
	
	@Test
    public void learnTest() {
        Integer testPolynomialOrder = 2;
        Integer testPolynomialInputLength = 8;
        Integer testPolynomialOutputLength = 16;
        
        logger.info("Generating function to learn.");
        PolynomialFunction function =  PolynomialFunctions.randomFunction(testPolynomialInputLength, testPolynomialOutputLength, 4, testPolynomialOrder); // TODO randomly generate number of terms by order
        logger.info("Learning function.");
        Pair<SimplePolynomialFunction, List<BitVector>> learnedInfo = MultivariateLearning.learnFunction( function, testPolynomialOrder);
        SimplePolynomialFunction learned = learnedInfo.getLeft();
        List<BitVector> inputs = learnedInfo.getRight();
        
        Assert.assertEquals( function.apply( inputs.get( 25 ) ) , learned.apply( inputs.get( 25 ) ) );
        
        BitVector input = BitUtils.randomVector(testPolynomialInputLength);
        
        Assert.assertEquals( function.apply( input ) , learned.apply( input ) );
    }
}