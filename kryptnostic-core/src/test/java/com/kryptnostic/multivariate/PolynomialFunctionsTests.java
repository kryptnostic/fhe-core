package com.kryptnostic.multivariate;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class PolynomialFunctionsTests {

    @Test
    public void testLowerTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = SimplePolynomialFunctions.lowerTruncatingIdentity(128, 64);
        BitVector input = BitVectors.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitVectors.subVector(input, 0, 1), output);
    }
    
    @Test
    public void testUpperTruncatingIdentity() {
        SimplePolynomialFunction truncatingIdentity = SimplePolynomialFunctions.upperTruncatingIdentity(128, 64);
        BitVector input = BitVectors.randomVector(128);
        BitVector output = truncatingIdentity.apply(input);
        Assert.assertEquals(BitVectors.subVector(input, 1, 2), output);
    }

}
