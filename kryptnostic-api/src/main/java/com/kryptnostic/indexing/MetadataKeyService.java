package com.kryptnostic.indexing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kryptnostic.indexing.metadata.Metadatum;

/**
 * MetadataKeyService handles mapping tokens and nonces to lookup keys.
 * @author Matthew Tamayo-Rios <matthew@kryptnostic.com>
 *
 */
public interface MetadataKeyService {
	Map<String,List<Metadatum>> mapTokensToKeys( Set<Metadatum> metadata ); 
}
