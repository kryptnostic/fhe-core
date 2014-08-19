package com.kryptnostic.indexing;

import java.util.Set;

import com.kryptnostic.indexing.metadata.Metadata;
import com.kryptnostic.indexing.metadata.Metadatum;

/**
 * MetadataKeyService handles mapping tokens and nonces to lookup keys.
 * @author Matthew Tamayo-Rios <matthew@kryptnostic.com>
 *
 */
public interface MetadataKeyService {
	Metadata mapTokensToKeys( Set<Metadatum> metadata ); 
}
