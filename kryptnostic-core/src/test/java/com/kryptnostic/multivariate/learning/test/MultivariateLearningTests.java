package com.kryptnostic.multivariate.learning.test;

import java.util.Random;

import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.learning.MultivariateLearning;


public class MultivariateLearningTests {
	private static final Logger logger = LoggerFactory.getLogger( MultivariateLearningTests.class );
	private static final Random r = new Random(0);
	
	/**
	 * Tests that learning is accurate when the learning function is given an accurate test polynomial order. 
	 */
	@Test
	public void learnInverseTest() {
		Integer testPolynomialOrder = 2;
		Integer testPolynomialInputLength = 64;
		Integer testPolynomialOutputLength = 128;
		
		logger.info("Generating function to invert.");
		PolynomialFunction function =  PolynomialFunctions.randomFunction(testPolynomialInputLength, testPolynomialOutputLength, 4, testPolynomialOrder); // TODO randomly generate number of terms by order
		logger.info("Learning inverse function.");
		PolynomialFunction inverse = MultivariateLearning.learnInverse(function, testPolynomialOrder);
		
		BitVector input = BitUtils.randomVector(testPolynomialInputLength);
		BitVector output = function.apply(input);
		
		BitVector invertedOutput = inverse.apply(output);
		
		Assert.assertTrue(invertedOutput.equals(input));
	}
	
	
}