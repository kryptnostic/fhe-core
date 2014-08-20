package com.kryptnostic.search;

import java.util.Set;

public interface QueryAnalyzer {
    Set<String> analyzeQuery( String query );
}
