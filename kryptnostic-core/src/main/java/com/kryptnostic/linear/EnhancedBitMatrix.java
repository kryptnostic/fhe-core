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
    
    protected EnhancedBitMatrix() { 
        rows = Lists.newArrayList();
    }
    
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
    
    public boolean get( int row , int col ) {
        return rows.get( row ).get( col );
    }
    
    public void set( int row , int col ) {
        rows.get( row ).set( col );
    }
    
    public void clear( int row , int col ) {
        rows.get( row ).clear( col );
    }
    
    public boolean isZero() {
        for( BitVector row : rows ) {
            if( row.cardinality() > 0 ) {
                return false;
            }
        }
        return true;
    }
    
    public EnhancedBitMatrix inverse() throws SingularMatrixException {
        EnhancedBitMatrix workingSet = new EnhancedBitMatrix( this );
        EnhancedBitMatrix inverse = identity( rows.size() ) ;
        
        rowReducedEchelonForm( workingSet , inverse );
        
        if( workingSet.equals( identity( rows.size() ) ) ) {
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
        //TODO: Optimize this
        int rows = cols();
        EnhancedBitMatrix rrefT = rowReducedEchelonForm().tranpose();
        EnhancedBitMatrix identity = EnhancedBitMatrix.identity( rows );
        
        /*
         * In order to find the nullspace basis, we have to perform row reduction on the first rrefT.rows() 
         * of the transpose of the full matrix ( rrefT | I ).
         * 
         * First add all the identity matrix rows to the transposed reduced rowEchelonForm of the current matrix.
         * Next, perform the equivalent of column row reduction until the first #rows are in RREF.
         * 
         * Finally remove any rows where the first #rows cols aren't all zero and return the transpose of that.
         */
        
        rowReducedEchelonForm( rrefT , identity );
        List<BitVector> keep = Lists.newArrayListWithExpectedSize( identity.rows() );
        for( int i = 0 ; i < rrefT.rows() ; ++i ) {
            BitVector row = rrefT.rows.get( i );
            if( row.cardinality() == 0 ) { 
                keep.add( identity.rows.get( i ) );
            }
        }
        
        EnhancedBitMatrix nsBasis = new EnhancedBitMatrix( keep );
        transpose( nsBasis );
        return nsBasis;
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
    
    public EnhancedBitMatrix rightGeneralizedInverse() throws SingularMatrixException {
        int numRows = cols(), numCols = rows();
        EnhancedBitMatrix invertibleExtension = null;
        EnhancedBitMatrix inverse = null; 
        boolean invertible = false;
        int rounds = 1000;
        
        while( !invertible && ( (--rounds) != 0 ) ) {
            invertibleExtension = EnhancedBitMatrix.randomMatrix(numRows, numCols);;
            try {
                inverse = multiply( invertibleExtension ).inverse();
                invertible = true;
            } catch (SingularMatrixException e) {
                continue;
            }
        }
        
        if( !invertible ) {
            throw new SingularMatrixException( "Unable to compute the left generalized inverse, since no invertible extension was found." );
        }
        
        return invertibleExtension.multiply( inverse ); 
    }
    
    public EnhancedBitMatrix leftGeneralizedInverse() throws SingularMatrixException {
        int numRows = cols(), numCols = rows();
        EnhancedBitMatrix invertibleExtension = null;
        EnhancedBitMatrix inverse = null; 
        boolean invertible = false;
        int rounds = 1000;
        
        while( !invertible && ( (--rounds) != 0 ) ) {
            invertibleExtension = EnhancedBitMatrix.randomMatrix(numRows, numCols);;
            try {
                inverse = invertibleExtension.multiply( this ).inverse();
                invertible = true;
            } catch (SingularMatrixException e) {
                continue;
            }
        }
        
        if( !invertible ) {
            throw new SingularMatrixException( "Unable to compute the left generalized inverse, since no invertible extension was found." );
        }
        
        return inverse.multiply( invertibleExtension ); 
    }
    
    public static boolean determinant( EnhancedBitMatrix m ) throws NonSquareMatrixException {
        if( m.cols() != m.rows() ) {
            throw new NonSquareMatrixException("Cannot compute the determinant of non-square matrix with dimensions " + m.rows() +" x " + m.cols() );
        }
        
        EnhancedBitMatrix workingSet = new EnhancedBitMatrix( m );
        rowReducedEchelonForm( workingSet );
        for( int i = 0 ; i < workingSet.rows() ; ++i ) {
            if( !workingSet.get( i , i ) ) {
                return false;
            }
        }
        return true;
    }
    
    public static void transpose( EnhancedBitMatrix m ) {
        transpose( m.rows , m.cols() );
    }
    
    public static void transpose( List<BitVector> rows , int cols ) {
        List<BitVector> newRows = Lists.newArrayListWithCapacity( cols );
        for( int i = 0 ; i < cols ; ++i ) {
            newRows.add( new BitVector( cols ) );
        }
        
        for( int i = 0 ; i < rows.size() ; ++i ) {
            BitVector row = rows.get( i );
            for( int j = 0 ; j < row.size() ; ++j ) {
                if( row.get( j ) ) {
                    newRows.get( j ).set( i );
                }
            }
        }
        rows.clear();
        rows.addAll( newRows );
    }
    
    public static void rowReducedEchelonForm( EnhancedBitMatrix m , EnhancedBitMatrix ... augments ) {
        rowReducedEchelonForm( m, m.rows.size() , augments);
    }
    
    public static void rowReducedEchelonForm( EnhancedBitMatrix m , int lastRow , EnhancedBitMatrix ... augments) {
        List<BitVector> rows = m.rows;
        
        /*
         * Gaussian elimination w/ optional augmentation.
         */

        for( int row = 0 ; row < lastRow && (row < rows.iterator().next().size() ); ++row ) {
            BitVector currentRow = rows.get( row );
            //If the the row #row has the row-th entry set proceed with elimination;
            if( !currentRow.get( row ) ) {
                for( int remainingRow = row + 1 ; remainingRow < lastRow ; ++remainingRow ) {
                    BitVector alternateRow = rows.get( remainingRow ); 
                    if( alternateRow.get( row ) ) {
                        m.swap( row, remainingRow );
                        for( int i = 0 ; i < augments.length ; ++i ) {
                            augments[i].swap( row , remainingRow );
                        }
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
                    for( int i = 0 ; i < augments.length ; ++i ) {
                        augments[i].rows.get( remainingRow ).xor( augments[i].rows.get( row ) );
                    }
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
    
    public static class NonSquareMatrixException extends Exception {
        private static final long serialVersionUID = -174810104575212495L;

        public NonSquareMatrixException( String message ) {
            super( message );
        }
    }
}
