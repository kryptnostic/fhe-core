package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.kryptnostic.bitwise.BitVectors;

public class BitVectorDeserializer extends JsonDeserializer<BitVector> {

    @Override
    public BitVector deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
        return BitVectors.unmarshalBitvector(jp.getValueAsString());
    }

}
