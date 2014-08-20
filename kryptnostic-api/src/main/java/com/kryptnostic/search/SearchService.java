package com.kryptnostic.search;

import java.util.Set;

import com.kryptnostic.indexing.metadata.Metadatum;

public interface SearchService {
	public Set<Metadatum> search( String query );
}
