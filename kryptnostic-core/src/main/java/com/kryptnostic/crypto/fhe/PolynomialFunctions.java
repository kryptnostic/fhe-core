package com.kryptnostic.crypto.fhe;

import com.kryptnostic.multivariate.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.PolynomialFunctionJoiner;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

import cern.colt.bitvector.BitVector;

public class PolynomialFunctions {
    
    public static SimplePolynomialFunction XOR( int xorLength ) {
        int inputLength = xorLength >>> 1;
        Monomial[] monomials = new Monomial[ xorLength ];
        BitVector[] contributions = new BitVector[ xorLength ];
        
        for( int i = 0 ; i < inputLength ; ++i ) {
            int offsetIndex = i + inputLength;
            monomials[ i ] = Monomial.linearMonomial( xorLength , i);
            monomials[ offsetIndex ] = Monomial.linearMonomial( xorLength , offsetIndex );
            BitVector contribution = new BitVector( xorLength );
            contribution.set( i );
            contributions[ i ] = contribution;
            contributions[ offsetIndex ] = contribution.copy(); //In theory everything else makes a copy so we could cheat here and save memory.
        }
        
        return new PolynomialFunctionGF2( xorLength , xorLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction BINARY_XOR( int xorLength ) {
        int inputLength = xorLength << 1;
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        
        for( int i = 0 ; i < xorLength ; ++i ) {
            int offsetIndex = i + xorLength;
            monomials[ i ] = Monomial.linearMonomial( inputLength , i);
            monomials[ offsetIndex ] = Monomial.linearMonomial( inputLength , offsetIndex );
            BitVector contribution = new BitVector( xorLength );
            contribution.set( i );
            contributions[ i ] = contribution;
            //TODO: In theory everything else makes a copy so we could cheat here and save memory.
            contributions[ offsetIndex ] = contribution.copy(); 
        }
        
        return new PolynomialFunctionGF2( inputLength , xorLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction AND( int andLength ) {
        int inputLength = andLength >>> 1;
        Monomial[] monomials = new Monomial[ inputLength ];
        BitVector[] contributions = new BitVector[ inputLength ];
        
        for( int i = 0 ; i < inputLength ; ++i ) {
            int offsetIndex = i + inputLength;
            monomials[ i ] = Monomial
                                .linearMonomial( andLength , i)
                                .inplaceProd( Monomial.linearMonomial( andLength , offsetIndex ) );
            BitVector contribution = new BitVector( andLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        return new PolynomialFunctionGF2( andLength , andLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction BINARY_AND( int andLength ) {
        int inputLength = andLength << 1;
        Monomial[] monomials = new Monomial[ andLength ];
        BitVector[] contributions = new BitVector[ andLength ];
        
        for( int i = 0 ; i < andLength ; ++i ) {
            int offsetIndex = i + andLength;
            monomials[ i ] = Monomial
                                .linearMonomial( inputLength , i)
                                .inplaceProd( Monomial.linearMonomial( inputLength , offsetIndex ) );
            BitVector contribution = new BitVector( andLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        return new PolynomialFunctionGF2( inputLength , andLength , monomials, contributions );
    }
    
    public static SimplePolynomialFunction LSH( int inputLength , int shiftLength ) {
        Monomial[] monomials = new Monomial[ inputLength - shiftLength ];
        BitVector[] contributions = new BitVector[ inputLength - shiftLength ];
        int upperLimit = inputLength - shiftLength;
        for( int i = 0 ; i < upperLimit ; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( i + shiftLength );
            contributions[ i ] = contribution;
        }
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction NEG( int inputLength ) {
        Monomial[] monomials = new Monomial[ inputLength + 1];
        BitVector[] contributions = new BitVector[ inputLength + 1];
        for( int i = 0 ; i < ( monomials.length - 1) ; ++i ) {
            monomials[ i ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( i );
            contributions[ i ] = contribution;
        }
        
        monomials[ inputLength ] = new Monomial( inputLength );
        contributions[ inputLength ] = new BitVector( inputLength );
        contributions[ inputLength ].not();
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction RSH( int inputLength , int shiftLength ) {
        Monomial[] monomials = new Monomial[ inputLength - shiftLength ];
        BitVector[] contributions = new BitVector[ inputLength - shiftLength ];
        int index;
        for( int i = shiftLength ; i < inputLength ; ++i ) {
            index = i - shiftLength;
            monomials[ index ] = Monomial.linearMonomial( inputLength , i );
            BitVector contribution = new BitVector( inputLength );
            contribution.set( index );
            contributions[ index ] = contribution;
        }
        return new PolynomialFunctionGF2( inputLength, inputLength , monomials , contributions );
    }
    
    public static SimplePolynomialFunction HALF_ADDER( SimplePolynomialFunction xor , SimplePolynomialFunction and , SimplePolynomialFunction lsh ) {
        return xor.xor( lsh.compose( and ) );
    }
    
    public static PolynomialFunction ADDER( int length ) {
        return ADDER( length , XOR( length ) , AND( length ) , LSH( length , 1 ) );
    }
    
    //TODO: Finish adder generation.
    public static PolynomialFunction ADDER( int length , SimplePolynomialFunction xor , SimplePolynomialFunction and , SimplePolynomialFunction lsh ) {
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2();
        
        /*
         * Actually building out the algebraic representation of an adder is prohibitively expensive.
         * Initialization:
         * carry = x&y;
         * current = x + y
         * 
         * next_carry = ( current & carry ) << 1
         * current ^= carry
         * carry = next_carry
         */
        PolynomialFunction currentCarry = CompoundPolynomialFunctionGF2.fromFunctions( and , lsh ); 
        PolynomialFunction applyCarry = new PolynomialFunctionJoiner( xor , xor , currentCarry );
        
        return applyCarry;
    }
}
