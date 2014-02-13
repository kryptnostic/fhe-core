package com.kryptnostic.crypto.fhe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class HomomorphicFunctions {
    private static final Logger logger = LoggerFactory.getLogger( HomomorphicFunctions.class );
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
        return PolynomialFunctionGF2.concatenate( xor , carry ); 
    }
    
    public static PolynomialFunction EfficientAnd( int length , PrivateKey privateKey ) throws SingularMatrixException {
        int plaintextLength = privateKey.getE1().cols() ,
            ciphertextLength = privateKey.getE2().rows();
        EnhancedBitMatrix L  = privateKey.randomizedL() ,
                          E1 = privateKey.getE1() ,
                          E2 = privateKey.getE2() ,
                          D  = privateKey.getD();
        
        SimplePolynomialFunction X = PolynomialFunctionGF2.identity( ciphertextLength );
        SimplePolynomialFunction DX = D.multiply( X );
        SimplePolynomialFunction R1 = PolynomialFunctionGF2.randomFunction( ciphertextLength << 1 ,  ciphertextLength << 1 ) ,
                                 R2 = PolynomialFunctionGF2.randomFunction( ciphertextLength << 1 ,  ciphertextLength << 1 );
        
        SimplePolynomialFunction V1 = 
                E1
                    .multiply( L.multiply( X ).xor( R1 ) )
                    .xor( E2.multiply( DX.xor( R2 ) ) );
        
        SimplePolynomialFunction V2 = 
                E1
                    .multiply( L.multiply( X ).xor( R1 ) )
                    .xor( E2.multiply( DX.xor( R2 ) ) );        
        
        SimplePolynomialFunction V3 = 
                E1
                    .multiply( L.multiply( X ).xor( R1 ) )
                    .xor( E2.multiply( DX.xor( R2 ) ) );
        
        SimplePolynomialFunction V4 = 
                E1
                    .multiply( L.multiply( X ).xor( R1 ) )
                    .xor( E2.multiply( DX.xor( R2 ) ) );
        
        return null;
    }
}
