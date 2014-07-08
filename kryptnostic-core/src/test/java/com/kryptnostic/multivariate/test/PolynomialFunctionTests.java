package com.kryptnostic.multivariate.test;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;


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
        logger.trace( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }

    @Test
    public void denseRandomMVQTest() {
        SimplePolynomialFunction f = PolynomialFunctions.denseRandomMultivariateQuadratic(256 , 256);
        Assert.assertEquals( 256 , f.getInputLength() );
        Assert.assertEquals( 256 , f.getOutputLength() );
        for( Monomial m : f.getMonomials() ) {
            Assert.assertTrue( m.cardinality() > 0 );
            Assert.assertTrue( m.cardinality() <= 2 );
        }
        
        BitVector input = BitUtils.randomVector( f.getInputLength() );
        
        BitVector result = f.apply(input);
        Assert.assertNotNull( result );
        Assert.assertEquals( f.getOutputLength(),  result.size() );
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
        logger.trace( "Result: {}" , result );
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
       logger.trace("Row A: {}", rowA);
       logger.trace("Row B: {}", rowB);
       Set<Monomial> expected = Sets.newHashSet();
       
       for( Monomial mA : rowA ) {
           for( Monomial mB : rowB ) {
               Monomial product = mA.product( mB );
               if( !expected.add( product ) ) {
                   expected.remove( product );
               } 
           }
       }
       logger.trace("Expected: {}", expected );
       Set<Monomial> actual = PolynomialFunctionGF2.product( rowA , rowB );
       Assert.assertEquals( expected , actual );
   }
   
   @Test 
   public void addTest() {
       SimplePolynomialFunction lhs = PolynomialFunctions.randomFunction(256, 256);
       SimplePolynomialFunction rhs = PolynomialFunctions.randomFunction(256, 256);
       BitVector val = BitUtils.randomVector( 256 ) ;
       BitVector expected = lhs.apply( val );
       expected.xor( rhs.apply( val ) );
       Assert.assertEquals( expected, lhs.xor( rhs ).apply( val ) );
   }
   
   @Test
   public void productTest() {
       SimplePolynomialFunction lhs = PolynomialFunctions.randomFunction(256, 256);
       SimplePolynomialFunction rhs = PolynomialFunctions.randomFunction(256, 256);
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
       logger.trace( "Most frequent monomial found: {}" , mostFrequent );
       Assert.assertEquals( new Monomial( 256 ).chainSet( 0 ).chainSet( 1 ) , mostFrequent );
   }
   
   @Test
   public void composeTest() {
	   final int BIT_SIZE = 256;
       SimplePolynomialFunction outer = PolynomialFunctions.randomFunction(BIT_SIZE, BIT_SIZE, 10, 2);
       SimplePolynomialFunction inner = PolynomialFunctions.randomFunction(BIT_SIZE, BIT_SIZE, 10, 2);
       SimplePolynomialFunction composed = outer.compose( inner );
       
       for( int i = 0 ; i < 25 ; ++i ) {
           BitVector randomInput = BitUtils.randomVector( BIT_SIZE );
           BitVector innerResult = inner.apply( randomInput );
           BitVector outerResult = outer.apply( innerResult );
           BitVector composedResult = composed.apply( randomInput );
           logger.trace("Random input: {}" , randomInput );
           logger.trace("Inner result: {}" , innerResult );
           logger.trace("Outer result: {}" , outerResult );
           logger.trace("Composed result: {}" , composedResult );
           Assert.assertEquals( outerResult , composedResult );
       }
   }
   
   @Test
   public void testConcatenateInputsAndOutputs() {
       SimplePolynomialFunction lhs = PolynomialFunctions.randomFunction( 128 , 128 );
       SimplePolynomialFunction rhs = PolynomialFunctions.randomFunction( 128 , 128 );
       
       SimplePolynomialFunction concatenated = FunctionUtils.concatenateInputsAndOutputs( lhs , rhs);
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
       SimplePolynomialFunction f = PolynomialFunctions.randomFunction( 256 , 128 );
       String fString = f.toString();
       logger.trace( "f = {}" , fString );
       
       SimplePolynomialFunction fPrime = FunctionUtils.fromString( 256 , fString );
       Assert.assertEquals( f , fPrime );
   }

   @Test
   public void testRandomlyPartitionMVQ() {
       SimplePolynomialFunction f = PolynomialFunctions.denseRandomMultivariateQuadratic( 256 , 256 );
       Pair<SimplePolynomialFunction,SimplePolynomialFunction> gh = PolynomialFunctions.randomlyPartitionMVQ(f);
       SimplePolynomialFunction g = gh.getLeft();
       SimplePolynomialFunction h = gh.getRight();
       
       Assert.assertEquals( f, h.xor(g) );
       
       BitVector input = BitUtils.randomVector( f.getInputLength() );
       BitVector expected = f.apply( input );
       BitVector result = g.apply(input);
       result.xor( h.apply(input) );
       
       Assert.assertEquals( expected , result );
   }
}
