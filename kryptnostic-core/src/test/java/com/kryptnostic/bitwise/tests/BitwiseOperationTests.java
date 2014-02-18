package com.kryptnostic.bitwise.tests;

import org.junit.Test;

import com.kryptnostic.bitwise.AndOperation;
import com.kryptnostic.bitwise.NotOperation;
import com.kryptnostic.bitwise.OrOperation;
import com.kryptnostic.bitwise.XorOperation;
import com.kryptnostic.linear.BitUtils;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class BitwiseOperationTests {
    @Test
    public void XorTest() {
        XorOperation xor = new XorOperation( 64 );
        
        BitVector v1 = BitUtils.randomVector( 64 ) , 
                  v2 = BitUtils.randomVector( 64 );
        
        BitVector actual = xor.apply( v1 , v2 );
        BitVector expected = v1.copy();
        expected.xor( v2 );
        
        Assert.assertEquals( expected , actual );
    }
    
    @Test
    public void AndTest() {
        AndOperation and = new AndOperation( 64 );
        
        BitVector v1 = BitUtils.randomVector( 64 ) , 
                  v2 = BitUtils.randomVector( 64 );
        
        BitVector actual = and.apply( v1 , v2 );
        BitVector expected = v1.copy();
        expected.and( v2 );
        
        Assert.assertEquals( expected , actual );
    }
    
    @Test
    public void OrTest() {
        OrOperation or = new OrOperation( 64 );
        
        BitVector v1 = BitUtils.randomVector( 64 ) , 
                  v2 = BitUtils.randomVector( 64 );
        
        BitVector actual = or.apply( v1 , v2 );
        BitVector expected = v1.copy();
        expected.or( v2 );
        
        Assert.assertEquals( expected , actual );
    }
    
    @Test
    public void NotTest() {
        NotOperation not = new NotOperation( 64 );
        
        BitVector v1 = BitUtils.randomVector( 64 ); 
        
        BitVector actual = not.apply( v1 );
        BitVector expected = v1.copy();
        expected.not();
        
        Assert.assertEquals( expected , actual );
    }
}
