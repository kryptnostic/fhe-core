package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.kryptnostic.bitwise.BitVectors;

public class BitVectorSerializer extends JsonSerializer<BitVector> {

    @Override
    public void serialize(BitVector value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonGenerationException {
        String out = BitVectors.marshalBitvector(value);
        jgen.writeString(out);
    }

}
