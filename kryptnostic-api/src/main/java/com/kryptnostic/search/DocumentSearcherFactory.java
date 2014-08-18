package com.kryptnostic.search;

import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;


/**
 * Factory interface for classes capable of building searchers.
 * @author Matthew Tamayo-Rios <matthew@kryptnostic.com>
 */
public interface DocumentSearcherFactory {
	SimplePolynomialFunction createSearcher( String token );
}
