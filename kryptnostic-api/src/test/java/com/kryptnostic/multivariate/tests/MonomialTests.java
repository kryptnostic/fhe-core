package com.kryptnostic.multivariate.tests;
import java.util.Arrays;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.Monomial;


public class MonomialTests {

    @Test
    public void testProduct() {
        Monomial m1 = Monomial.randomMonomial( 257 , 128 );
        Monomial m2 = Monomial.randomMonomial( 257 , 128 );
        
        
        Monomial result = m1.product( m2 );
        m1.or(m2);
        Assert.assertArrayEquals( result.elements() , m1.elements() );
    }
    
    @Test
    public void testSelfProduct() {
        Monomial m = Monomial.randomMonomial( 257 , 128 );
        Monomial result = m.product( m );
        
        Assert.assertArrayEquals( result.elements() , m.elements() );
    }
    
    @Test
    public void testSubsets() {
        Monomial m = new Monomial( 139 );
        m.chainSet( 0 ).chainSet( 2 ).chainSet( 10 ).chainSet( 25 );
        
        Set<Monomial> pairs = Sets.newHashSet() , triplets = Sets.newHashSet();
        
        pairs.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 2 ) );
        pairs.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 10 ) );
        pairs.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 25 ) );
        pairs.add( new Monomial( 139 ).chainSet( 2 ).chainSet( 10 ) );
        pairs.add( new Monomial( 139 ).chainSet( 2 ).chainSet( 25 ) );
        pairs.add( new Monomial( 139 ).chainSet( 10 ).chainSet( 25 ) );
        
        triplets.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 2 ).chainSet( 10 ) );
        triplets.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 2 ).chainSet( 25 ) );
        triplets.add( new Monomial( 139 ).chainSet( 0 ).chainSet( 10 ).chainSet( 25 ) );
        triplets.add( new Monomial( 139 ).chainSet( 2 ).chainSet( 10 ).chainSet( 25 ) ); 
        
        Assert.assertEquals( pairs , m.subsets( 2 ) );
        Assert.assertEquals( triplets, m.subsets( 3 ) );
    }
}
