package com.kryptnostic.linear;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.MultivariateUtils;

import cern.colt.bitvector.BitVector;

public class EnhancedBitMatrix {
    protected final List<BitVector> rows;
    
    public EnhancedBitMatrix( int numRows, int numCols ) {
        Preconditions.checkArgument( numRows > 0 ,  "Number of rows must be greater than zero.");
        Preconditions.checkArgument( numCols > 0 ,  "Number of columns must be greater than zero.");
        rows = Lists.newArrayListWithExpectedSize( numRows );
        for( int i = 0 ; i < numRows ; ++i ) {
            rows.add( new BitVector( numCols ) ); 
        }
    }
    
    public EnhancedBitMatrix( EnhancedBitMatrix m ) {
        this( m.rows );
    }
    
    public EnhancedBitMatrix( List<BitVector> rows ) {
        this.rows = 
                Lists.newArrayList(
                        Iterables.transform( rows , new Function<BitVector,BitVector>() {
                            @Override
                            public BitVector apply(BitVector row) {
                                return row.copy();
                            }
                        }));
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
        int lastRow = current.rows.size();
        
        /*
         * Gaussian elimination
         */
        
        for( int row = 0 ; row < lastRow; ++row ) {
            boolean successful = false;
            BitVector currentRow = current.rows.get( row );
            //If the the row #row has the row-th entry set proceed with elimination;
            if( !currentRow.get( row ) ) {
                for( int remainingRow = row + 1 ; remainingRow < lastRow ; ++remainingRow ) {
                    BitVector alternateRow = current.rows.get( remainingRow ); 
                    if( alternateRow.get( row ) ) {
                        inverse.swap( row, remainingRow );
                        current.swap( row, remainingRow );
                        currentRow = alternateRow;
                        successful = true;
                        break;
                    }
                }
            } else {
                successful = true;
            }
            
            /*
             * Zero the the row-th column in all other rows. If no suitable row was found throw an exception.
             */
            
            if( successful ) { 
                for( int remainingRow = 0 ; remainingRow < lastRow ; ++remainingRow ) {
                    if( remainingRow == row ) {
                        continue;
                    }
                    BitVector rRow = current.rows.get( remainingRow );
                    if( rRow.get( row ) ) {
                        rRow.xor( currentRow );
                        inverse.rows.get( remainingRow ).xor( inverse.rows.get( row ) );
                    }
                }   
            } else {
                throw new SingularMatrixException("Unable to compute the inverse of a singular matrix.");
            }
        }
        
        BitVector finalRow = current.rows.get( lastRow - 1);
        if( finalRow.cardinality() == 1 && finalRow.get( lastRow - 1 ) ) {
            return inverse;
        } else {
            throw new SingularMatrixException("Unable to compute the inverse of a singular matrix.");
        }
    }
    
    public EnhancedBitMatrix rowReducedEchelonForm() {
        EnhancedBitMatrix current = new EnhancedBitMatrix( rows );
        rowReducedEchelonForm( current );
        return current;
    }
    
    public EnhancedBitMatrix getNullspaceBasis() {
        //TODO: Implement this
        EnhancedBitMatrix rref = rowReducedEchelonForm();
        int limit = Math.min( rref.cols() , rref.rows() );
        for( int i = 0 ; i < rref.rows() ; ++i ) {
//            int BitUtils.getFirstSetBit( rref.rows.get( i ) );
        }
        return null;
        
    }
    
    public EnhancedBitMatrix getLeftNullifyingMatrix() {
        //TODO: Implement this
        return null;
        
    }
    
    public EnhancedBitMatrix tranpose() {
        EnhancedBitMatrix transposed = new EnhancedBitMatrix( this ); 
        transpose( transposed  );
        return transposed;
    }
    
   
    public BitVector multiply( BitVector v ) {
        //TODO: Optimize this a little better.
        BitVector result = new BitVector( rows.size() );
        for( int i = 0 ; i < rows.size() ; ++i ) {
            BitVector row = rows.get( i );
            BitVector prod = row.copy();
            prod.and( v );
            
            long r = 0L;
            for( long l : prod.elements() ) {
                r^=l;
            }
            if( BitUtils.parity( r ) == 1L ) { 
                result.set( i );
            }
//            if( ( prod.cardinality() % 2 ) == 1 ) {
//                result.set( i );
//            }
        }
        
        return result;
    }
    
