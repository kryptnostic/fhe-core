package com.kryptnostic.indexing.analysis;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TokenizingWhitespaceAnalyzer implements Analyzer {
	private static final Pattern onlyWords = Pattern.compile("([a-zA-Z]+)");

	public Map<String, List<Integer>> analyze( String source ) {
		Matcher m = onlyWords.matcher( source );
		Map<String, List<Integer>> hits = Maps.newHashMap();
		while( m.find() ) {
			int location = m.start();
			String s = m.group();
			List<Integer> locations = hits.get( s );
			if( locations == null ) {
				locations = Lists.newArrayList();
				hits.put( s ,  locations );
			} 
			locations.add( location );
		}
		
		return hits;
	}
	
}
