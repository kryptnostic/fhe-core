package com.kryptnostic.crypto;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.NonSquareMatrixException;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;

public class SearchEncryptionKeyExample {

    @Test
    public void doStuff() throws NonSquareMatrixException, SingularMatrixException {
        int N = 64;
        EnhancedBitMatrix clientSpecificHash  = EnhancedBitMatrix.randomInvertibleMatrix( N );
        List<BitVector> Trows = Lists.newArrayList( EnhancedBitMatrix.identity( N ).getRows() );
        
        for( int i = 0 ; i <  N ; ++i ) {
            Trows.add( new BitVector( 64 ) );
        }
        
        List<BitVector> Rrows = Lists.newArrayList();
        
        for( int i = 0 ; i <  N ; ++i ) {
            Rrows.add( new BitVector( 64 ) );
        }
        
        EnhancedBitMatrix Rbottom = EnhancedBitMatrix.randomInvertibleMatrix( N );
        Rrows.addAll( Rbottom.getRows() );
        
        List<BitVector> RPrimerows = Lists.newArrayList();
        
        for( int i = 0 ; i <  N ; ++i ) {
            RPrimerows.add( new BitVector( 64 ) );
        }
        
        EnhancedBitMatrix RPrimeBottom = EnhancedBitMatrix.randomInvertibleMatrix( N );
        RPrimerows.addAll( RPrimeBottom.getRows() );
        List<BitVector> concatenatedRows = Lists.newArrayListWithExpectedSize( 2*N );
        List<BitVector> concatenatedPrimeRows = Lists.newArrayListWithExpectedSize( 2*N );
        
        for( int i = 0 ; i < Trows.size() ; ++i ) {
            concatenatedRows.add( BitVectors.concatenate( Trows.get(i), Rrows.get( i ) ) );
            concatenatedPrimeRows.add( BitVectors.concatenate( Trows.get(i), RPrimerows.get( i ) ) );
        }
        
        EnhancedBitMatrix TR = EnhancedBitMatrix.directFromRows( concatenatedRows );
        EnhancedBitMatrix TRprime = EnhancedBitMatrix.directFromRows( concatenatedPrimeRows );
        
        List<BitVector> hashRows = Lists.newArrayList();
        
        for( BitVector v : EnhancedBitMatrix.identity( N ).getRows() ) {
            hashRows.add(  BitVectors.concatenate( v , v ) );
        }
        
        EnhancedBitMatrix hash = new EnhancedBitMatrix( hashRows );
        
        EnhancedBitMatrix hashTR = hash.multiply( TR ); 
        EnhancedBitMatrix hashTRprime = hash.multiply( TRprime );
        EnhancedBitMatrix d_iAdapter = Rbottom.inverse().multiply( RPrimeBottom );
        
        BitVector tau = BitVectors.randomVector( N );
        BitVector d_i = BitVectors.randomVector( N );
        BitVector indexInput = BitVectors.concatenate( tau, d_i );
        BitVector adaptedIndexInput = BitVectors.concatenate( tau, d_iAdapter.multiply( d_i ) );
        
        Assert.assertEquals( RPrimeBottom.multiply( d_i ), Rbottom.multiply( d_iAdapter ).multiply( d_i ) );
        Assert.assertEquals(  hashTR.multiply( adaptedIndexInput ), hashTRprime.multiply( indexInput ) );
    }
}