    public EnhancedBitMatrix multiply( EnhancedBitMatrix m ) {
        List<BitVector> resultRows = Lists.newArrayListWithExpectedSize( rows.size() );
        
        for( int i = 0; i < rows.size() ; ++i ) {
            BitVector lhs = rows.get( i );
            BitVector result = new BitVector( m.cols() );
            for( int j = 0 ; j < lhs.size() ; ++j ) {
                if( lhs.get( j ) ) {
                    result.xor( m.rows.get( j ) );
                }
            }
            resultRows.add( result );
        }
        
        return new EnhancedBitMatrix( resultRows );
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

    @Override
    public String toString() {
        return "EnhancedBitMatrix [rows=" + rows + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rows == null) ? 0 : rows.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof EnhancedBitMatrix))
            return false;
        EnhancedBitMatrix other = (EnhancedBitMatrix) obj;
        if (rows == null) {
            if (other.rows != null)
                return false;
        } else if (!rows.equals(other.rows))
            return false;
        return true;
    }   
    
    public static void transpose( EnhancedBitMatrix m ) {
        List<BitVector> newRows = Lists.newArrayListWithCapacity( m.cols() );
        
        for( int i = 0 ; i < m.cols() ; ++i ) {
            newRows.add( new BitVector( m.rows() ) );
        }
        
        for( int i = 0 ; i < m.rows() ; ++i ) {
            BitVector row = m.rows.get( i );
            for( int j = 0 ; j < row.size() ; ++j ) {
                if( row.get( j ) ) {
                    newRows.get( j ).set( i );
                }
            }
        }
    }
    
    public static void rowReducedEchelonForm( EnhancedBitMatrix m ) {
        List<BitVector> rows = m.rows;
        int lastRow = rows.size();
        
        /*
         * Gaussian elimination
         */
        
        for( int row = 0 ; row < lastRow; ++row ) {
            BitVector currentRow = rows.get( row );
            //If the the row #row has the row-th entry set proceed with elimination;
            if( !currentRow.get( row ) ) {
                for( int remainingRow = row + 1 ; remainingRow < lastRow ; ++remainingRow ) {
                    BitVector alternateRow = rows.get( remainingRow ); 
                    if( alternateRow.get( row ) ) {
                        m.swap( row, remainingRow );
                        currentRow = alternateRow;
                        break;
                    }
                }
            }            
            /*
             * Zero the the row-th column in all other rows. If no suitable row was found throw an exception.
             */
            
                for( int remainingRow = 0 ; remainingRow < lastRow ; ++remainingRow ) {
                    if( remainingRow == row ) {
                        continue;
                    }
                    BitVector rRow = rows.get( remainingRow );
                    if( rRow.get( row ) ) {
                        rRow.xor( currentRow );
                    }
                }   
        }
        
    }
    
    public static EnhancedBitMatrix randomSqrMatrix( int size ) { 
        List<BitVector> rows = Lists.newArrayListWithExpectedSize( size );
        for( int i = 0 ; i < size ; ++i ) {
            rows.add( MultivariateUtils.randomVector( size ) );
        }
        return new EnhancedBitMatrix( rows );
    }
    
    public static EnhancedBitMatrix randomMatrix( int numRows , int numCols ) {
        List<BitVector> rows = Lists.newArrayListWithExpectedSize( numRows );
        for( int i = 0 ; i < numRows ; ++i ) {
            rows.add( MultivariateUtils.randomVector( numCols ) );
        }
        return new EnhancedBitMatrix( rows );
    }
    public static class SingularMatrixException extends Exception {
        private static final long serialVersionUID = -1261170128691437282L;

        public SingularMatrixException(String message) {
            super( message );
        }
    }
    
}
