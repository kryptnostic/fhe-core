package com.kryptnostic.fhe.serialization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryptnostic.crypto.EncryptedSearchPrivateKey;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class EncryptedSearchPrivateKeyTests {

    @Test
    public void roundtripTest() throws IOException, SingularMatrixException {
        EncryptedSearchPrivateKey key = new EncryptedSearchPrivateKey( 8 );

        ObjectMapper mapper = KodexObjectMapperFactory.getObjectMapper();
        String serialized = mapper.writeValueAsString( key );

        Assert.assertNotNull( serialized );

        EncryptedSearchPrivateKey recovered = mapper.readValue( serialized, EncryptedSearchPrivateKey.class );
        Assert.assertEquals( key.getLeftSquaringMatrix(), recovered.getLeftSquaringMatrix() );
        Assert.assertEquals( key.getRightSquaringMatrix(), recovered.getRightSquaringMatrix() );
    }

}
