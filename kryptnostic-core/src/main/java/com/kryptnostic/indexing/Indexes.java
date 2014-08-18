package com.kryptnostic.indexing;

import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class Indexes {
	public static SimplePolynomialFunction generateRandomIndexingFunction( int tokenLength, int nonceLength, int locationLength ) {
		SimplePolynomialFunction outer = PolynomialFunctions.denseRandomMultivariateQuadratic( locationLength , locationLength );
		SimplePolynomialFunction inner = PolynomialFunctions.unsafeRandomManyToOneLinearCombination( tokenLength + nonceLength , locationLength );
		return outer.compose( inner );
	}

}
