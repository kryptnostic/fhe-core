package com.krytpnostic.multivariate.test;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;
import junit.framework.Assert;


public class PolynomialFunctionTests {
    private static final Logger logger = LoggerFactory.getLogger( PolynomialFunctionTests.class );
    private static final Random r = new Random(0);
    @Test
    public void builderTest() {
        PolynomialFunctionGF2.Builder builder = PolynomialFunctionGF2.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = BitUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunctionGF2 f = builder.build();
        BitVector result = f.apply( BitUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
    @Test
    public void evaluationTest() {
        PolynomialFunctionGF2.Builder builder = PolynomialFunctionGF2.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = BitUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);
        }
        
        PolynomialFunctionGF2 f = builder.build();
        BitVector result = f.apply( BitUtils.randomVector( 256 ) );
        logger.info( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
   @Test
   public void identityTest() {
       PolynomialFunctionGF2 f = PolynomialFunctionGF2.identity( 256 );
       BitVector input = BitUtils.randomVector( 256 );
       
       Assert.assertEquals( input , f.apply( input ) );
   }
   
   @Test
   public void monomialSetProductTest() {
       Set<Monomial> mset = Sets.newHashSet();
       Set<BitVector> cset = Sets.newHashSet();
       while( mset.size() < 256 ) {
           if( mset.add( Monomial.randomMonomial( 256 , 4 ) ) ) {
               cset.add(BitUtils.randomVector( 256 ));
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
       Set<Monomial> actual = PolynomialFunctionGF2.product( rowA , rowB );
       Assert.assertEquals( expected , actual );
   }
   
   @Test 
   public void addTest() {
       PolynomialFunctionGF2 lhs = PolynomialFunctionGF2.randomFunction(256, 256);
       PolynomialFunctionGF2 rhs = PolynomialFunctionGF2.randomFunction(256, 256);
       BitVector val = BitUtils.randomVector( 256 ) ;
       BitVector expected = lhs.apply( val );
       expected.xor( rhs.apply( val ) );
       Assert.assertEquals( expected, lhs.xor( rhs ).apply( val ) );
   }
   
   @Test
   public void productTest() {
       PolynomialFunctionGF2 lhs = PolynomialFunctionGF2.randomFunction(256, 256);
       PolynomialFunctionGF2 rhs = PolynomialFunctionGF2.randomFunction(256, 256);
       BitVector val = BitUtils.randomVector( 256 ) ;
       BitVector expected = lhs.apply( val );
       expected.and( rhs.apply( val ) );
       Assert.assertEquals( expected, lhs.and( rhs ).apply( val ) );
   }
   
   @Test 
   public void mostFrequentFactorTest() {
       Monomial[] monomials = new Monomial[] { 
         new Monomial( 256 ).chainSet( 0 ).chainSet(1) ,
         new Monomial( 256 ).chainSet( 0 ).chainSet(1).chainSet(2) ,
         new Monomial( 256 ).chainSet( 0 ).chainSet(1).chainSet(2).chainSet(3) ,
         new Monomial( 256 ).chainSet( 0 ).chainSet(1).chainSet(4) ,
       };
       BitVector[] contributions = new BitVector[] {
               BitUtils.randomVector( 256 ) ,
               BitUtils.randomVector( 256 ) ,
               BitUtils.randomVector( 256 ) ,
               BitUtils.randomVector( 256 )
       };
       
       
       Map<Monomial, Set<Monomial>> memoizedComputations = PolynomialFunctionGF2.initializeMemoMap( 256 , monomials , contributions );
       Map<Monomial, List<Monomial>> possibleProducts = PolynomialFunctionGF2.allPossibleProduct( memoizedComputations.keySet() );  // 1
       Monomial mostFrequent = PolynomialFunctionGF2.mostFrequentFactor( monomials , possibleProducts.keySet() , ImmutableSet.<Monomial>of() );
       logger.info( "Most frequent monomial found: {}" , mostFrequent );
       Assert.assertEquals( new Monomial( 256 ).chainSet( 0 ).chainSet( 1 ) , mostFrequent );
   }
   
   @Test
   public void composeTest() {
       PolynomialFunctionGF2 outer = PolynomialFunctionGF2.randomFunction(256, 256);
       PolynomialFunctionGF2 inner = PolynomialFunctionGF2.randomFunction(256, 256);
       SimplePolynomialFunction composed = outer.compose( inner );
       
       for( int i = 0 ; i < 25 ; ++i ) {
           BitVector randomInput = BitUtils.randomVector( 256 );
           BitVector innerResult = inner.apply( randomInput );
           BitVector outerResult = outer.apply( innerResult );
           BitVector composedResult = composed.apply( randomInput );
           logger.info("Random input: {}" , randomInput );
           logger.info("Inner result: {}" , innerResult );
           logger.info("Outer result: {}" , outerResult );
           logger.info("Composed result: {}" , composedResult );
           Assert.assertEquals( outerResult , composedResult );
       }
   }
   
   @Test
   public void testConcatenateInputsAndOutputs() {
       SimplePolynomialFunction lhs = PolynomialFunctionGF2.randomFunction( 128 , 128 );
       SimplePolynomialFunction rhs = PolynomialFunctionGF2.randomFunction( 128 , 128 );
       
       SimplePolynomialFunction concatenated = PolynomialFunctionGF2.concatenateInputsAndOutputs( lhs , rhs);
       long[] src = new long[] { r.nextLong() , r.nextLong() , r.nextLong() , r.nextLong() };
       BitVector input = new BitVector( src , 256 );
       BitVector lhsInput = new BitVector( new long[] { src[0] , src[1] } , 128 );
       BitVector rhsInput = new BitVector( new long[] { src[2] , src[3] } , 128 );
       
       BitVector concatenatedResult = concatenated.apply( input );
       BitVector lhsResult = lhs.apply( lhsInput );
       BitVector rhsResult = rhs.apply( rhsInput );
       
       Assert.assertEquals( lhsResult.elements()[0] , concatenatedResult.elements()[0] );
       Assert.assertEquals( lhsResult.elements()[1] , concatenatedResult.elements()[1] );
       Assert.assertEquals( rhsResult.elements()[0] , concatenatedResult.elements()[2] );
       Assert.assertEquals( rhsResult.elements()[1] , concatenatedResult.elements()[3] );
   }
   
   @Test
   public void testToFromString() {
       SimplePolynomialFunction f = PolynomialFunctionGF2.randomFunction( 256 , 128 );
       String fString = f.toString();
       logger.info( "f = {}" , fString );
       
       SimplePolynomialFunction fPrime = FunctionUtils.fromString( 256 , fString );
       Assert.assertEquals( f , fPrime );
   }

}
