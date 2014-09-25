package com.kryptnostic.multivariate.composition;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Lists;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.multivariate.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public class RecursiveLinearizingComposerTree {
    //TODO: Fix for non-MVQ( linear ) compose (i.e general) 
    private final int                                  minBucket;
    private final EnhancedBitMatrix                    outerCols;
    private final BitVector                            innerSelector;
    protected final RecursiveLinearizingComposerTree[] buckets;
    protected final int                                  outerInputLength;

    public RecursiveLinearizingComposerTree(final int minBucket, final int outerInputLength,
            final RecursiveLinearizingComposerTree[] buckets) {
        this.minBucket = minBucket;
        this.buckets = buckets;
        this.outerCols = new EnhancedBitMatrix( 0 , 0 );
        this.innerSelector = new BitVector( outerInputLength );
        this.outerInputLength = outerInputLength;
    }

    public List<SimplePolynomialFunction> evaluate(BitVector result, List<Monomial> fixedMonomials, List<Monomial> growingMonomials, 
            List<BitVector> innerRows, ConcurrentMap<Monomial, Integer> indices) {
        List<SimplePolynomialFunction> results = Lists.newArrayList();
        // TODO: Don't go through every bucket if none exist.
        for (int i = 0; i < buckets.length; ++i) {
            RecursiveLinearizingComposerTree b = buckets[i];
            // If the bucket exists, then lets do the product and recursively evaluate
            if (b != null) {
                BitVector nextResult = product( result , innerRows.get( i + minBucket ) , growingMonomials , indices );
                results.addAll( b.evaluate( nextResult , fixedMonomials , growingMonomials, innerRows , indices ) );
            }
        }
        if( innerSelector.cardinality() == 0 ) {
            return results;
        }
        // For every row in the resulting set of monomial contributions product it with the result computed so far.
        EnhancedBitMatrix innerContributionMatrix = outerCols.transpose().multiply( select( innerRows ) );

        EnhancedBitMatrix lhs = null;
        if (result != null) {
            BitVector[] tRows = new BitVector[innerContributionMatrix.cols()];
            Arrays.fill( tRows , result );
            lhs = new EnhancedBitMatrix( Arrays.asList( tRows ) ).transpose();
        }

        SimplePolynomialFunction f = new OptimizedPolynomialFunctionGF2( fixedMonomials.get( 0 ).size() ,
                innerContributionMatrix.rows() , fixedMonomials.toArray( new Monomial[0] ) , innerContributionMatrix
                        .transpose().getRows().toArray( new BitVector[0] ) );
        if (lhs != null) {
            SimplePolynomialFunction g = new OptimizedPolynomialFunctionGF2( f.getInputLength() , f.getOutputLength() ,
                    Arrays.copyOf( growingMonomials.toArray( new Monomial[0] ) , result.size() ), lhs.getRows().toArray( new BitVector[0] ) );
            results.add( f.and( g ) );
        } else {
            results.add( f );
        }

        return results;
    }

    public void bucket(final Monomial m, final BitVector contribution) {
        for (int i = minBucket; i < m.size(); ++i) {
            if (m.get( i )) {
                m.clear( i );
                if (m.isZero()) {
                    outerCols.addRow( contribution );
                    innerSelector.set( i );
                } else {
                    RecursiveLinearizingComposerTree b = buckets[i - minBucket];
                    if (b == null) {
                        b = buckets[i - minBucket] = new RecursiveLinearizingComposerTree( minBucket + 1 ,
                                outerInputLength , new RecursiveLinearizingComposerTree[buckets.length - 1] );
                    }
                    b.bucket( m , contribution );
                }
            }
        }
    }
    
    public BitVector product(BitVector lhs, BitVector rhs, List<Monomial> monomials,
            ConcurrentMap<Monomial, Integer> indices) {
        if( lhs == null ) {
            return rhs;
        }
        BitVector result = new BitVector( monomials.size() );
        for (int i = 0; i < lhs.size(); ++i) {
            if (lhs.getQuick( i )) {
                for (int j = 0; j < rhs.size(); ++j) {
                    if (rhs.getQuick( j )) {
                        Monomial p = monomials.get( i ).product( monomials.get( j ) );

                        Integer indexObj = indices.get( p );
                        int index;
                        if (indexObj == null) {
                            index = monomials.size();
                            indexObj = indices.putIfAbsent( p , index );
                            if (indexObj == null) {
                                monomials.add( p );
                                result.setSize( index + 1 );
                                indexObj = index;
                            }
                        }

                        if (result.getQuick( indexObj )) {
                            result.clear( indexObj );
                        } else {
                            result.set( indexObj );
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Given contributions of outer and inner polynomials as well as the list of inner monomials, computes the product,
     * updating the list of monomials, the map of monomials and returning the resultant contributions.
     * 
     * @param lhs
     * @param rhs
     * @param monomials
     * @param indices
     * @return
     */
    public BitVector product(BitVector lhs, BitVector rhs, List<Monomial> monomials,
            Map<Monomial, Integer> indices) {
        if (lhs == null) {
            return rhs;
        }
        BitVector result = new BitVector( monomials.size() );
        for (int i = 0; i < lhs.size(); ++i) {
            if (lhs.getQuick( i )) {
                for (int j = 0; j < rhs.size(); ++j) {
                    if (rhs.getQuick( j )) {
                        Monomial p = monomials.get( i ).product( monomials.get( j ) );

                        Integer indexObj = indices.get( p );
                        int index;
                        
                        if (indexObj == null) { 
                            //Monomial hasn't been seen before
                            index = monomials.size();
                            monomials.add( p );
                            indices.put( p , index );
                            result.setSize(  monomials.size() );
                        } else {
                            index = indexObj;
                        }
                        
                        if (result.getQuick( index )) {
                            result.clear( index );
                        } else {
                            result.set( index );
                        }
                    }
                }
            }
        }
        return result;
    }

    protected EnhancedBitMatrix select(List<BitVector> rows) {
        return select( innerSelector , rows );
    }

    protected static EnhancedBitMatrix select(BitVector selector, List<BitVector> rows) {
        List<BitVector> filteredRows = Lists.newArrayListWithCapacity( selector.cardinality() );
        for (int i = 0; i < rows.size(); ++i) {
            if (selector.get( i )) {
                filteredRows.add( rows.get( i ) );
            }
        }
        return EnhancedBitMatrix.directFromRows( filteredRows );
    }
}
