package com.kryptnostic.linear;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.kryptnostic.multivariate.MultivariateUtils;

import cern.colt.bitvector.BitMatrix;
import cern.colt.bitvector.BitVector;

public class EnhancedBitMatrix {
    protected final List<BitVector> rows;
    
    public EnhancedBitMatrix( int numRows, int numCols ) {
        Preconditions.checkArgument( numRows > 0 ,  "Number of rows must be greater than zero.");
        Preconditions.checkArgument( numCols > 0 ,  "Number of columns must be greater than zero.");
        ImmutableList.Builder<BitVector> builder = ImmutableList.builder();
        for( int i = 0 ; i < numRows ; ++i ) {
            builder.add( new BitVector( numCols ) ); 
        }
        this.rows = builder.build();
    }
    
    public EnhancedBitMatrix( ImmutableList<BitVector> rows ) {
        this.rows = rows;
    }
    
    public EnhancedBitMatrix( List<BitVector> rows ) {
        this.rows = FluentIterable.from( rows )
            .transform( new Function<BitVector,BitVector>() {
                @Override
                public BitVector apply(BitVector row) {
                   return row.copy();
                }
            }).toList();
    }
    
    public int rows() {
        return rows.size();
    }
    
    public int cols() {
        return rows.iterator().next().size();
    }
    
    public EnhancedBitMatrix inverse() throws SingularMatrixException {
        EnhancedBitMatrix inverse = identity( rows.size() );
        EnhancedBitMatrix current = new EnhancedBitMatrix( rows );
        int lastRow = rows.size() - 1;
        for( int row = 0 ; row < lastRow; ++row ) {
            boolean successful = false;
            for( int swapRow = row + 1 ; swapRow < rows.size() ; ++swapRow ) {
                if( rows.get( swapRow ).get( row ) ) {
                    inverse.swap( row, swapRow );
                    current.swap( row, swapRow );
                    successful = true;
                    break;
                }
            }
            /*
             * Zero the the row-th column in all other rows. If no suitable row was found throw an exception.
             */
            if( !successful ) {
                throw new SingularMatrixException("Unable to compute the inverse of a singular matrix.");
            } else { 
                BitVector selectedRow = rows.get( row );
                for( int processRow = row + 1 ; processRow < rows.size() ; ++processRow ) {
                    rows.get( processRow ).xor( selectedRow );
                }   
            }
        }
        BitVector finalRow = rows.get( lastRow );
        if( finalRow.cardinality() == 1 && finalRow.get( lastRow ) ) {
            return inverse;
        } else {
            throw new SingularMatrixException("Unable to compute the inverse of a singular matrix.");
        }
    }
    
    public BitVector multiply( BitVector v ) {
        return null;
    }
    
    public EnhancedBitMatrix multiply( BitMatrix m ) {
        return null;
    }
        
    protected BitVector getFirstNonZeroRowForColumn( int column ) {
        return Iterables.getFirst( Iterables.filter( rows , new ColumnSelector( column ) ) , null );
    }
    
    public static EnhancedBitMatrix identity( int size ) {
        EnhancedBitMatrix identityMatrix = new EnhancedBitMatrix( size , size );
        for( int i = 0 ; i < size ; ++i ) {
            identityMatrix.rows.get( i ).set( i );
        }
        return identityMatrix;
    }
    
    protected void swap( int rowA, int rowB ) {
        BitVector vA = rows.get( rowA );
        rows.set( rowA , rows.get( rowB ) );
        rows.set( rowB , vA );
    }
    
    protected static class ColumnSelector implements Predicate<BitVector> {
        private final int column; 
        public ColumnSelector( int column ) {
            this.column = column;
        }
        
        @Override
        public boolean apply( BitVector input ) {
            return input.get( column );
        }
        
    }
    
    public static class SingularMatrixException extends Exception {
        private static final long serialVersionUID = -1261170128691437282L;

        public SingularMatrixException(String message) {
            super( message );
        }
    }
    
    public static EnhancedBitMatrix randomSqrMatrix( int size ) { 
        ImmutableList.Builder<BitVector> builder = ImmutableList.builder();
        for( int i = 0 ; i < size ; ++i ) {
            builder.add( MultivariateUtils.randomVector( size ) );
        }
        
        return new EnhancedBitMatrix( builder.build() );
    }
}
