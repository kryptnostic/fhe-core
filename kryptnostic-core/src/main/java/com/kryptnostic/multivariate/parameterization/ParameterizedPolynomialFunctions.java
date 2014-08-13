package com.kryptnostic.multivariate.parameterization;

import java.util.Iterator;
import java.util.List;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.kryptnostic.multivariate.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.CompoundPolynomialFunctions;
import com.kryptnostic.multivariate.PolynomialFunctionGF2;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;

public final class ParameterizedPolynomialFunctions {
    private ParameterizedPolynomialFunctions(){}

    public static SimplePolynomialFunction extend( int extendedSize , SimplePolynomialFunction f ) {
        Preconditions.checkArgument( extendedSize%64 == 0, "Extension size must be multiple of 64." );
        Monomial[] originalMonomials = f.getMonomials();
        BitVector[] originalContributions = f.getContributions();
        Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
        BitVector[] newContributions = new BitVector[ originalContributions.length ];

        for( int i = 0 ; i < newMonomials.length ; ++i ) {
            newMonomials[ i ] = originalMonomials[ i ].extend( extendedSize );
            newContributions[ i ] = originalContributions[ i ].copy();
        }

        return new PolynomialFunctionGF2( extendedSize , f.getOutputLength() , newMonomials , newContributions );
    }

    public static SimplePolynomialFunction extendAndShift( int extendedSize, int shiftSize , SimplePolynomialFunction f ) {
        Preconditions.checkArgument( shiftSize%64 == 0, "Shift size must be multiple of 64." );
        Monomial[] originalMonomials = f.getMonomials();
        BitVector[] originalContributions = f.getContributions();
        Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
        BitVector[] newContributions = new BitVector[ originalContributions.length ];

        for( int i = 0 ; i < newMonomials.length ; ++i ) {
            newMonomials[ i ] = originalMonomials[ i ].extendAndShift( extendedSize , shiftSize );
            newContributions[ i ] = originalContributions[ i ].copy();
        }

        return new PolynomialFunctionGF2( extendedSize , f.getOutputLength() , newMonomials , newContributions );
    }

    public static SimplePolynomialFunction extendAndShift( int extendedSize, int baseIndex, int shiftSize , SimplePolynomialFunction f ) {
        Preconditions.checkArgument( shiftSize%64 == 0, "Shift size must be multiple of 64." );
        Monomial[] originalMonomials = f.getMonomials();
        BitVector[] originalContributions = f.getContributions();
        Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
        BitVector[] newContributions = new BitVector[ originalContributions.length ];

        for( int i = 0 ; i < newMonomials.length ; ++i ) {
            newMonomials[ i ] = originalMonomials[ i ].extendAndShift( extendedSize , baseIndex, shiftSize );
            newContributions[ i ] = originalContributions[ i ].copy();
        }

        return new PolynomialFunctionGF2( extendedSize , f.getOutputLength() , newMonomials , newContributions );
    }

    public static CompoundPolynomialFunction extendAndMap( int extendedSize, int[][] srcRanges, int[][] dstRanges , CompoundPolynomialFunction f ) {
        Iterator<PolynomialFunction> fIter = f.getFunctions().iterator();
        PolynomialFunction pf = fIter.next();
        SimplePolynomialFunction extended = null;
        for( int i = 0 ; i < srcRanges.length ; ++i ) {
            Preconditions.checkState( pf instanceof SimplePolynomialFunction , "All functions in compound polynomial function must be polynomials." );
            SimplePolynomialFunction original = (SimplePolynomialFunction) pf;
            Monomial[] originalMonomials = original.getMonomials();
            BitVector[] originalContributions = original.getContributions();
            Monomial[] newMonomials = new Monomial[ originalMonomials.length ];
            BitVector[] newContributions = new BitVector[ originalContributions.length ];
            for( int j = 0 ; j < originalMonomials.length ; ++j ) {
                newMonomials[ j ] = originalMonomials[ j ].extendAndMapRanges( extendedSize , srcRanges , dstRanges );
                newContributions[ j ] = originalContributions[ j ].copy();
            }
            extended = new PolynomialFunctionGF2( extendedSize , pf.getOutputLength() , newMonomials , newContributions );

        }

        List<PolynomialFunction> newFunctions = Lists.newArrayListWithCapacity( f.getFunctions().size() );
        newFunctions.add( extended );
        while( fIter.hasNext() ) {
            newFunctions.add( fIter.next() );
        }

        return new CompoundPolynomialFunctionGF2( newFunctions );
    }

