package com.kryptnostic.multivariate;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.PolynomialFunctions;

public class PolynomialFunctionsTests {

    @Test
    public void testLowerTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = PolynomialFunctions.lowerTruncatingIdentity(128, 64);
        BitVector input = BitVectors.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitVectors.subVector(input, 0, 1), output);
    }
    
    @Test
    public void testUpperTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = PolynomialFunctions.upperTruncatingIdentity(128, 64);
        BitVector input = BitVectors.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitVectors.subVector(input, 1, 2), output);
    }

}
