package com.kryptnostic.crypto;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.BasePolynomialFunction;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctions;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.parameterization.ParameterizedPolynomialFunctionGF2;

public class EncryptedSearchPrivateKey {
    private static final HashFunction hf = Hashing.murmur3_128();
    /*
     * Query collapsers are used for collapsing the expanded search tokens submitted to the server (E^+)(T)(X^+) 
     */
    private final EnhancedBitMatrix leftQueryCollapser,rightQueryCollapser;
    /*
     * Index collapsers are used for computing the actual index location of a shared document.
     */
    private final EnhancedBitMatrix leftIndexCollapser,rightIndexCollapser;
    
    /*
     * 
     */
    private final SimplePolynomialFunction indexingFunction;
    
    private final EnhancedBitMatrix queryMixer;
    private final int hashBits;
    private final int nonceBits;
    
    public EncryptedSearchPrivateKey() throws SingularMatrixException {
        //Defaults are 128 bit murmur hash
        this( 128, 64, 64, 8, 16, 8, 16 );
    }
    
    //TODO: Make a builder too many ints...
    public EncryptedSearchPrivateKey( int hashBits, int nonceBits, int indexLength, int collapsedQueryBits, int expandedQueryBits, int collpasedIndexBits, int expandedIndexBits ) throws SingularMatrixException {
        leftQueryCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( collpasedIndexBits , expandedIndexBits , 25 );
        rightQueryCollapser = EnhancedBitMatrix.randomLeftInvertibleMatrix( expandedIndexBits , collpasedIndexBits , 25 );
        
        leftIndexCollapser = EnhancedBitMatrix.randomRightInvertibleMatrix( collpasedIndexBits , expandedIndexBits , 25 );
        rightIndexCollapser = EnhancedBitMatrix.randomLeftInvertibleMatrix( expandedIndexBits, collpasedIndexBits , 25 );
        
        queryMixer = EnhancedBitMatrix.randomInvertibleMatrix( hashBits + nonceBits );
        indexingFunction = PolynomialFunctions.denseRandomMultivariateQuadratic( hashBits , hashBits );
        this.hashBits = hashBits;
        this.nonceBits = nonceBits;
    }
        
    /**
     * Generates a search token by computing 
     * @param token
     * @param publicKey
     * @return
     * @throws SingularMatrixException
     */
    public BitVector prepareSearchToken( String token ) throws SingularMatrixException {
        BitVector searchHash = BitVectors.fromBytes( hashBits , hf.hashString( token , Charsets.UTF_8 ).asBytes() );
        BitVector nonce = BitVectors.randomVector( nonceBits );
        BitVector searchVector = BitVectors.concatenate( searchHash , nonce );
        return queryMixer.multiply( searchVector );
    }

    public EnhancedBitMatrix getLeftQueryCollapser() {
        return leftQueryCollapser;
    }

    public EnhancedBitMatrix getRightQueryCollapser() {
        return rightQueryCollapser;
    }

    public EnhancedBitMatrix getLeftIndexCollapser() {
        return leftIndexCollapser;
    }

    public EnhancedBitMatrix getRightIndexCollapser() {
        return rightIndexCollapser;
    }
    
    public EnhancedBitMatrix getQueryMixer() { 
        return queryMixer;
    }
    
    public EnhancedBitMatrix newDocumentKey() {
        return EnhancedBitMatrix.randomMatrix( indexingFunction.getInputLength() , hashBits );
    }

    public SimplePolynomialFunction getDownmixingIndexer(EnhancedBitMatrix documentKey) {
        EnhancedBitMatrix lhs = leftIndexCollapser.multiply( documentKey );
        SimplePolynomialFunction f = PolynomialFunctions.identity( hashBits );
        return twoSidedMultiply( f , lhs , rightIndexCollapser );
    }
    
    public SimplePolynomialFunction getQueryHasher( SimplePolynomialFunction globalHash, SimplePolynomialFunction decryptor ) throws SingularMatrixException {
        return twoSidedMultiplyWithMixing( globalHash , decryptor, leftQueryCollapser.rightInverse() , rightQueryCollapser.leftInverse() , queryMixer );
    }
    
    public static SimplePolynomialFunction twoSidedMultiplyWithMixing( SimplePolynomialFunction f , SimplePolynomialFunction decryptor, EnhancedBitMatrix lhs , EnhancedBitMatrix rhs , EnhancedBitMatrix queryMixer ) throws SingularMatrixException {
        Preconditions.checkArgument( lhs.cols() == rhs.rows() , "Left hand side columns and right hand side rows must align." );
        
        EnhancedBitMatrix queryUnmixer = queryMixer.inverse();
        EnhancedBitMatrix downMixer = new EnhancedBitMatrix( EnhancedBitMatrix.identity( queryUnmixer.rows() ).getRows().subList( 0 , 128 ) );
        
        BasePolynomialFunction a = (BasePolynomialFunction)f;  
        BasePolynomialFunction b = (BasePolynomialFunction)a.partialComposeRight( downMixer.multiply( queryUnmixer.multiply( PolynomialFunctions.identity( 192 ) ) ) );
        ParameterizedPolynomialFunctionGF2 g =(ParameterizedPolynomialFunctionGF2) b.partialComposeLeft( decryptor );
        
        return twoSidedMultiply( g , lhs, rhs );
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
}
