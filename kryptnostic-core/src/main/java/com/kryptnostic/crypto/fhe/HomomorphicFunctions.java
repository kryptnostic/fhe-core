package com.kryptnostic.crypto.fhe;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;
import com.kryptnostic.multivariate.util.PolynomialFunctions;

public class HomomorphicFunctions {
    private static final Logger logger = LoggerFactory.getLogger( HomomorphicFunctions.class );
    
	private HomomorphicFunctions(){}
    
    public static SimplePolynomialFunction HomomorphicXor( int length , PrivateKey privateKey ) {
        return privateKey.computeHomomorphicFunction( PolynomialFunctions.XOR( length ) );
    }
    
    public static SimplePolynomialFunction HomomorphicAnd( int length , PrivateKey privateKey ) {
        return privateKey.computeHomomorphicFunction( PolynomialFunctions.AND( length ) );
    }
    
    public static SimplePolynomialFunction HomomorphicLsh( int length, PrivateKey privatekey ) {
        return privatekey.computeHomomorphicFunction( PolynomialFunctions.LSH( length , 1 ) );
    }
    
    public static SimplePolynomialFunction BinaryHomomorphicXor( int length , PrivateKey privateKey ) {
        return privateKey.computeBinaryHomomorphicFunction( PolynomialFunctions.BINARY_XOR( length ) );
    }
    
    public static SimplePolynomialFunction BinaryHomomorphicAnd( int length , PrivateKey privateKey ) {
        return privateKey.computeBinaryHomomorphicFunction( PolynomialFunctions.BINARY_AND( length ) );
    }

    public static SimplePolynomialFunction BinaryHomomorphicCarry( int length, PrivateKey privatekey ) {
        return privatekey.computeBinaryHomomorphicFunction( 
                PolynomialFunctions
                .LSH( length , 1 )
                .compose( PolynomialFunctions.BINARY_AND( length ) ) );
    }
    
    public static SimplePolynomialFunction HomomorphicHalfAdder( int length , PrivateKey privateKey ) {
        SimplePolynomialFunction xor = privateKey.computeBinaryHomomorphicFunction( PolynomialFunctions.BINARY_XOR( length ) );
        logger.info("Generated XOR portion of half adder.");
        SimplePolynomialFunction and = privateKey.computeBinaryHomomorphicFunction( PolynomialFunctions.BINARY_AND( length ) );
        logger.info("Generated AND portion of half adder.");
        SimplePolynomialFunction carry = 
                privateKey.computeBinaryHomomorphicFunction( 
                        PolynomialFunctions
                            .LSH( length , 1 )
                            .compose( PolynomialFunctions.BINARY_AND( length ) ) );
//        return privateKey.computeBinaryHomomorphicFunction( PolynomialFunctions.HALF_ADDER( 64 ) ) ; 
        logger.debug( "Generated carry portion of half adder" );
        return PolynomialFunctions.concatenate( xor , carry ); 
    }
    
