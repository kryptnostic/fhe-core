package com.kryptnostic.multivariate;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.ImmutableList;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.util.PolynomialFunctions;


/**
 * @author mtamayo
 */
public class ParameterizedPolynomialFunctionTests {
//TODO: Actually test things.	
//	@Test
	public void testExtendAndMap() {
		
	}
	
//	@Test 
	public void testBinaryAnd() {
		final int inputLength = 128;
		final int outputLength = 128;
		SimplePolynomialFunction fPipeline = PolynomialFunctions.denseRandomMultivariateQuadratic(inputLength, outputLength);
		SimplePolynomialFunction gPipeline = PolynomialFunctions.denseRandomMultivariateQuadratic(inputLength, outputLength);
		SimplePolynomialFunction fRaw = PolynomialFunctions.denseRandomMultivariateQuadratic(inputLength>>1, outputLength);
		SimplePolynomialFunction gRaw = PolynomialFunctions.denseRandomMultivariateQuadratic(inputLength>>1, outputLength);
		
		SimplePolynomialFunction f = new ParameterizedPolynomialFunctionGF2(inputLength, outputLength, fRaw.getMonomials(), fRaw.getContributions(), ImmutableList.of( CompoundPolynomialFunctions.fromFunctions(fPipeline)));
		SimplePolynomialFunction g = new ParameterizedPolynomialFunctionGF2(inputLength, outputLength, gRaw.getMonomials(), gRaw.getContributions(), ImmutableList.of( CompoundPolynomialFunctions.fromFunctions(gPipeline)));
		
		SimplePolynomialFunction fg;
		
		BitVector input = BitVectors.randomVector(inputLength);
		BitVector expected = f.apply(input);
		expected.and(g.apply(input));
		
		
	}
}
