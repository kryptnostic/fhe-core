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
		Integer testPolynomialInputLength = 8;
		Integer testPolynomialOutputLength = 16;
		
		logger.info("Generating function to invert.");
		PolynomialFunction function =  PolynomialFunctions.identity( testPolynomialInputLength ); // TODO randomly generate number of terms by order
		logger.info("Learning inverse function.");
		Pair<List<BitVector>, SimplePolynomialFunction> stuff = MultivariateLearning.learnInverse(function, testPolynomialOrder);
		PolynomialFunction inverse = stuff.getRight();
		BitVector input = stuff.getLeft().get(0);//BitUtils.randomVector(testPolynomialInputLength);
		BitVector output = function.apply(input);
		
		
		BitVector invertedOutput = inverse.apply(output);
		
		
		Assert.assertTrue(invertedOutput.equals(input));
	}
	
	
}