package com.krytpnostic.multivariate.test;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.Monomial;
import com.kryptnostic.multivariate.MultivariateUtils;
import com.kryptnostic.multivariate.PolynomialFunction;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;


public class PolynomialFunctionTests {
    private static final Random r = new Random( System.currentTimeMillis() );
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionTests.class );
    @Test
    public void builderTest() {
        PolynomialFunction.Builder builder = PolynomialFunction.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunction f = builder.build();
        BitVector result = f.evalute( MultivariateUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
    @Test
    public void evaluationTest() {
        PolynomialFunction.Builder builder = PolynomialFunction.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = MultivariateUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunction f = builder.build();
        BitVector result = f.evalute( MultivariateUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
    
   @Test
   public void productTest() {
       Monomial mask = new Monomial( 256 ).chainSet( 25 ).chainSet( 100 );
       Set<Monomial> mset = Sets.newHashSet();
       Set<BitVector> cset = Sets.newHashSet();
       while( mset.size() < 256 ) {
           if( mset.add( Monomial.randomMonomial( 256 , 4 ) ) ) {
               cset.add(MultivariateUtils.randomVector( 256 ));
           }
       }
       List<Monomial> monomials = Lists.newArrayList( mset );
       List<BitVector> contributions = Lists.newArrayList( cset );
       Set<Monomial> rowA = Sets.newHashSet();
       Set<Monomial> rowB = Sets.newHashSet();
       
       for( int i = 0 ; i < contributions.size() ; ++i ) {
           BitVector contribution = contributions.get(i);
           if( contribution.get( 25 ) ) {
               rowA.add( monomials.get( i ) );
           }
           if( contribution.get( 100 ) ) {
               rowB.add( monomials.get( i ) );
           }
       }
       logger.info("Row A: {}", rowA);
       logger.info("Row B: {}", rowB);
       Set<Monomial> expected = Sets.newHashSet();
       
       for( Monomial mA : rowA ) {
           for( Monomial mB : rowB ) {
               Monomial product = mA.product( mB );
               if( !expected.add( product ) ) {
                   expected.remove( product );
               } 
           }
       }
       logger.info("Expected: {}", expected );
       Set<Monomial> actual = PolynomialFunction.product( mask , monomials , contributions );
       Assert.assertEquals( expected , actual );
   }
}