    public static CompoundPolynomialFunction extendAndShift( int extendedSize , int baseIndex, int shiftSize , CompoundPolynomialFunction f ) {
        List<PolynomialFunction> functions = f.getFunctions();
        List<PolynomialFunction> newFunctions = Lists.newArrayListWithCapacity( functions.size() );
        for( PolynomialFunction pf : functions ) {
            Preconditions.checkState( pf instanceof SimplePolynomialFunction , "Every function in pipeline " );
            SimplePolynomialFunction spf = (SimplePolynomialFunction)pf;
            newFunctions.add( extendAndShift( extendedSize , baseIndex , shiftSize , spf ) );
        }

        return new CompoundPolynomialFunctionGF2(newFunctions);
    }


    public static SimplePolynomialFunction xor( SimplePolynomialFunction f, SimplePolynomialFunction g) {
        Preconditions.checkArgument( f.isParameterized() || g.isParameterized() , "At least one of the functions should be parameterized!" );
        Preconditions.checkArgument( f.getInputLength() == g.getInputLength() , "Input lengths must match in order to compute the XOR of two functions.");
        Preconditions.checkArgument( f.getOutputLength() == g.getOutputLength() , "Output lengths must match in order to compute the XOR of two functions.");
        if( f.isParameterized() && g.isParameterized() ) {
            /*
             * Both functions are parameterized.  We need to check whether the pipelines are equal or whether one pipeline will have to be concatenated with the other.
             */
            ParameterizedPolynomialFunctionGF2 ppfF = (ParameterizedPolynomialFunctionGF2)f;
            ParameterizedPolynomialFunctionGF2 ppfG = (ParameterizedPolynomialFunctionGF2)g;

            if( ppfF.getPipelines().equals( ppfG.getPipelines() ) ) {
                //Pipelines are the same no need for concatenation, we can just xor directly.
                SimplePolynomialFunction partialResult =  wrapAsNonParameterizedFunction( ppfF ).xor( wrapAsNonParameterizedFunction( ppfG ) );
                return new ParameterizedPolynomialFunctionGF2( f.getInputLength() , f.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , ppfF.getPipelines() );
            } else {
                //Pipelines are not equal. Append g to f.
                int extendedSize = ppfF.getInputLength() + ppfF.getPipelineOutputLength() + ppfG.getPipelineOutputLength();
                SimplePolynomialFunction shiftedRhs = ParameterizedPolynomialFunctions.extendAndShift( extendedSize , g.getInputLength(), ppfF.getPipelineOutputLength() , g );
                SimplePolynomialFunction extendedLhs = ParameterizedPolynomialFunctions.extend( extendedSize, f );
                SimplePolynomialFunction partialResult = shiftedRhs.xor( extendedLhs );
                Iterable<CompoundPolynomialFunction> pipelines = Iterables.concat( ppfF.getPipelines() , ppfG.getPipelines() );
                return new ParameterizedPolynomialFunctionGF2( ppfF.getInputLength() , ppfF.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , pipelines );
            }
        } else if ( f.isParameterized() && !g.isParameterized() ) {
            //Problem is that extendedLhs
            ParameterizedPolynomialFunctionGF2 ppfF = (ParameterizedPolynomialFunctionGF2)f;
            int extendedSize = ppfF.getInputLength() + ppfF.getPipelineOutputLength();
            SimplePolynomialFunction extendedLhs = ParameterizedPolynomialFunctions.extend( extendedSize, g );
            SimplePolynomialFunction partialResult = wrapAsNonParameterizedFunction( f ).xor( extendedLhs );
            return new ParameterizedPolynomialFunctionGF2( ppfF.getInputLength() , ppfF.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , ppfF.getPipelines() );
        } else if ( !f.isParameterized() && g.isParameterized() ) {
            return xor( g , f );
        } 

        //We should never reach her, since it will result in an exception at the beginning of the function if neither function is parameterized.
        return null;
    }

