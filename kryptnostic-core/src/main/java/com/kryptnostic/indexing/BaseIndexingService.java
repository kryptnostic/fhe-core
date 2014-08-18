package com.kryptnostic.indexing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;
import com.kryptnostic.indexing.analysis.Analyzer;
import com.kryptnostic.indexing.analysis.TokenizingWhitespaceAnalyzer;
import com.kryptnostic.indexing.metadata.BaseMetadatum;
import com.kryptnostic.indexing.metadata.Metadatum;

public class BaseIndexingService implements IndexingService {
	private final Set<Analyzer> analyzers;
	
	public BaseIndexingService() {
		analyzers = Sets.<Analyzer>newHashSet( new TokenizingWhitespaceAnalyzer() );
	}
	
	@Override
	public Set<Metadatum> index(String documentId, String document) {
		Set<Metadatum> metadata = Sets.newHashSet();
		for( Analyzer analyzer : analyzers ) {
			Map<String, List<Integer>> invertedIndex = analyzer.analyze( document );
			for( Entry<String, List<Integer>> entry : invertedIndex.entrySet() ) {
				String token = entry.getKey();
				List<Integer> locations = entry.getValue();
				metadata.add( new BaseMetadatum(documentId, token, locations) );
			}
		}
		return metadata;
	}

	@Override
	public boolean registerAnalyzer(Analyzer analyzer) {
		return analyzers.add( analyzer );
	}

	@Override
	public Set<Analyzer> getAnalyzers() {
		return Sets.newHashSet( analyzers );
	}

}
