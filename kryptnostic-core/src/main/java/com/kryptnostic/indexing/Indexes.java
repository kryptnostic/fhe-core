package com.kryptnostic.indexing;

import java.nio.ByteBuffer;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashFunction;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class Indexes {
	public static SimplePolynomialFunction generateRandomIndexingFunction( int tokenLength, int nonceLength, int locationLength ) {
		SimplePolynomialFunction outer = PolynomialFunctions.denseRandomMultivariateQuadratic( locationLength , locationLength );
		SimplePolynomialFunction inner = PolynomialFunctions.unsafeRandomManyToOneLinearCombination( tokenLength + nonceLength , locationLength );
		return outer.compose( inner );
	}

	public static BitVector computeHashAndGetBits( HashFunction hf , String token ) {
	    //TODO: Make this a salted hash and persist the hash as part of the private key on the phone.
	       byte[] hash = hf.hashString(token, Charsets.UTF_8).asBytes();
	        //TODO: Consider padding output to a multiple of 8
	        Preconditions.checkState( hash.length % 8 == 0 , "Output length of has function must be a multiple of 8.");
	        long [] raw = new long[ hash.length >>> 3 ];
	        ByteBuffer.wrap( hash ).asLongBuffer().get( raw );
	        
	        return new BitVector( raw , raw.length << 6 );
	}
}
