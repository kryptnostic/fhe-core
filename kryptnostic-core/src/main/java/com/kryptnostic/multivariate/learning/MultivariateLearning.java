package com.kryptnostic.multivariate.learning;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.pkcs11.wrapper.Functions;
import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;


/**
 * Utility methods for multivariate learning.
 * @author Nick Hewitt
 *
 */
public class MultivariateLearning {
	private static final Logger logger = LoggerFactory.getLogger( MultivariateLearning.class );
	private static final Integer MAX_INPUT_VECTORS = 1000000;

	/**
	 * Given a polynomial and an assumed order of that polynomial, computes the inverse.
	 * @param function
	 * @param order
	 * @return
	 */
	public static Pair<List<BitVector>, SimplePolynomialFunction> learnInverse(PolynomialFunction function, Integer orderOfInverse) {
		Set<Monomial> monomials = Monomial.allMonomials( function.getOutputLength() , orderOfInverse);
		monomials.add( Monomial.constantMonomial( function.getOutputLength() ) ) ;
		SimplePolynomialFunction f = functionFromMonomials( monomials );
		
		List<BitVector> functionInputs = null;
		EnhancedBitMatrix outputs, outputsTransposed, generalizedInverse = null;
		
		for (int quantityInput = monomials.size(); quantityInput < MAX_INPUT_VECTORS; quantityInput = quantityInput << 1) {
			functionInputs = generateInputs( function.getInputLength(), quantityInput );
			List<BitVector> functionOutputs = Lists.newArrayList();
			for (BitVector input : functionInputs) {
				functionOutputs.add( f.apply( function.apply( input ) ) );
			}
			
			outputs = new EnhancedBitMatrix( functionOutputs );
			outputsTransposed = outputs.tranpose();
			try {
				generalizedInverse = outputsTransposed.rightGeneralizedInverse();
			} catch (SingularMatrixException e) {
				logger.error("Error inverting evaluated monomials: " + e.toString());
			}
			if ( generalizedInverse != null) {
				break;
			}
		}
		
		EnhancedBitMatrix coefficients = new EnhancedBitMatrix( functionInputs ).tranpose().multiply( generalizedInverse );
		return Pair.of(functionInputs, coefficients.multiply( f ));
	}
	
	private static SimplePolynomialFunction functionFromMonomials( Set<Monomial> monomialSet ) {
	    Monomial[] monomials = monomialSet.toArray( new Monomial[0] );
	    BitVector[] contributions = new BitVector[ monomials.length ];
	    for( int i = 0 ; i < monomials.length ; ++i ) {
	        BitVector contribution = new BitVector( monomials.length );
	        contribution.set( i );
	        contributions[ i ] = contribution;
	    }
	    return new PolynomialFunctionGF2( monomials[ 0 ].size() , monomials.length , monomials, contributions );
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