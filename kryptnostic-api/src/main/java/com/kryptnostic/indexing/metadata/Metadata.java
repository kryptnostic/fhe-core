package com.kryptnostic.indexing.metadata;

import java.util.List;
import java.util.Map;

import cern.colt.bitvector.BitVector;

public interface Metadata {
    Map<String, List<Metadatum>> getMetadataMap();
    public List<BitVector> getNonces();
}
