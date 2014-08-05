package com.kryptnostic.multivariate.learning;

import java.util.Set;

import org.junit.Assert;

import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;


/**
 * Utility methods for multivariate learning.
 * @author Nick Hewitt
 *
 */
public class MultivariateLearning {
	
	/**
	 * Given a polynomial and an assumed order of that polynomial, computes the inverse.
	 * @param function
	 * @param order
	 * @return
	 */
	public static PolynomialFunction learnInverse(PolynomialFunction function, Integer orderOfInverse) {
		// generate monomials
		Set<Monomial> monomials = generatePossibleMonomials(function.getInputLength());
		

		
		// generate enhanced bit matrix to represent system of equations
		// compute rowReducedEchelonForm
		// use this matrix to create contributions vector for monomials and generate inverse polynomial
		return null;
	}

	/**
	 * Creates a monomial for every possible unique term given an input length. The unique terms correspond
	 * to every combination of the input variables.
	 * @return
	 * @throws Exception 
	 */
	private static Set<Monomial> generatePossibleMonomials(Integer inputLength) {
		Set<Monomial> monomials = Sets.newHashSet();
		for ( int i = 0; i < inputLength; i++) {
			Monomial monomial = new Monomial( inputLength );
			for (int j = i; j < inputLength; j++) {
				monomial.set(j);
			}
			Assert.assertTrue( monomials.add( monomial ) );
		}
		return monomials;
	}
	
}