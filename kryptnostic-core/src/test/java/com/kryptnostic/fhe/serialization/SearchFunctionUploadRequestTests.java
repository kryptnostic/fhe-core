package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class SearchFunctionUploadRequestTests extends SerializationTestUtils {
    private static final int LEN = 256;

    @Test
    public void serializeTest() throws JsonGenerationException, JsonMappingException, IOException {
        SimplePolynomialFunction function = SimplePolynomialFunctions.lightRandomFunction(LEN, LEN);
        SearchFunctionUploadRequest req = new SearchFunctionUploadRequest(function);

        String str = "{\"function\":" + serialize(function) + "}";

        Assert.assertEquals(str, serialize(req));
    }

    @Test
    public void deserializeTest() throws JsonParseException, JsonMappingException, IOException {
        SimplePolynomialFunction function = SimplePolynomialFunctions.lightRandomFunction(LEN, LEN);
        SearchFunctionUploadRequest req = new SearchFunctionUploadRequest(function);

        String str = "{\"function\":" + mapper.writeValueAsString(function) + "}";

        SearchFunctionUploadRequest out = deserialize(str, SearchFunctionUploadRequest.class);

        Assert.assertEquals(req.getFunction(), out.getFunction());
    }
}
