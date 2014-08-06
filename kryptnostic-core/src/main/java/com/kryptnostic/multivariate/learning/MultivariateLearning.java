package com.kryptnostic.multivariate.learning;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;


/**
 * Utility methods for multivariate learning.
 * @author Nick Hewitt
 *
 */
public class MultivariateLearning {
	private static final Logger logger = LoggerFactory.getLogger( MultivariateLearning.class );

	/**
	 * Given a polynomial and an assumed order of that polynomial, computes the inverse.
	 * @param function
	 * @param order
	 * @return
	 */
	public static PolynomialFunction learnInverse(PolynomialFunction function, Integer orderOfInverse) {
		// generate monomials
		Set<Monomial> monomials = new Monomial( orderOfInverse + 1 ).subsetsOfSize();
		
		// create monomials.size() of BitVector inputs to function and retain outputs
		// evaluate output BV for each monomial.
		// transpose this matrix
		// check that null space is zero if not double quantity of input
		List<BitVector> functionInputs;
		EnhancedBitMatrix outputs, outputsTransposed;
		int quantityInputVectors = monomials.size();
		do {
			functionInputs = generateInputs( function.getInputLength(), quantityInputVectors );
			List<BitVector> functionOutputs = Lists.newArrayList();
			for (BitVector input : functionInputs) {
				functionOutputs.add( function.apply( input ) );
			}
			
			outputs = new EnhancedBitMatrix( functionOutputs );
			outputsTransposed = outputs.tranpose();
			quantityInputVectors = quantityInputVectors << 2;
		} while ( !( outputs.getNullspaceBasis().rows() == 0 ) );
		
		// compute generalized inverse
		EnhancedBitMatrix generalizedInverse = null;
		try {
			generalizedInverse = outputsTransposed.rightGeneralizedInverse();
		} catch (SingularMatrixException e) {
			logger.error("Error inverting evaluated monomials: " + e.toString());
		}
		
		// multiply by plaintext to get contributions
		EnhancedBitMatrix contributions =  generalizedInverse.multiply( new EnhancedBitMatrix( functionInputs ));
		//  generate inverse polynomial
		Map<Monomial, BitVector> contributionsMap = FunctionUtils.mapViewFromMonomialsAndContributions( 
				monomials.toArray( new Monomial[monomials.size()]), 
				contributions.getRows().toArray( new BitVector[contributions.getRows().size()])); 
		PolynomialFunction inverseFunction = PolynomialFunctions.fromMonomialContributionMap(function.getOutputLength(), function.getInputLength(), contributionsMap);
		
		return inverseFunction;
	}

	/**
	 * Creates a list of randomly generated BitVectors of length specified.
	 * @param size
	 * @return
	 */
	private static List<BitVector> generateInputs(int vectorLength, int quantity) {
		List<BitVector> inputs = Lists.newArrayList();
		for (int i = 0; i < quantity; i++) {
			inputs.add( BitUtils.randomVector( vectorLength ) );
		}
		return inputs;
	}

	
	
}