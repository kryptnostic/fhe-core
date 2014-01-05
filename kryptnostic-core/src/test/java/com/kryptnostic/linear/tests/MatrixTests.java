package com.kryptnostic.linear.tests;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;

public class MatrixTests {
    private static final Logger logger = LoggerFactory.getLogger( MatrixTests.class );
    
    @Test
    public void inverseTest() throws SingularMatrixException {
        int count = 0 ;
        boolean success = false;
        while( count < 10 ) {
            EnhancedBitMatrix m = EnhancedBitMatrix.randomSqrMatrix( 10 );
            EnhancedBitMatrix mcpy = new EnhancedBitMatrix( m );
            EnhancedBitMatrix minv;
            try {
                minv = m.inverse();
                Assert.assertEquals( m , mcpy);
                EnhancedBitMatrix identity = minv.multiply( m );
                Assert.assertEquals( identity , EnhancedBitMatrix.identity( 10 ) );
                count = 10;
                success = true;
            } catch( SingularMatrixException e ) {
                logger.error( "Singlur matrices cannot be inverted." , e );
                ++count;
            }
            logger.info("M^-1 = {}", m );
        }
        Assert.assertEquals( success , true );
    }
    
    @Test
    public void vectorMultipltyTest() {
        /*
         * Build out predetermined matrix
         * 
         * 1 0 1 1
         * 0 1 0 1
         */ 

        BitVector row1 = new BitVector( 4 );
        BitVector row2 = new BitVector( 4 );
        
        row1.set( 0 );row1.set( 2 );row1.set( 3 );
        row2.set( 1 );row2.set( 3 );
        
        /*         
         * Build out predetermined vectors
         * 
         * v1 -> { 0 , 0 }
         * 1
         * 1
         * 0
         * 1
         * 
         * v2 -> { 0 , 0 }
         * 1
         * 0
         * 1
         * 0
         * 
         * v3 -> { 1 , 1 }
         * 0
         * 1
         * 1
         * 0
         */
        
        BitVector v1 = new BitVector( 4 );
        v1.set( 0 );v1.set( 1 );v1.set( 3 );
        
        BitVector v2 = new BitVector( 4 );
        v2.set( 0 );v2.set( 2 );
        
        BitVector v3 = new BitVector( 4 );
        v3.set( 1 );v3.set( 2 );
        
        EnhancedBitMatrix m = new EnhancedBitMatrix( Lists.newArrayList( row1, row2 ) );
        
        logger.info( "v1 = {} , v2 = {} , v3 = {}" , v1 , v2 , v3 );
        
        BitVector r1 = m.multiply( v1 );
        Assert.assertEquals( r1.size() , 2 );
        BitVector r2 = m.multiply( v2 );
        Assert.assertEquals( r2.size() , 2 );
        BitVector r3 = m.multiply( v3 );
        Assert.assertEquals( r3.size() , 2 );
        logger.info( "r1 = {} , r2 = {} , r3 = {}" , r1 , r2 , r3 );
        
        Assert.assertEquals( r1.get( 0 ) , false );
        Assert.assertEquals( r1.get( 1 ) , false );
        
        Assert.assertEquals( r2.get( 0 ) , false );
        Assert.assertEquals( r2.get( 1 ) , false );
        
        Assert.assertEquals( r3.get( 0 ) , true );
        Assert.assertEquals( r3.get( 1 ) , true );
        
        
    }
    
    
}
