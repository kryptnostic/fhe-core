package com.kryptnostic.linear;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.FunctionUtils;
import com.kryptnostic.multivariate.util.PolynomialFunctions;

public class EnhancedBitMatrix {
    private static final Random r = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger( EnhancedBitMatrix.class );
    protected final List<BitVector> rows;
    
    protected EnhancedBitMatrix() { 
        rows = Lists.newArrayList();
    }
    
    protected EnhancedBitMatrix( Byte differentiator , List<BitVector> rows ) {
        this.rows = rows;
    }
    
    public EnhancedBitMatrix( int numRows, int numCols ) {
        Preconditions.checkArgument( numRows >= 0 ,  "Number of rows must not be negative");
        Preconditions.checkArgument( numCols >= 0 ,  "Number of columns must not be negative");
        rows = Lists.newArrayListWithExpectedSize( numRows );
        for( int i = 0 ; i < numRows ; ++i ) {
            rows.add( new BitVector( numCols ) ); 
        }
    }
    
    public EnhancedBitMatrix( EnhancedBitMatrix m ) {
        this( m.rows );
    }
    
    public EnhancedBitMatrix( Iterable<BitVector> rows ) {
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
    	if ( rows.size() != 0) {
    		return rows.iterator().next().size();
    	}
        return 0;
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
    
    public List<BitVector> getRows() {
    	return rows;
    }
    //TODO: Add unit test
    public EnhancedBitMatrix add( EnhancedBitMatrix rhs ) {
        Preconditions.checkArgument( rows()==rhs.rows() , "Matrices being added must have the same number of rows.");
        Preconditions.checkArgument( cols()==rhs.cols() , "Matrices being added must have the same number of columns.");
        EnhancedBitMatrix result = new EnhancedBitMatrix ( rows );
        for( int row = 0 ; row < result.rows() ; ++row ) {
            result.rows.get( row ).xor( rhs.rows.get( row ) );
        }
        return result;
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
    
    public EnhancedBitMatrix rightInverse() throws SingularMatrixException { 
        EnhancedBitMatrix workingSet = new EnhancedBitMatrix( this );
        EnhancedBitMatrix inverse = identity( rows.size() );
        
        rowReducedEchelonForm( workingSet , inverse );
        
        isFullRank( workingSet );
        
        int numCols = cols();
        BitVector[] basis = new BitVector[ numCols ];
        List<BitVector> constantBasis = Lists.newArrayList();
        
        //Use row-echelon form to extract nullspace vector
        List<BitVector> wsRows = workingSet.getRows();
        Integer[] firstNonZeroIndex = new Integer[ wsRows.size() ];
        for( int i = 0; i < wsRows.size(); ++i ) {
            BitVector v = wsRows.get( i );
            int value = -1;
            for( int j = 0; j < v.size() ; ++j ) {
                if( v.get( j ) ) {
                    if( value == -1 ) {
                        value = j;
                        //Now lets add the constants for the first column to the appropriate rows of a newly expanded vector
                        firstNonZeroIndex[ i ] = j;
                    } else {
                        if( basis[ j ] == null ) {
                            basis[ j ] = new BitVector( numCols );
                            basis[ j ].set( j );
                        }
                        BitVector b = basis[ j ];
                        b.set( value );
                    }
                }
            }
        }
        
        
        List<BitVector> filtered = Lists.newArrayList();
        for( BitVector b : basis ) {
            if( b!=null ) {
                filtered.add( b );
            }
        }
        
        if( filtered.size() != (numCols - rows() ) ) {
            throw new SingularMatrixException( "Matrix has no generalized right inverse." );
        }
        
        EnhancedBitMatrix inverseColumnar = inverse.transpose();
        
        for( BitVector column : inverseColumnar.getRows() ) {
            constantBasis.add( map( firstNonZeroIndex , column , numCols ) );
        }
        
        EnhancedBitMatrix randomizer = EnhancedBitMatrix.randomMatrix( rows() , filtered.size() );
        EnhancedBitMatrix nullspan = randomizer.multiply( EnhancedBitMatrix.directFromRows( filtered ) ).transpose();
        
        EnhancedBitMatrix cB = new EnhancedBitMatrix( constantBasis ).transpose();
        
        return nullspan.add( cB );

    }
    
    static void isFullRank( EnhancedBitMatrix m ) throws SingularMatrixException {
        for( BitVector row : m.getRows() ) {
            if( row.cardinality() == 0 ) {
                throw new SingularMatrixException( "Matrix has no generalized right inverse." );
            }
        }
    }
    
    static BitVector map( Integer[] firstNonZeroIndex , BitVector constant , int len ) {
        BitVector result = new BitVector( len );
        for( int i = 0 ; i < firstNonZeroIndex.length ; ++i ) {
            if( constant.get( i ) ) {
                result.set( firstNonZeroIndex[ i ] );
            }
        }
        return result;
    }
    
    public EnhancedBitMatrix leftInverse() throws SingularMatrixException { 
        EnhancedBitMatrix workingSet = new EnhancedBitMatrix( this ).transpose();
        EnhancedBitMatrix inverse = identity( workingSet.getRows().size() );
        
        rowReducedEchelonForm( workingSet , inverse );
        
        //Make sure we can actually compute the right generalized inverse
        isFullRank( workingSet );
        
        int numCols = rows();
        BitVector[] basis = new BitVector[ numCols ];
        List<BitVector> constantBasis = Lists.newArrayList();
        
        //Use row-echelon form to extract nullspace vector
        List<BitVector> wsRows = workingSet.getRows();
        Integer[] firstNonZeroIndex = new Integer[ wsRows.size() ];
        for( int i = 0; i < wsRows.size(); ++i ) {
            BitVector v = wsRows.get( i );
            if( (v.cardinality() == 0 ) && (inverse.getRow( i ).cardinality()!=0) ) {
//                throw new SingularMatrixException( "Matrix has no generalized right inverse." );
            }
            int value = -1;
            for( int j = 0; j < v.size() ; ++j ) {
                if( v.get( j ) ) {
                    if( value == -1 ) {
                        value = j;
                        //Now lets add the constants for the first column to the appropriate rows of a newly expanded vector
                        firstNonZeroIndex[ i ] = j;
                    } else {
                        if( basis[ j ] == null ) {
                            basis[ j ] = new BitVector( numCols );
                            basis[ j ].set( j );
                        }
                        BitVector b = basis[ j ];
                        b.set( value );
                    }
                }
            }
        }
        
        
        List<BitVector> filtered = Lists.newArrayList();
        for( BitVector b : basis ) {
            if( b!=null ) {
                filtered.add( b );
            }
        }
        
        if( filtered.size() != (numCols - cols() ) ) {
            throw new SingularMatrixException( "Matrix has no generalized right inverse." );
        }
        
        EnhancedBitMatrix inverseColumnar = inverse.transpose();
        
        for( BitVector column : inverseColumnar.getRows() ) {
            constantBasis.add( map( firstNonZeroIndex , column , numCols ) );
        }
        
        EnhancedBitMatrix randomizer = EnhancedBitMatrix.randomMatrix( cols() , filtered.size() );
        EnhancedBitMatrix nullspan = randomizer.multiply( EnhancedBitMatrix.directFromRows( filtered ) ).transpose();
        
        EnhancedBitMatrix cB = new EnhancedBitMatrix( constantBasis ).transpose();
        
        return nullspan.add( cB ).transpose();
    }
    
    public EnhancedBitMatrix rowReducedEchelonForm() {
        EnhancedBitMatrix current = new EnhancedBitMatrix( rows );
        rowReducedEchelonForm( current );
        return current;
    }
    
    //TODO: Figure out why this routine stopped working 
    public EnhancedBitMatrix getNullspaceBasis() {
        //TODO: Optimize this
        int rows = cols();
        EnhancedBitMatrix rrefT = rowReducedEchelonForm().transpose();
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
    
    public EnhancedBitMatrix nullspace() {
        EnhancedBitMatrix workingSet = this.transpose();
        rowReducedEchelonForm( workingSet );
        int numCols = workingSet.cols();
        BitVector[] basis = new BitVector[ numCols ];
        
        //Use row-echelon form to extract nullspace vector
        List<BitVector> wsRows = workingSet.getRows();
        Integer[] firstNonZeroIndex = new Integer[ wsRows.size() ];
        for( int i = 0; i < wsRows.size(); ++i ) {
            BitVector v = wsRows.get( i );
            int value = -1;
            for( int j = 0; j < v.size() ; ++j ) {
                if( v.get( j ) ) {
                    if( value == -1 ) {
                        value = j;
                        //Now lets add the constants for the first column to the appropriate rows of a newly expanded vector
                        firstNonZeroIndex[ i ] = j;
                    } else {
                        if( basis[ j ] == null ) {
                            basis[ j ] = new BitVector( numCols );
                            basis[ j ].set( j );
                        }
                        BitVector b = basis[ j ];
                        b.set( value );
                    }
                }
            }
        }
        
        List<BitVector> filtered = Lists.newArrayList();
        for( BitVector b : basis ) {
            if( b!=null ) {
                filtered.add( b );
            }
        }

        return new EnhancedBitMatrix( filtered );
    }
    
    public EnhancedBitMatrix getLeftNullifyingMatrix() throws SingularMatrixException {
        EnhancedBitMatrix nmat = nullspace();
        Set<Integer> rowsToKeep = Sets.newHashSet();
        int rowCountToKeep = cols();
        Preconditions.checkState( rowCountToKeep <= nmat.rows() , "Cannot keep more rows than exist."  );
        while( rowsToKeep.size() != rowCountToKeep ) {
            rowsToKeep.add(r.nextInt( rowCountToKeep ) );
        }
        
        
        BitVector[] newRows = new BitVector[ rowCountToKeep ];

        int i = 0;
        for( int rowToKeep : rowsToKeep ){
            newRows[ i++ ] = nmat.rows.get( rowToKeep );
        }
        
        return new EnhancedBitMatrix( Arrays.asList( newRows ) );
    }
    
    public EnhancedBitMatrix transpose() {
        EnhancedBitMatrix transposed = new EnhancedBitMatrix( this ); 
        transpose( transposed );
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
    
    public SimplePolynomialFunction multiply( SimplePolynomialFunction f ) {
        Monomial[] monomials = f.getMonomials();
        BitVector[] contributions = f.getContributions();
        Map<Monomial, BitVector> results = Maps.newHashMap();
        for( int i = 0 ; i < monomials.length ; ++i ) {
            BitVector newContrib = this.multiply( contributions[i] );
            if( newContrib.cardinality() != 0 ) {
                results.put( monomials[i].clone() , newContrib );
            }
        }
        
        Monomial[] newMonomials = new Monomial[ results.size() ];
        BitVector[] newContributions = new BitVector[ results.size() ];
        int index = 0;
        for( Entry<Monomial,BitVector> result : results.entrySet() ) {
            newMonomials[ index ] = result.getKey();   
            newContributions[ index ] = result.getValue();
            ++index;
        }
        
        if( f.isParameterized() ) {
            ParameterizedPolynomialFunctionGF2 ppf = (ParameterizedPolynomialFunctionGF2)f;
            return new ParameterizedPolynomialFunctionGF2( f.getInputLength() , rows() , newMonomials , newContributions , ppf.getPipelines() );
        }
        
        return PolynomialFunctions.fromMonomialContributionMap(
                f.getInputLength() , 
                rows() , 
                FunctionUtils.mapViewFromMonomialsAndContributions(newMonomials, newContributions) );
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
            throw new SingularMatrixException( "Unable to compute the right generalized inverse, since no invertible extension was found." );
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
    
    //TODO: Add unit test
    public boolean isIdentity() {
        for( int i = 0 ; i < rows() ; ++i ) {
            BitVector row = rows.get( i );
            if( !row.get( i ) && (row.cardinality()==1) ) {
                return false;
            }
        }
        return true;
    }
    
    public static EnhancedBitMatrix identityExpander(int  identityRows , int randomRows ) {
        EnhancedBitMatrix m = new EnhancedBitMatrix( randomRows , identityRows );
        for( int i = 0 ; i < identityRows ; ++i ) {
            m.rows.get( i ).set( i );
        }
//        for( int i = identityRows ; i < randomRows ; ++i ) {
//            m.rows.get( i ).xor( BitUtils.randomBitVector( identityRows ) );
//        }
        return m;
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
            newRows.add( new BitVector( rows.size() ) );
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
    
    public static void rowReducedEchelonForm( EnhancedBitMatrix m , int numRows , EnhancedBitMatrix ... augments) {
        List<BitVector> rows = m.rows;
        int row = 0;
        int numCols = m.cols();
        int limit = Math.min( numRows, numCols );
        /*
         * Gaussian elimination w/ optional augmentation.
         * 
         * We seek the first row with currentColumn non-zero
         */
        for( int col = 0; col < numCols ; ++col ) {
            int suitableRow = getSuitableRow( rows, col, row );
            if( suitableRow >= 0 ) {
                //Perform the row swap.
                m.swap( row, suitableRow );
                augmentedSwap( row , suitableRow , augments );
                //Zero out all other rows
                BitVector currentRow = rows.get( row );
                for( int candidateIndex = 0 ; candidateIndex < limit ; ++candidateIndex ) {
                    if( candidateIndex != row ) {
                        BitVector candidateRow = rows.get( candidateIndex );
                        if( candidateRow.get( col ) ) {
                            candidateRow.xor( currentRow );
                            augmentedXor( candidateIndex , row , augments );
                        }
                    }
                }
                ++row;
            } 
        }
    }
    
    static void augmentedXor( int destRow, int srcRow , EnhancedBitMatrix ... augments ) {
        for( int i = 0 ; i < augments.length ; ++i ) {
            augments[i].rows.get( destRow ).xor( augments[i].rows.get( srcRow ) );
        }
    }
    
    static void augmentedSwap( int destRow, int srcRow , EnhancedBitMatrix ... augments ) {
        for( int i = 0 ; i < augments.length ; ++i ) {
            augments[i].swap( destRow, srcRow );
        }
    }
    
    static int getSuitableRow( List<BitVector> rows , final int col , int startRow ) {
        for( int i = startRow ; i < rows.size(); ++i ) {
            if( rows.get( i ).get( col ) ) {
                return i;
            }
        }
        return -1;
    }
    
    public static EnhancedBitMatrix rref( EnhancedBitMatrix m , EnhancedBitMatrix ... augments ) {
        int row = 0, col;
        int numCols = m.cols();
        int numRows = m.rows();
        
        for( col = 0 ; col < numCols; ++col ) {
            for( int i = row ; i < numRows ; ++i) {
                //For each row 
                if( m.get( i , col ) ) {
                    m.swap( row , i );
                    for( int j = 0 ; i < augments.length ; ++j ) {
                        augments[j].swap( row , i );
                    }
                }
            }
        }
        
        return null;
    }
    
    public static EnhancedBitMatrix randomLeftInvertibleMatrix( int rows , int cols , int attempts ) throws SingularMatrixException {
        Preconditions.checkArgument( rows > cols, "A left invertible matrix requires more rows than columns." );
        Preconditions.checkArgument( attempts > 0, "Number of attempts must be greater than zero." );
        
        EnhancedBitMatrix result = EnhancedBitMatrix.randomMatrix( rows , cols );
        for( int i = 0 ; i < 25 ; ++i ) {
            try {
                result.leftInverse();
                return result;
            } catch (SingularMatrixException e) {
                logger.trace("Singular matrix generated... trying another matrix.");
            }
        }
        throw new SingularMatrixException( "Unable to generate random left invertible matrix." );
    }
    
    public static EnhancedBitMatrix randomRightInvertibleMatrix( int rows , int cols, int attempts ) throws SingularMatrixException {
        Preconditions.checkArgument( cols > rows, "A right invertible matrix requires more rows than columns." );
        Preconditions.checkArgument( attempts > 0, "Number of attempts must be greater than zero." );
        
        EnhancedBitMatrix result = EnhancedBitMatrix.randomMatrix( rows , cols );
        for( int i = 0 ; i < 25 ; ++i ) {
            try {
                result.rightInverse();
                return result;
            } catch (SingularMatrixException e) {
                logger.trace("Singular matrix generated... trying another matrix.");
            }
        }
        throw new SingularMatrixException( "Unable to generate random right invertible matrix." );
    }
    
    public static EnhancedBitMatrix squareMatrixfromBitVector( BitVector v ) {
        final int rows = (int) Math.sqrt( v.size() );
        Preconditions.checkArgument( (rows*rows) == v.size() , "BitVector size must be a perfect square" );
        ImmutableList.Builder<BitVector> rowsBuilder = ImmutableList.builder();
        for( int i = 0 ; i < rows; ++i ) {
            rowsBuilder.add( v.partFromTo( i*rows , (i*rows)+rows - 1 ) );
        }
        return new EnhancedBitMatrix( rowsBuilder.build() );
    }
    
    public static EnhancedBitMatrix randomSqrMatrix( int size ) { 
        List<BitVector> rows = Lists.newArrayListWithExpectedSize( size );
        for( int i = 0 ; i < size ; ++i ) {
            rows.add( BitVectors.randomVector( size ) );
        }
        return new EnhancedBitMatrix( rows );
    }
    
    public static EnhancedBitMatrix randomMatrix( int numRows , int numCols ) {
        List<BitVector> rows = Lists.newArrayListWithExpectedSize( numRows );
        for( int i = 0 ; i < numRows ; ++i ) {
            BitVector v = BitVectors.randomVector( numCols );
            while( v.cardinality() == 0 ) {
                v = BitVectors.randomVector( numCols );
            }
            rows.add( v );
        }
        return new EnhancedBitMatrix( rows );
    }
    
    public static EnhancedBitMatrix randomInvertibleMatrix( int rows ) {
        EnhancedBitMatrix result = null;
        
        try {
            while( result == null || !determinant( result ) ) {
                result = randomMatrix( rows, rows );
            }
        } catch (NonSquareMatrixException e) {
            logger.error("This shouldn't be possible." , e );
        }
        
        return result;
    }

    /**
     * Efficient, but unsafe method for directly allocating a bit matrix from a list of rows
     * @param rows the bitvectors being appropriated for the matrix
     * @return an {@code EnhancedBitMatrix} whos rows are {@code rows} 
     */
    public static EnhancedBitMatrix directFromRows( List<BitVector> rows ) {
        return new EnhancedBitMatrix(null, rows );
        
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

    public BitVector getRow(int i) {
        return rows.get( i ).copy();
    }
    
    public void addRow( BitVector row ) {
        int numCols = cols();
        Preconditions.checkArgument( (numCols==0) || (row.size() == numCols ) , "New row must have the same number of columns as matrix.");
        rows.add( row );
    }
    
}
