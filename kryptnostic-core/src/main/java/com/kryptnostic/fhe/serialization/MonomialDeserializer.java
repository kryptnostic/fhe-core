package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.util.Monomials;

public class MonomialDeserializer extends JsonDeserializer<Monomial>{

    @Override
    public Monomial deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return Monomials.unmarshallMonomial(jp.getValueAsString()); 
    }

}
