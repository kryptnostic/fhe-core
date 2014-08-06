package com.kryptnostic.multivariate.learning;

import java.util.Set;

import com.kryptnostic.multivariate.FunctionUtils;
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
		Set<Monomial> monomials = new Monomial( function.getInputLength() ).subsetsOfSize();
		

		
		// generate enhanced bit matrix to represent system of equations
		// compute rowReducedEchelonForm
		// use this matrix to create contributions vector for monomials and generate inverse polynomial
		return null;
	}

	
	
}