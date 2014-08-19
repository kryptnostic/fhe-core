package com.kryptnostic.indexing.metadata;

import java.util.List;
import java.util.Map;

import cern.colt.bitvector.BitVector;

public class BalancedMetadata implements Metadata {
    private final Map<String, List<Metadatum>> metadataMap;
    private final List<BitVector> nonces;
    
    public BalancedMetadata( Map<String, List<Metadatum>> metadataMap, List<BitVector> nonces ) {
        this.metadataMap=metadataMap;
        this.nonces = nonces;
    }

    public Map<String, List<Metadatum>> getMetadataMap() {
        return metadataMap;
    }

    public List<BitVector> getNonces() {
        return nonces;
    }
    
    public static Metadata from(Map<String, List<Metadatum>> metadataMap, List<BitVector> nonces) {
        return new BalancedMetadata( metadataMap , nonces );
    }
}
