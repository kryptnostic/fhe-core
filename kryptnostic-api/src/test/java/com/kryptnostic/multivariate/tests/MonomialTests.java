package com.kryptnostic.multivariate.tests;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.gf2.Monomial;

public class MonomialTests {

    @Test
    public void testProduct() {
        Monomial m1 = Monomial.randomMonomial( 257, 128 );
        Monomial m2 = Monomial.randomMonomial( 257, 128 );

        Monomial result = m1.product( m2 );
        m1.or( m2 );
        Assert.assertArrayEquals( result.elements(), m1.elements() );
    }

    @Test
    public void testSelfProduct() {
        Monomial m = Monomial.randomMonomial( 257, 128 );
        Monomial result = m.product( m );

        Assert.assertArrayEquals( result.elements(), m.elements() );
    }

    @Test
    public void testSubsets() {
        Monomial m = new Monomial( 139 );
        m.chainSet( 0 ).chainSet( 2 ).chainSet( 10 ).chainSet( 25 );

        Set<Monomial> pairs = Sets.newHashSet(), triplets = Sets.newHashSet();

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

        Assert.assertEquals( pairs, m.subsets( 2 ) );
        Assert.assertEquals( triplets, m.subsets( 3 ) );
        Assert.assertEquals( Sets.newHashSet(), m.subsets( 5 ) );
    }

    @Test
    public void testIsFactor() {
        Monomial m = new Monomial( 139 );
        m.chainSet( 0 ).chainSet( 2 ).chainSet( 10 ).chainSet( 25 ).chainSet( 64 ).chainSet( 65 ).chainSet( 130 );
        Assert.assertEquals( true, m.hasFactor( new Monomial( 139 ).chainSet( 0 ).chainSet( 65 ) ) );
        Assert.assertEquals( true, m.hasFactor( new Monomial( 139 ).chainSet( 10 ).chainSet( 130 ) ) );
        Assert.assertEquals( true, m.hasFactor( new Monomial( 139 ).chainSet( 64 ).chainSet( 65 ) ) );
        Assert.assertEquals( false, m.hasFactor( new Monomial( 139 ).chainSet( 0 ).chainSet( 65 ).chainSet( 5 ) ) );
        Assert.assertEquals( false, m.hasFactor( new Monomial( 139 ).chainSet( 1 ).chainSet( 3 ).chainSet( 5 ) ) );
    }

    @Test
    public void testToFromString() {
        Monomial m = new Monomial( 65 );
        m.chainSet( 0 ).chainSet( 2 ).chainSet( 25 );
        String monomialString = m.toStringMonomial();

        Assert.assertEquals( "x1*x3*x26", monomialString );
        Monomial m2 = Monomial.fromString( 65, monomialString );
        Assert.assertEquals( m, m2 );
    }

    @Test
    public void testExtendAndShift() {
        long[] values = { 56, 34667 };
        BitVector expected = new BitVector( values, 128 );
        BitVector extendedShifted = Monomial.extendAndShift( expected, 192, 64 );
        for ( int i = 0; i < expected.size(); i++ ) {
            Assert.assertEquals( expected.get( i ), extendedShifted.get( i + 64 ) );
        }

        BitVector extendedShiftedFromBase = Monomial.extendAndShift( expected, 256, 64, 64 );
        for ( int i = 0; i < 64; i++ ) {
            Assert.assertEquals( expected.get( i ), extendedShiftedFromBase.get( i ) );
        }
        for ( int i = 64; i < 128; i++ ) {
            Assert.assertEquals( expected.get( i ), extendedShiftedFromBase.get( i + 64 ) );
        }
    }

}
