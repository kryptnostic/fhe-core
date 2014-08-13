package com.kryptnostic.multivariate.learning.test;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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
//	@Test
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
        PolynomialFunction function =  PolynomialFunctions.randomFunction(testPolynomialInputLength, testPolynomialOutputLength, 4, testPolynomialOrder);
        logger.info("Learning function.");
        Pair<SimplePolynomialFunction, List<BitVector>> learnedInfo = MultivariateLearning.learnFunction( function, testPolynomialOrder);
        SimplePolynomialFunction learned = learnedInfo.getLeft();
        List<BitVector> inputs = learnedInfo.getRight();
        
        Assert.assertEquals( function.apply( inputs.get( 25 ) ) , learned.apply( inputs.get( 25 ) ) );
        
        BitVector input = BitUtils.randomVector(testPolynomialInputLength);
        
        Assert.assertEquals( function.apply( input ) , learned.apply( input ) );
    }
	
	/**
	 * STATISTICAL TESTING
	 */
	
	public void learnFunctionVarySize( boolean learnInverse ) {
		Integer testPolynomialOrder = 2;
        Integer minSize = 8;
        Integer maxSize = 16;
        Integer samplingPercent = 10;
		for (int inputLength = minSize; inputLength <= maxSize; inputLength = inputLength << 1) {
			logger.info("Testing inverse:" + learnInverse);
			logger.info("Testing learnInverse for function of input length: " + inputLength);
        	int outputLength = inputLength << 1;
        	PolynomialFunction function =  PolynomialFunctions.randomFunction( inputLength, outputLength, inputLength, testPolynomialOrder);
        	
        	Pair<SimplePolynomialFunction, List<BitVector>> learnedInfo;
        	SummaryStatistics stats;
        	if ( learnInverse ) {
    			learnedInfo = MultivariateLearning.learnInverse( function, testPolynomialOrder);
    			stats = measureInverseAccuracy(function, learnedInfo.getLeft(), samplingPercent);
        	} else {
        		learnedInfo = MultivariateLearning.learnFunction( function, testPolynomialOrder);
        		stats = measureFunctionAccuracy(function, learnedInfo.getLeft(), samplingPercent);
        	} 
			logger.info(stats.toString());
        }
	}
	
	public void learnFunctionVaryOrder( boolean learnInverse ) {
		Integer minOrder = 1;
		Integer maxOrder = 3;
		Integer actualOrder = 2;
		
        Integer inputLength = 8;
        Integer outputLength = 16;
        Integer samplingPercent = 10;
		for (int order = minOrder; order <= maxOrder; order++) {
			logger.info("Testing inverse:" + learnInverse);
        	logger.info("Testing for function of order" + order);
        	PolynomialFunction function =  PolynomialFunctions.randomFunction( inputLength, outputLength, inputLength, actualOrder);
        	
        	Pair<SimplePolynomialFunction, List<BitVector>> learnedInfo;
        	SummaryStatistics stats;
        	if ( learnInverse ) {
    			learnedInfo = MultivariateLearning.learnInverse( function, order);
    			stats = measureInverseAccuracy(function, learnedInfo.getLeft(), samplingPercent);
        	} else {
        		learnedInfo = MultivariateLearning.learnFunction( function, order);
        		stats = measureFunctionAccuracy(function, learnedInfo.getLeft(), samplingPercent);
        	} 
			logger.info(stats.toString());
        }
	}
	
	/**
	 * Evaluates the precision of the learned function relative to the known function, in terms of the Hamming weight of 
	 * the expected output XORed with the found output, over the percent possible inputs specified.
	 * @param expected
	 * @param found
	 * @param percentCoverage
	 * @return
	 */
	private static SummaryStatistics measureFunctionAccuracy(PolynomialFunction function, PolynomialFunction learnedFunction, double percentCoverage) {
		double numPossibleInputs = Math.pow(2, function.getInputLength() - 1);
		long numSamples = ((Long) Math.round(( percentCoverage / 100 ) * numPossibleInputs)).intValue();
		
		SummaryStatistics stats = new SummaryStatistics();
		for (long i = 0; i < numSamples; i++) {
			BitVector input = BitUtils.randomVector( function.getInputLength() );
			BitVector expectedOutput = function.apply( input );
			BitVector foundOutput = learnedFunction.apply( input );
			
			expectedOutput.xor(foundOutput);
			int hammingWeight = expectedOutput.cardinality();
			
			stats.addValue(hammingWeight);
		}
		
		return stats;
	}
	
	/**
	 * Evaluates the precision of the inverse function relative to the known function, in terms of the Hamming weight of 
	 * the function input XORed with the input computed by the inverse  function, over the percent possible inputs specified.
	 * @param expected
	 * @param found
	 * @param percentCoverage
	 * @return
	 */
	private static SummaryStatistics measureInverseAccuracy(PolynomialFunction function, PolynomialFunction inverse, double percentCoverage) {
		Integer numPossibleInputs = 2 << function.getInputLength() - 1;
		Integer numSamples = ((Long) Math.round(( percentCoverage / 100 ) * numPossibleInputs)).intValue();
		
		SummaryStatistics stats = new SummaryStatistics();
		for (int i = 0; i < numSamples; i++) {
			BitVector input = BitUtils.randomVector( function.getInputLength() );
			BitVector output = function.apply( input );
			BitVector foundInput = inverse.apply( output );
			
			input.xor( foundInput );
			int hammingWeight = input.cardinality();
			
			stats.addValue(hammingWeight);
		}
		
		return stats;
	}
	
}