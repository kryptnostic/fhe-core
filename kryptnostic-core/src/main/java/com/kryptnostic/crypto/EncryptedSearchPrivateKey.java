package com.kryptnostic.crypto;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.parameterization.ParameterizedPolynomialFunctionGF2;

public class EncryptedSearchPrivateKey {
    private static final HashFunction hf = Hashing.murmur3_128();
    private static final int hashBits = hf.bits();
    
    /*
     * Query collapsers are used for collapsing the expanded search tokens submitted to the server (E^+)(T)(X^+) 
     */
    private final EnhancedBitMatrix leftQueryExpander,rightQueryExpander;
    /*
     * Index collapsers are used for computing the actual index location of a shared document.
     */
    private final EnhancedBitMatrix leftIndexCollapser,rightIndexCollapser;

    private final EnhancedBitMatrix hashCollapser;
    private final SimplePolynomialFunction indexingFunction;
    private final PublicKey publicKey;
    
    public EncryptedSearchPrivateKey( PrivateKey privateKey, PublicKey publicKey ) throws SingularMatrixException {
        int doubleHashBits = hashBits << 1;
        indexingFunction = PolynomialFunctions.denseRandomMultivariateQuadratic( hashBits , hashBits );
        leftQueryExpander = EnhancedBitMatrix.randomLeftInvertibleMatrix( 16 , 8 , 25 );
        rightQueryExpander = EnhancedBitMatrix.randomRightInvertibleMatrix( 8 , 16 , 25 );
        
        leftIndexCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( hashBits , doubleHashBits , 25 );
        rightIndexCollapser = EnhancedBitMatrix.randomLeftInvertibleMatrix( doubleHashBits , hashBits , 25 );
        
        if( publicKey.getEncrypter().getInputLength() == doubleHashBits ) {
            hashCollapser = EnhancedBitMatrix.identity( doubleHashBits );
        } else {
            hashCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( hashBits >>> 1 , hashBits , 25 );
        }
        this.publicKey = publicKey;
    }
        
    /**
     * Generates a search token by computing 
     * @param term
     * @param publicKey
     * @return
     * @throws SingularMatrixException
     */
    public BitVector prepareSearchToken( String term ) throws SingularMatrixException {
        BitVector searchHash = hash( term ); 
        return publicKey.getEncrypter().apply( BitVectors.concatenate( searchHash , BitVectors.randomVector( searchHash.size() ) ) );
    }
    
    public BitVector hash( String term ) {
        return hashCollapser.multiply( BitVectors.fromBytes( hashBits , hf.hashString( term , Charsets.UTF_8 ).asBytes() ) ); 
    }

    public EnhancedBitMatrix getLeftQueryExpander() {
        return leftQueryExpander;
    }

    public EnhancedBitMatrix getRightQueryExpander() {
        return rightQueryExpander;
    }

    public EnhancedBitMatrix getLeftIndexCollapser() {
        return leftIndexCollapser;
    }

    public EnhancedBitMatrix getRightIndexCollapser() {
        return rightIndexCollapser;
    }
    
    public EnhancedBitMatrix newDocumentKey() {
        return EnhancedBitMatrix.randomInvertibleMatrix( hashBits );
    }

    public SimplePolynomialFunction getDownmixingIndexer(EnhancedBitMatrix documentKey) {
        EnhancedBitMatrix lhs = leftIndexCollapser.multiply( documentKey );
        SimplePolynomialFunction f = PolynomialFunctions.identity( hashBits );
        return indexingFunction.compose( twoSidedMultiply( f , lhs , rightIndexCollapser ) );
    }
    
    public SimplePolynomialFunction getQueryHasher( SimplePolynomialFunction globalHash, PrivateKey privateKey ) throws SingularMatrixException {
        return twoSidedMultiply( globalHash.compose( privateKey.getMirroredDecryptor() ) , leftQueryExpander , rightQueryExpander );
    }
    
    public static SimplePolynomialFunction twoSidedMultiply(SimplePolynomialFunction f, EnhancedBitMatrix lhs, EnhancedBitMatrix rhs ) {
        BitVector[] contributions = f.getContributions();
        BitVector[] newContributions = new BitVector[ contributions.length ];
        
        for( int i = 0 ; i < contributions.length ; ++i ) {
            newContributions[ i ] = BitVectors.fromSquareMatrix( lhs.multiply( EnhancedBitMatrix.squareMatrixfromBitVector( contributions[ i ] ) ).multiply( rhs ) );
        }
        
        if( f.getClass().equals(  ParameterizedPolynomialFunctionGF2.class ) ) {
            ParameterizedPolynomialFunctionGF2 g = (ParameterizedPolynomialFunctionGF2) f;
            return new ParameterizedPolynomialFunctionGF2( g.getInputLength(), newContributions[0].size() , g.getMonomials(), newContributions , g.getPipelines() );
        } else {
            return new OptimizedPolynomialFunctionGF2( f.getInputLength() , newContributions[0].size() , f.getMonomials(), newContributions );
        }
    }
    
    public static int getHashBits() { 
        return hashBits;
    }
}
