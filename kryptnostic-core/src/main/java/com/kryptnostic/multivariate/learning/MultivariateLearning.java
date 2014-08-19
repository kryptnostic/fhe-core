package com.kryptnostic.multivariate.learning;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
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
	 * @return Pair of inverse function and training data over which it was valid
	 */
	public static Pair<SimplePolynomialFunction, List <BitVector>> learnInverse(PolynomialFunction function, Integer orderOfInverse) {
		Set<Monomial> monomials = Monomial.allMonomials( function.getOutputLength() , orderOfInverse);
		SimplePolynomialFunction monomialsFunction = functionFromMonomials( monomials );
		
		List<BitVector> functionInputs = null;
		EnhancedBitMatrix coefficients = null;
		
		for (int quantityInput = monomials.size(); quantityInput < MAX_INPUT_VECTORS; quantityInput = quantityInput << 1) {
			Pair<List<BitVector>,List<BitVector>> trainingData = getTrainingData( function, quantityInput);
			functionInputs = trainingData.getLeft();
			List<BitVector> functionOutputs = Lists.newArrayList();
			for (BitVector output : trainingData.getRight()) {
				functionOutputs.add( monomialsFunction.apply( output ) );
			}
			
			coefficients = learnCoefficients(functionOutputs, functionInputs);
			if ( coefficients != null) {
				logger.info("Succesfully inverted evaluated monomials with " + quantityInput + " inputs");
				break;
			}
		}
		if ( coefficients == null ) {
        	logger.info("Unable to find an inverse.");
        	return null; 
        }
        return Pair.of( coefficients.multiply( monomialsFunction ) , functionInputs );
	}
	
	/**
     * Given a polynomial and an assumed order of that polynomial, computes a representative polynomial of known coefficients
     * and monomials.
     * @param function
     * @param order
     * @return Pair of function and training data over which it was valid
     */
    public static Pair<SimplePolynomialFunction, List <BitVector>> learnFunction(PolynomialFunction function, Integer order) {
        Set<Monomial> monomials = Monomial.allMonomials( function.getInputLength() , order);
        SimplePolynomialFunction monomialsFunction = functionFromMonomials( monomials );
        
        List<BitVector> functionInputs = null, extendedInputs;
        EnhancedBitMatrix coefficients = null;
        
        for (int quantityInput = monomials.size(); quantityInput < MAX_INPUT_VECTORS; quantityInput = quantityInput << 1) {
            Pair<List<BitVector>,List<BitVector>> trainingData = getTrainingData(function, quantityInput);
        	functionInputs = trainingData.getLeft();
            List<BitVector> functionOutputs = trainingData.getRight();
            extendedInputs = Lists.newArrayListWithCapacity( functionInputs.size() );
            for(BitVector input : functionInputs){
                extendedInputs.add( monomialsFunction.apply( input ) );
            }
            
            coefficients = learnCoefficients(extendedInputs, functionOutputs);
            if ( coefficients != null) {
            	logger.info("Succesfully inverted evaluated monomials with " + quantityInput + " inputs");
                break;
            }
        }
        if ( coefficients == null ) {
        	logger.info("Unable to find an inverse.");
        	return null; 
        }
        return Pair.of( coefficients.multiply( monomialsFunction ) , functionInputs );
    }
	
    /**
	 * Attempts to compute the coefficients of a supplied polynomial function, given training data.
	 * Returns null on failure to learn coefficients.
	 */
	public static EnhancedBitMatrix learnCoefficients(List<BitVector> inputs, List<BitVector> outputs) {
		EnhancedBitMatrix coefficients;
		
		EnhancedBitMatrix outputsTransposed = new EnhancedBitMatrix( outputs ).tranpose();
		EnhancedBitMatrix inputsTransposed = new EnhancedBitMatrix( inputs ).tranpose();
		try {
			EnhancedBitMatrix generalizedInverseInputs = inputsTransposed.rightGeneralizedInverse();
			coefficients = outputsTransposed.multiply( generalizedInverseInputs );
		} catch (SingularMatrixException e) {
			logger.info( e.toString() );
			return null;
		}
		
		return coefficients;
		
	}
	
    /**
     * Create polynomial with unit contribution from every monomial in the set given.
     * @param monomialSet
     * @return
     */
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
	 * Returns a list of random inputs and the corresponding list of outputs for the given function.
	 * @param function
	 * @param quantity
	 * @return
	 */
	private static Pair<List<BitVector>, List<BitVector>> getTrainingData(
			PolynomialFunction function, int quantity) {
		List<BitVector> inputs = Lists.newArrayList();
		List<BitVector> outputs = Lists.newArrayList();
		for (int i = 0; i < quantity; i++) {
			inputs.add( BitUtils.randomVector( function.getInputLength() ) );
		}
		
		for (BitVector input : inputs) {
			outputs.add( function.apply( input ) );
		}
		
		return Pair.of(inputs, outputs);
	}
}