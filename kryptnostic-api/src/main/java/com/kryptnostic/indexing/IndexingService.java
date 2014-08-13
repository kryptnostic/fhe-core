package com.kryptnostic.indexing;

import java.util.Set;

import com.kryptnostic.indexing.analysis.Analyzer;
import com.kryptnostic.indexing.metadata.Metadatum;

public interface IndexingService {
	Set<Metadatum> index( String documentId , String document );
	boolean registerAnalyzer( Analyzer analyzer );
	Set<Analyzer> getAnalyzers();
}
