package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.kryptnostic.bitwise.BitVectors;

public class BitVectorKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey( String key, DeserializationContext ctxt ) throws IOException, JsonProcessingException {
        if ( key.length() == 0 ) { // [JACKSON-360]
            return null;
        }

        return BitVectors.unmarshalBitvector( key );
    }
}