    //TODO: Figure out a way to merge with XOR extension logic.
    public static SimplePolynomialFunction and( SimplePolynomialFunction f, SimplePolynomialFunction g ) {
        Preconditions.checkArgument( f.isParameterized() || g.isParameterized() , "At least one of the functions should be parameterized!" );
        Preconditions.checkArgument( f.getInputLength() == g.getInputLength() , "Input lengths must match in order to compute the XOR of two functions.");
        Preconditions.checkArgument( f.getOutputLength() == g.getOutputLength() , "Output lengths must match in order to compute the XOR of two functions.");
        if( f.isParameterized() && g.isParameterized() ) {
            /*
             * Both functions are parameterized.  We need to check whether the pipelines are equal or whether one pipeline will have to be concatenated with the other.
             */
            ParameterizedPolynomialFunctionGF2 ppfF = (ParameterizedPolynomialFunctionGF2)f;
            ParameterizedPolynomialFunctionGF2 ppfG = (ParameterizedPolynomialFunctionGF2)g;

            if( ppfF.getPipelines().equals( ppfG.getPipelines() ) ) {
                //Pipelines are the same no need for concatenation, we can just xor directly.
                SimplePolynomialFunction partialResult =  wrapAsNonParameterizedFunction( ppfF ).and( wrapAsNonParameterizedFunction( ppfG ) );
                return new ParameterizedPolynomialFunctionGF2( f.getInputLength() , f.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , ppfF.getPipelines() );
            } else {
                /*
                 * Pipelines are not equal so we allocate monomials such that variables corresponding to g's pipeline
                 * are positioned after the f's pipeline variables. 
                 */
                int extendedSize = ppfF.getInputLength() + ppfF.getPipelineOutputLength() + ppfG.getPipelineOutputLength();
                SimplePolynomialFunction shiftedRhs = ParameterizedPolynomialFunctions.extendAndShift( extendedSize , g.getInputLength(), ppfF.getPipelineOutputLength() , g );
                SimplePolynomialFunction extendedLhs = ParameterizedPolynomialFunctions.extend( extendedSize, f );
                SimplePolynomialFunction partialResult = shiftedRhs.and( extendedLhs );
                Iterable<CompoundPolynomialFunction> pipelines = Iterables.concat( ppfF.getPipelines() , ppfG.getPipelines() );
                return new ParameterizedPolynomialFunctionGF2( ppfF.getInputLength() , ppfF.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , pipelines );
            }
        } else if ( f.isParameterized() && !g.isParameterized() ) {
            //Problem is that extendedLhs
            ParameterizedPolynomialFunctionGF2 ppfF = (ParameterizedPolynomialFunctionGF2)f;
            int extendedSize = ppfF.getInputLength() + ppfF.getPipelineOutputLength();
            SimplePolynomialFunction extendedLhs = ParameterizedPolynomialFunctions.extend( extendedSize, g );
            SimplePolynomialFunction partialResult = wrapAsNonParameterizedFunction( f ).xor( extendedLhs );
            return new ParameterizedPolynomialFunctionGF2( ppfF.getInputLength() , ppfF.getOutputLength() , partialResult.getMonomials() , partialResult.getContributions() , ppfF.getPipelines() );
        } else if ( !f.isParameterized() && g.isParameterized() ) {
            return xor( g , f );
        } 

        //We should never reach here, since it will result in an exception at the beginning of the function if neither function is parameterized.
        return null;
    }

    public static SimplePolynomialFunction fromUnshiftedVariables( int inputLength, SimplePolynomialFunction base , SimplePolynomialFunction[] pipelines ) {
        /*
         * Need to create parameterized function by shifting newXorVariables 
         */
        CompoundPolynomialFunction pipeline = CompoundPolynomialFunctions.fromFunctions( pipelines );
        int extendedSize = inputLength + pipeline.getOutputLength();
        SimplePolynomialFunction shiftedBase = ParameterizedPolynomialFunctions.extendAndShift( extendedSize , pipeline.getInputLength() , base );
        return new ParameterizedPolynomialFunctionGF2( inputLength , shiftedBase.getOutputLength() , shiftedBase.getMonomials() , shiftedBase.getContributions() , ImmutableList.of( pipeline ) );
    }

    private static SimplePolynomialFunction wrapAsNonParameterizedFunction( SimplePolynomialFunction f ) {
        Preconditions.checkArgument( Preconditions.checkNotNull( f , "Function cannot be null." ).isParameterized() , "Function must be paramterized to wrap.");
        ParameterizedPolynomialFunctionGF2 ppfF = (ParameterizedPolynomialFunctionGF2)f;
        int extendedSize = ppfF.getInputLength() + ppfF.getPipelineOutputLength();
        return new PolynomialFunctionGF2(extendedSize, f.getOutputLength(), f.getMonomials(), f.getContributions() );  
    }


}
