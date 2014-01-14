package com.kryptnostic.multivariate.tests;
import org.junit.Assert;
import org.junit.Test;

import com.kryptnostic.multivariate.Monomial;


public class MonomialTests {

    @Test
    public void testProduct() {
        Monomial m1 = Monomial.randomMonomial( 257 , 128 );
        Monomial m2 = Monomial.randomMonomial( 257 , 128 );
        
        
        Monomial result = m1.product( m2 );
        m1.and(m2);
        Assert.assertArrayEquals( result.elements() , m1.elements() );
    }
}
