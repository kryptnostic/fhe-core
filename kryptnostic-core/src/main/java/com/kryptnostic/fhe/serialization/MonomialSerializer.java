package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.util.Monomials;

public class MonomialSerializer extends JsonSerializer<Monomial> {

    @Override
    public void serialize(Monomial value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
            JsonProcessingException {
        String out = Monomials.marshallMonomial(value);
        jgen.writeString(out);
    }

}
