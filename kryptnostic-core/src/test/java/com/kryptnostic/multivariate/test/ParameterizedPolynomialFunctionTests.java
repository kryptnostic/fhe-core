package com.kryptnostic.multivariate.test;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.ImmutableList;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.parameterization.ParameterizedPolynomialFunctionGF2;


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
		
		BitVector input = BitUtils.randomVector(inputLength);
		BitVector expected = f.apply(input);
		expected.and(g.apply(input));
		
		
	}
}
