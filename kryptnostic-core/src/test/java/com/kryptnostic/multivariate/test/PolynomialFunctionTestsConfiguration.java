package com.kryptnostic.multivariate.test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import cern.colt.bitvector.BitVector;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.linear.BitUtils;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.FunctionUtils;
import com.kryptnostic.multivariate.Monomials;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctionPipelineStage;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

@Configuration
public class PolynomialFunctionTestsConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PolynomialFunctionTestsConfiguration.class);
    private static final Random r = new Random(0);
    private static final int INPUT_LENGTH = 128;
    private static final int OUTPUT_LENGTH = 128;
    
    @Timed
    public void builderTest() {
        OptimizedPolynomialFunctionGF2.Builder builder = OptimizedPolynomialFunctionGF2.builder(256, 256);
        for (int i = 0; i < 1024; ++i) {
            BitVector contribution = BitUtils.randomVector(256);
            builder.setMonomialContribution(Monomial.randomMonomial(256, 4), contribution);
        }

        OptimizedPolynomialFunctionGF2 f = builder.build();
        BitVector result = f.apply(BitUtils.randomVector(256));
        logger.trace("Result: {}", result);
        Assert.assertEquals(result.size(), 256);
    }

    @Timed
    public void denseRandomMVQTest() {
        SimplePolynomialFunction f = PolynomialFunctions.denseRandomMultivariateQuadratic(256, 256);
        Assert.assertEquals(256, f.getInputLength());
        Assert.assertEquals(256, f.getOutputLength());
        Assert.assertEquals(1 + 128 * 257, f.getMonomials().length);

        for (Monomial m : f.getMonomials()) {
            Assert.assertTrue(m.cardinality() <= 2);
        }

        BitVector input = BitUtils.randomVector(f.getInputLength());

        BitVector result = f.apply(input);
        Assert.assertNotNull(result);
        Assert.assertEquals(f.getOutputLength(), result.size());
    }

    @Timed
    public void evaluationTest() {
    	int nTrials = 5000;
    	long time = 0;
    	OptimizedPolynomialFunctionGF2.Builder builder = OptimizedPolynomialFunctionGF2.builder( 256 , 256 );
        for( int i = 0 ; i < 1024 ; ++i ) {
            BitVector contribution = BitUtils.randomVector( 256 );
            builder.setMonomialContribution( Monomial.randomMonomial( 256 , 4 ) , contribution);

        }

        OptimizedPolynomialFunctionGF2 f = builder.build();
        for (int j = 0; j < nTrials; j++) {
	        BitVector input = BitUtils.randomVector( 256 );
	        long start = System.nanoTime();
	        BitVector result = f.apply( input );
	        long end = System.nanoTime();
	        logger.trace( "Result: {}" , result );
	        Assert.assertEquals( result.size() ,  256 );
	        
	        time += TimeUnit.MILLISECONDS.convert((end - start), TimeUnit.NANOSECONDS);
        }
    	logger.info("Function evaluation test took an average: {} ms.",((double)time) / ((double)nTrials));
    }
   
    @Timed
    public void denseEvaluationTest() {
    	SimplePolynomialFunction f = PolynomialFunctions.denseRandomMultivariateQuadratic( 256 , 256 );
        BitVector result = f.apply( BitUtils.randomVector( 256 ) );
        logger.trace( "Result: {}" , result );
        Assert.assertEquals( result.size() ,  256 );
    }
    
   @Timed
   public void identityTest() {
		   SimplePolynomialFunction f = identity();
	       BitVector input = BitUtils.randomVector( 256 );
	       
	       Assert.assertEquals( input , f.apply( input ) );
       
   }
   
   @Timed
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
       Set<Monomial> actual = OptimizedPolynomialFunctionGF2.product( rowA , rowB );
       Assert.assertEquals( expected , actual );
   }
   
   @Timed 
   public void addTest() {
       SimplePolynomialFunction lhs = PolynomialFunctions.randomFunction(256, 256);
       SimplePolynomialFunction rhs = PolynomialFunctions.randomFunction(256, 256);
       BitVector val = BitUtils.randomVector( 256 ) ;
       BitVector expected = lhs.apply( val );
       expected.xor( rhs.apply( val ) );
       Assert.assertEquals( expected, lhs.xor( rhs ).apply( val ) );
   }
   
   @Timed
   public void productTest() {
       SimplePolynomialFunction lhs = PolynomialFunctions.randomFunction(256, 256, 10, 2);
       SimplePolynomialFunction rhs = PolynomialFunctions.randomFunction(256, 256, 10, 2);
       BitVector val = BitUtils.randomVector( 256 ) ;
       BitVector expected = lhs.apply( val );
       expected.and( rhs.apply( val ) );
       Assert.assertEquals( expected, lhs.and( rhs ).apply( val ) );
   }
   
   @Timed 
   public void testRowProduct() {
       SimplePolynomialFunction f = PolynomialFunctions.randomManyToOneLinearCombination( 256 );
       Monomial[] monomials = f.getMonomials();
       List<BitVector> contribs = Lists.newArrayList( f.getContributions() );
       ConcurrentMap<Monomial,Integer> indices = Maps.newConcurrentMap();
       
       for( int i = 0; i < monomials.length ; ++i ) {
           indices.put( monomials[ i ] , i );
       }
       
       EnhancedBitMatrix.transpose( contribs , f.getContributions().length );
       
       BitVector lhs = contribs.get( 110 );
       BitVector rhs = contribs.get( 100 );
       List<Monomial> mList = Lists.newArrayList( monomials );
       OptimizedPolynomialFunctionGF2 function = new OptimizedPolynomialFunctionGF2(0, 0, null, null);
       BitVector p = function.product( lhs , rhs , mList , indices );
       
       for( int i = 0 ; i < lhs.size() ; ++i ) {
           if( lhs.get( i ) ) {
               for( int j = 0 ; j < rhs.size(); j++ ) {
                   if( rhs.get( j ) ) {
                       Assert.assertNotNull( indices.get( mList.get( i ).product( mList.get( j ) ) ) );
                   }
                   
                   if( i!=j && lhs.get( i ) && rhs.get( j ) && lhs.get( j ) && rhs.get( i ) ) {
                       Assert.assertFalse( p.get( indices.get( mList.get( i ).product( mList.get( j ) ) ) ) );
                   }
               }
           }
       }
       
   }
   
   @Timed 
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
       
       
       Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap( 256 , monomials , contributions );
       Map<Monomial, List<Monomial>> possibleProducts = OptimizedPolynomialFunctionGF2.allPossibleProduct( memoizedComputations.keySet() );  // 1
       Monomial mostFrequent = OptimizedPolynomialFunctionGF2.mostFrequentFactor( monomials , possibleProducts.keySet() , ImmutableSet.<Monomial>of() );
       logger.trace( "Most frequent monomial found: {}" , mostFrequent );
       Assert.assertEquals( new Monomial( 256 ).chainSet( 0 ).chainSet( 1 ) , mostFrequent );
   }
   
   //@Timed 
   public void benchmarkAllPossibleProduct() {
       final int ROUNDS = 2;
       final int INPUT_LENGTH = 256;
       final int OUTPUT_LENGTH = 256;
       long totalNanos = 0,
            start = 0,
            stop = 0;
       double mu=0D, sigma=0D;
       long[] timings = new long[ ROUNDS ];
       
       for( int i = 0 ; i < timings.length ; ++i ) {
           SimplePolynomialFunction mvq = PolynomialFunctions.denseRandomMultivariateQuadratic( INPUT_LENGTH, OUTPUT_LENGTH );
           Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap( INPUT_LENGTH , mvq.getMonomials() , mvq.getContributions() );
           start = System.nanoTime();
           Map<Monomial, List<Monomial>> possibleProducts = OptimizedPolynomialFunctionGF2.allPossibleProduct( memoizedComputations.keySet() ); 
           stop = System.nanoTime();
           timings[ i ] = stop - start;
           totalNanos += timings[ i ];
       }
       
       mu = Double.valueOf( TimeUnit.MILLISECONDS.convert( totalNanos , TimeUnit.NANOSECONDS ) ) / ROUNDS;
       for( long timing : timings ) {
           double d = TimeUnit.MILLISECONDS.convert( timing , TimeUnit.NANOSECONDS )  - mu;
           sigma += d*d;
       }
       sigma /= (ROUNDS-1);
       sigma = Math.sqrt( sigma );
       
       logger.info( "Sequential mu = {} ms, sigma = {} ms, bits = {}" , mu , sigma , INPUT_LENGTH );
       totalNanos = 0;
       for( int i = 0 ; i < timings.length ; ++i ) {
           SimplePolynomialFunction mvq = PolynomialFunctions.denseRandomMultivariateQuadratic( INPUT_LENGTH, OUTPUT_LENGTH );
           Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap( INPUT_LENGTH , mvq.getMonomials() , mvq.getContributions() );
           start = System.nanoTime();
//           Map<Monomial, List<Monomial>> possibleProducts = PolynomialFunctionGF2.allPossibleProductParallelEx2( memoizedComputations.keySet() , memoizedComputations.keySet() ,Monomials.deepCloneToImmutableSet( mvq.getMonomials() ) , 2); 
           stop = System.nanoTime();
           timings[ i ] = stop - start;
           totalNanos += timings[ i ];
       }
       
       mu = Double.valueOf( TimeUnit.MILLISECONDS.convert( totalNanos , TimeUnit.NANOSECONDS ) ) / ROUNDS;
       for( long timing : timings ) {
           double d = TimeUnit.MILLISECONDS.convert( timing , TimeUnit.NANOSECONDS )  - mu;
           sigma += d*d;
       }
       sigma /= (ROUNDS-1);
       sigma = Math.sqrt( sigma );
       
       logger.info( "Parallel mu = {} ms, sigma = {} ms, bits = {}" , mu , sigma , INPUT_LENGTH );
       
   }
   
   //@Timed 
   public void benchmarkMostFrequentFactor() {
       final int ROUNDS = 100;
       final int INPUT_LENGTH = 128;
       final int OUTPUT_LENGTH = 128;
       long totalNanos = 0,
            start = 0,
            stop = 0;
       double mu=0D, sigma=0D;
       long[] timings = new long[ ROUNDS ];
       
       for( int i = 0 ; i < timings.length ; ++i ) {
           SimplePolynomialFunction mvq = PolynomialFunctions.denseRandomMultivariateQuadratic( INPUT_LENGTH, OUTPUT_LENGTH );
           Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap( INPUT_LENGTH , mvq.getMonomials() , mvq.getContributions() );
           Map<Monomial, List<Monomial>> possibleProducts = OptimizedPolynomialFunctionGF2.allPossibleProduct( memoizedComputations.keySet() ); 
           start = System.nanoTime();
           Monomial mostFreq = OptimizedPolynomialFunctionGF2.mostFrequentFactor( mvq.getMonomials() , possibleProducts.keySet() , memoizedComputations.keySet() );
           stop = System.nanoTime();
           timings[ i ] = stop - start;
           totalNanos += timings[ i ];
       }
       
       mu = Double.valueOf( TimeUnit.MILLISECONDS.convert( totalNanos , TimeUnit.NANOSECONDS ) ) / ROUNDS;
       for( long timing : timings ) {
           double d = TimeUnit.MILLISECONDS.convert( timing , TimeUnit.NANOSECONDS )  - mu;
           sigma += d*d;
       }
       sigma /= (ROUNDS-1);
       sigma = Math.sqrt( sigma );
       
       logger.info( "Sequential mu = {} ms, sigma = {} ms, bits = {}" , mu , sigma , INPUT_LENGTH );
       
       totalNanos = 0;
       for( int i = 0 ; i < timings.length ; ++i ) {
           SimplePolynomialFunction mvq = PolynomialFunctions.denseRandomMultivariateQuadratic( INPUT_LENGTH, OUTPUT_LENGTH );
           Set<Monomial> remaining = Monomials.deepCloneToImmutableSet( mvq.getMonomials() );
           Map<Monomial, Set<Monomial>> memoizedComputations = OptimizedPolynomialFunctionGF2.initializeMemoMap( INPUT_LENGTH , mvq.getMonomials() , mvq.getContributions() );
//           Map<Monomial, List<Monomial>> possibleProducts = PolynomialFunctionGF2.allPossibleProductParallelEx2( memoizedComputations.keySet() , memoizedComputations.keySet() , remaining , 2); 
           start = System.nanoTime();
//           Monomial mostFreq = PolynomialFunctionGF2.mostFrequentFactorParallel( possibleProducts.keySet() , remaining );
           stop = System.nanoTime();
           timings[ i ] = stop - start;
           totalNanos += timings[ i ];
       }
       
       mu = Double.valueOf( TimeUnit.MILLISECONDS.convert( totalNanos , TimeUnit.NANOSECONDS ) ) / ROUNDS;
       for( long timing : timings ) {
           double d = TimeUnit.MILLISECONDS.convert( timing , TimeUnit.NANOSECONDS )  - mu;
           sigma += d*d;
       }
       sigma /= (ROUNDS-1);
       sigma = Math.sqrt( sigma );
       
       logger.info( "Parallel mu = {} ms, sigma = {} ms, bits = {}" , mu , sigma , INPUT_LENGTH );
       
   }
   
   @Timed
   public void composeTest() {
	   final int BIT_SIZE = 128;
//       SimplePolynomialFunction outer = PolynomialFunctions.randomFunction(BIT_SIZE, BIT_SIZE, 10, 3);
//       SimplePolynomialFunction inner = PolynomialFunctions.randomFunction(BIT_SIZE, BIT_SIZE, 10, 2);
	   SimplePolynomialFunction outer = PolynomialFunctions.denseRandomMultivariateQuadratic( BIT_SIZE, BIT_SIZE );
	   SimplePolynomialFunction inner = PolynomialFunctions.randomManyToOneLinearCombination( BIT_SIZE );
       long start = System.nanoTime();
       SimplePolynomialFunction composed = outer.compose( inner );
       long millis = TimeUnit.MILLISECONDS.convert( System.nanoTime() - start , TimeUnit.NANOSECONDS ); 
       logger.info( "Compose took {} ms." , millis );
       for( int i = 0 ; i < 25 ; ++i ) {
           BitVector randomInput = BitUtils.randomVector( BIT_SIZE << 1);
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
   
   @Timed
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
   
   @Timed
   public void testConcatenateOutputs() {
       int inputLength = 256;
       int outputLength = 128;
       SimplePolynomialFunction lhs = PolynomialFunctions.denseRandomMultivariateQuadratic( inputLength, outputLength );
       SimplePolynomialFunction rhs = PolynomialFunctions.denseRandomMultivariateQuadratic( inputLength, outputLength );
       
       SimplePolynomialFunction concatenated = PolynomialFunctions.concatenate( lhs , rhs);
       
       BitVector input = BitUtils.randomVector( inputLength );
       
       BitVector concatenatedResult = concatenated.apply( input );
       BitVector lhsResult = lhs.apply( input );
       BitVector rhsResult = rhs.apply( input );
       BitVector expected = FunctionUtils.concatenate( lhsResult , rhsResult );
       
       Assert.assertEquals( expected , concatenatedResult ); 
   }
   
   @Timed
   public void testToFromString() {
       SimplePolynomialFunction f = PolynomialFunctions.randomFunction( 256 , 128 );
       String fString = f.toString();
       logger.trace( "f = {}" , fString );
       
       SimplePolynomialFunction fPrime = FunctionUtils.fromString( 256 , fString );
       Assert.assertEquals( f , fPrime );
   }

   @Timed
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
   
   @Timed 
   public void testPipelineStage() {
       final int inputLength = 128;
       final int outputLength = 128;
       
       long start = System.currentTimeMillis();
       SimplePolynomialFunction f = PolynomialFunctions.denseRandomMultivariateQuadratic( inputLength , outputLength );
       SimplePolynomialFunction inner = 
                   PolynomialFunctions
                       .randomManyToOneLinearCombination( inputLength  );
       PolynomialFunctionPipelineStage stage = PolynomialFunctionPipelineStage.build( f , inner );
       long stop = System.currentTimeMillis();
       long millis = stop - start;
       logger.info( "Non-linear pipeline stage generation took {} ms" , millis );

       BitVector input = BitUtils.randomVector( inputLength<<1 );
       BitVector inputLower = stage.getLower().apply( inner.apply( input ) );
       BitVector inputUpper = stage.getUpper().apply( inner.apply( input ) );        
       BitVector expected = f.apply( inner.apply( input ) );
       
       BitVector actual = 
               stage.getC1().multiply( inputLower );
       actual.xor( stage.getC2().multiply( inputUpper ) );
       Assert.assertEquals( expected, actual );
       
       BitVector concatenatedInput = FunctionUtils.concatenate( inputLower, inputUpper );
       
       Assert.assertEquals( inputLower, PolynomialFunctions.lowerIdentity( inputLength << 1 ).apply( concatenatedInput ) );
       Assert.assertEquals( inputUpper, PolynomialFunctions.upperIdentity( inputLength << 1 ).apply( concatenatedInput ) );
       
       BitVector combinationActual = stage.getCombination().apply( concatenatedInput );
       Assert.assertEquals( expected, combinationActual );
        
       BitVector overallActual = stage.getCombination().apply( stage.getStep().apply( input ) );
       Assert.assertEquals( concatenatedInput , stage.getStep().apply( input ) );   
       Assert.assertEquals( expected , overallActual );
   }
   
   @Timed
   public void testCombination() {
       int inputLength = 128;
       BitVector inputLower = BitUtils.randomVector( inputLength );
       BitVector inputUpper = BitUtils.randomVector( inputLength );
       
       EnhancedBitMatrix c1 = EnhancedBitMatrix.randomInvertibleMatrix( inputLength );
       EnhancedBitMatrix c2 = EnhancedBitMatrix.randomInvertibleMatrix( inputLength );
       
       SimplePolynomialFunction combination = PolynomialFunctions.linearCombination( c1 , c2 );
       
       BitVector expected = c1.multiply( inputLower );
       expected.xor( c2.multiply( inputUpper ) );
       
       BitVector concatenatedInput = FunctionUtils.concatenate( inputLower, inputUpper );
       
       Assert.assertEquals( inputLower, PolynomialFunctions.lowerIdentity( inputLength << 1 ).apply( concatenatedInput ) );
       Assert.assertEquals( inputUpper, PolynomialFunctions.upperIdentity( inputLength << 1 ).apply( concatenatedInput ) );
       
       SimplePolynomialFunction f = c1.multiply( PolynomialFunctions.lowerIdentity( inputLength << 1 ) );
       SimplePolynomialFunction g = c2.multiply( PolynomialFunctions.upperIdentity( inputLength << 1 ) );
       
       Assert.assertEquals( c1.multiply( inputLower ) , f.apply( concatenatedInput ) );
       Assert.assertEquals( c2.multiply( inputUpper ) , g.apply( concatenatedInput ) );
       
       SimplePolynomialFunction fg = f.xor( g );
       Assert.assertEquals( expected, fg.apply( concatenatedInput ) ); 
       Assert.assertEquals( expected , combination.apply( concatenatedInput ) );
   }
   
   @Timed
   public void testUnitPipeline() {

       int totalMillis = 0;
       long start = System.currentTimeMillis();

       SimplePolynomialFunction[] functions = 
               PolynomialFunctions
                   .arrayOfRandomMultivariateQuadratics( INPUT_LENGTH , OUTPUT_LENGTH , 1 );

       SimplePolynomialFunction inner =
               PolynomialFunctions
                   .randomManyToOneLinearCombination( INPUT_LENGTH  );

       Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipelineDescription = PolynomialFunctions.buildNonlinearPipeline( inner , functions );
       
       SimplePolynomialFunction fg = pipelineDescription.getRight()[0];
       
       long stop = System.currentTimeMillis();
       long millis = stop - start;
       logger.info( "Non-linear unit pipeline test took {} ms" , millis );
       totalMillis+=millis;

       BitVector input = BitUtils.randomVector( INPUT_LENGTH << 1 );
       BitVector expected = functions[0].apply( inner.apply( input ) );
       BitVector actual = pipelineDescription.getLeft().apply( fg.apply( input ) );
       Assert.assertEquals( expected, actual );
       logger.info( "Non-linear unit pipeline test took a total of {} ms" , totalMillis );
   }
   
   @Timed
   public void testNonlinearPipeline() {
       final int inputLength = 128;
       final int outputLength = 128;
       int totalMillis = 0;
       for(int i = 1; i < 5; ++i) {
           long start = System.currentTimeMillis();
           
           SimplePolynomialFunction[] functions = 
               PolynomialFunctions
                   .arrayOfRandomMultivariateQuadratics( inputLength , outputLength , 1 );

           SimplePolynomialFunction inner =
                   PolynomialFunctions
                       .randomManyToOneLinearCombination( inputLength  );

           Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipelineDescription = PolynomialFunctions.buildNonlinearPipeline( inner , functions );

           CompoundPolynomialFunction originalPipeline = CompoundPolynomialFunctions.fromFunctions( functions );
           CompoundPolynomialFunction newPipeline = CompoundPolynomialFunctions.fromFunctions( pipelineDescription.getRight() );

           long stop = System.currentTimeMillis();
           long millis = stop - start;
           logger.info( "Non-linear pipeline test of length {} took {} ms" , i , millis );
           totalMillis+=millis;
           
           BitVector input = BitUtils.randomVector( inputLength << 1 );
           BitVector expected = originalPipeline.apply( inner.apply( input ) );
           BitVector actual = pipelineDescription.getLeft().apply( newPipeline.apply( input ) );
           Assert.assertEquals( expected, actual );
       }
       logger.info( "Non-linear pipeline test took a total of {} ms" , totalMillis );
   }
   
   @Timed
   public void testTestAssumptions() {
       Assert.assertTrue( randomFunction()!=randomFunction() );
       Assert.assertTrue( identity()!=identity() );
   }

   @Bean
   @Scope( value = ConfigurableBeanFactory.SCOPE_PROTOTYPE )
   public SimplePolynomialFunction randomFunction() {
       return PolynomialFunctions.randomFunction( INPUT_LENGTH , OUTPUT_LENGTH );
   }

   @Bean
   @Scope( value = ConfigurableBeanFactory.SCOPE_PROTOTYPE )
   public SimplePolynomialFunction identity() {
       return PolynomialFunctions.identity( INPUT_LENGTH );
   }
   
   @Bean
   @Scope( value = ConfigurableBeanFactory.SCOPE_PROTOTYPE )
   public SimplePolynomialFunction optimizedIdentity() {
       return PolynomialFunctions.identity( INPUT_LENGTH ).optimize();
   }
}
