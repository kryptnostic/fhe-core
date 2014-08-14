package com.kryptnostic.indexing;

import cern.colt.bitvector.BitVector;

public interface MetadataKeyService {
	String getKey( String token , BitVector nonce );
}