    //TODO: Fix this to support parameterized functions
    public static SimplePolynomialFunction DirectHomomorphicAnd( PrivateKey privateKey ) {
        /*
         * Doing a direct homomorphic and with parameterized functions requires making sure the pipelines are preserved in the shifting process.
         * 
         */
        ParameterizedPolynomialFunctionGF2 decryptor = (ParameterizedPolynomialFunctionGF2) privateKey.getDecryptor();
        Monomial [] monomials = decryptor.getMonomials();
        Monomial [] lhsMonomials = new Monomial[ monomials.length ];
        Monomial [] rhsMonomials = new Monomial[ monomials.length ];
        int input = decryptor.getInputLength();
        int pipeline = input+decryptor.getPipelineOutputLength();
        
        int lhsInput = input;
        int rhsInput = lhsInput << 1;
        int lhsPipeline = rhsInput + decryptor.getPipelineOutputLength();
        int rhsPipeline = lhsPipeline + decryptor.getPipelineOutputLength();
        
        /*
         *  [         decryptor inputLength        ]  ===> [                    newSize                     ]
         *  [ input monomials | pipeline monomials ]  ===> [ lhsInput| rhsInput | lhsPipeline | rhsPipeline ] 
         */
        int [] srcRanges = new int[] { 0 , input , input, pipeline } ;
        int [][] lhsDstRanges = new int[][] {{ 0 , lhsInput } , { rhsInput, lhsPipeline } };
        int [][] rhsDstRanges = new int[][] {{ lhsInput , rhsInput } , { lhsPipeline , rhsPipeline } };
        
        List<CompoundPolynomialFunction> newLhsPipeline = Lists.newArrayListWithCapacity( decryptor.getPipelines().size() );
        List<CompoundPolynomialFunction> newRhsPipeline = Lists.newArrayListWithCapacity( decryptor.getPipelines().size() );
        
        for( CompoundPolynomialFunction f : decryptor.getPipelines() ) {
            newLhsPipeline.add( ParameterizedPolynomialFunctions.extendAndMap( rhsInput , new int[] { 0 , input }  ,  new int[][] {{ 0 , lhsInput }} , f ) );
            newRhsPipeline.add( ParameterizedPolynomialFunctions.extendAndMap( rhsInput , new int[] { 0 , input } , new int[][] {{ lhsInput , rhsInput }} , f ) );
        }
        
        for( int i = 0 ; i < monomials.length ; ++i ) {
            Monomial m = monomials[ i ];
            
            Monomial mLHS = m.extendAndMapRanges( rhsPipeline , srcRanges , lhsDstRanges );
            Monomial mRHS = m.extendAndMapRanges( rhsPipeline , srcRanges , rhsDstRanges );
            lhsMonomials[ i ] = mLHS;
            rhsMonomials[ i ] = mRHS;
        }
        
        
        SimplePolynomialFunction X = new OptimizedPolynomialFunctionGF2( rhsInput , decryptor.getOutputLength() , lhsMonomials , decryptor.getContributions() );
        SimplePolynomialFunction Y = new OptimizedPolynomialFunctionGF2( rhsInput , decryptor.getOutputLength() , rhsMonomials , decryptor.getContributions() );
        logger.info("Generated functions for producting.");
        SimplePolynomialFunction XY = X.and( Y );
        logger.info("Computed product of decryption functons");
        
        
        
        return privateKey.encryptBinary( new ParameterizedPolynomialFunctionGF2( rhsInput , decryptor.getOutputLength() , XY.getMonomials() , XY.getContributions() , Iterables.concat( newLhsPipeline , newRhsPipeline ) ) );
    }
    
    public static PolynomialFunction EfficientAnd( PrivateKey privateKey ) throws SingularMatrixException {
        EnhancedBitMatrix L  = privateKey.randomizedL(),
                          E1 = privateKey.getE1(),
                          E2 = privateKey.getE2(),
                          D  = privateKey.getD();
        int plaintextLength = E1.cols(),
            ciphertextLength = E1.rows();
        
        SimplePolynomialFunction X = PolynomialFunctions.lowerBinaryIdentity( plaintextLength );
        SimplePolynomialFunction Y = PolynomialFunctions.upperBinaryIdentity( plaintextLength );
        SimplePolynomialFunction DX = D.multiply( X );
        SimplePolynomialFunction DY = D.multiply( Y );
        SimplePolynomialFunction DXplusY = D.multiply( X.xor( Y ) );
        
        EnhancedBitMatrix R = EnhancedBitMatrix.randomInvertibleMatrix( E1.rows() );
        
        SimplePolynomialFunction R1 = PolynomialFunctions.randomFunction( ciphertextLength >> 1 ,  ciphertextLength >> 1 ) ,
                                 R2 = PolynomialFunctions.randomFunction( ciphertextLength >> 1 ,  ciphertextLength >> 1 );
        
        SimplePolynomialFunction V1 = 
                E1
                    .multiply( L.multiply( X ).xor( R1 ) )
                    .xor( E2.multiply( DXplusY.xor( R2 ) ) );
        
        SimplePolynomialFunction V2 = 
                E1
                    .multiply( L.multiply( Y ).xor( R2 ) )
                    .xor( E2.multiply( DXplusY.xor( R2 ) ) );        
        
        SimplePolynomialFunction V3 = 
                E1
                    .multiply( R.multiply(  L.multiply( X ).xor( R1 ) ) )
                    .xor( E2.multiply( DXplusY.xor( R2 ) ) );
        
        SimplePolynomialFunction V4 = 
                E1
                    .multiply( R.multiply( L.multiply( X ).xor( R1 ) ) )
                    .xor( E2.multiply( DXplusY.xor( R2 ) ) );
        
        SimplePolynomialFunction Lx = L.multiply( X );
        SimplePolynomialFunction Ly = L.multiply( Y );
        
//        SimplePolynomialFunction PLL =
//                E1
//                    .multiply( Lx.and( Ly ).xor( privateKey.getF().compose( DXplusY ) ) ) 
//                    .xor( E2.multiply( DX ) );
        
        return null;
    }
}
