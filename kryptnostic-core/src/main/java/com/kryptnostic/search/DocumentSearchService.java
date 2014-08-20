package com.kryptnostic.search;

import java.util.Set;

import cern.colt.bitvector.BitVector;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.indexing.Indexes;
import com.kryptnostic.indexing.analysis.TokenizingWhitespaceAnalyzer;
import com.kryptnostic.indexing.metadata.Metadatum;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class DocumentSearchService implements SearchService {
    private final QueryAnalyzer analyzer = new TokenizingWhitespaceAnalyzer();
    private final SimplePolynomialFunction addresser;
    private final HashFunction hf = Hashing.sha256();
    
    public DocumentSearchService( SimplePolynomialFunction hash , SimplePolynomialFunction decryptor , int tokenLength ) {
        addresser = hash.concatenatingCompose( PolynomialFunctions.identity( tokenLength ) , decryptor );
    }
    
    @Override
    public Set<Metadatum> search(String query) {
        Set<String> tokens = analyzer.analyzeQuery( query );
        Set<Metadatum> metadata = null;
        for( String token : tokens ) { 
            BitVector tokenVector = Indexes.computeHashAndGetBits( hf , token );
            SimplePolynomialFunction queryFunction = addresser.resolve( tokenVector );
            //metadata = Server.submit( queryFunction );
        }
        return metadata;
    }

}
