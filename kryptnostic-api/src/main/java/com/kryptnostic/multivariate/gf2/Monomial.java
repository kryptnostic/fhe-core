package com.kryptnostic.multivariate.gf2;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import cern.colt.bitvector.BitVector;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.kryptnostic.multivariate.predicates.MonomialOrderHomogeneityPredicate;

public class Monomial extends BitVector {
    private static final long                              serialVersionUID           = -8751413919025034976L;
    private static final MonomialOrderHomogeneityPredicate linearHomogeneityPredicate = new MonomialOrderHomogeneityPredicate(
                                                                                              1 );
    // TODO:Re-enable random seeding.
    private static final Random                            r                          = new SecureRandom();

    public Monomial( int size ) {
        super( size );
    }

    public Monomial( long[] bits, int size ) {
        super( bits, size );
    }

    public boolean hasFactor( Monomial m ) {
        Preconditions.checkArgument(
                m.size() <= size(),
                "Monomial to test as factor cannot be of higher order than monomial that it factors into." );
        return unsafeHasFactor( m );
    }

    public boolean unsafeHasFactor( Monomial m ) {
        for ( int i = 0; i < bits.length; ++i ) {
            if ( ( bits[ i ] & m.bits[ i ] ) != m.bits[ i ] ) {
                return false;
            }
        }
        return true;
    }

    public Optional<Monomial> divide( Monomial m ) {
        Monomial result = clone();
        for ( int i = 0; i < result.bits.length; ++i ) {
            if ( ( result.bits[ i ] & m.bits[ i ] ) != m.bits[ i ] ) {
                return Optional.absent();
            } else {
                result.bits[ i ] ^= m.bits[ i ];
            }
        }
        return Optional.of( result );
    }

    public boolean isZero() {
        for ( long l : bits ) {
            if ( l != 0 ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluates input vector
     * 
     * @param input on which to evaluate the monomial.
     * @return True if the monomial has all corresponding input bits set to 1, false otherwise.
     */
    public boolean eval( BitVector input ) {
        Preconditions.checkArgument(
                size() == input.size(),
                "Number of terms in input doesn't not much number of terms in Monomial." );
        BitVector check = copy();
        check.and( input );
        return check.equals( this );
    }

    public Monomial partialEval( BitVector input ) {
        Preconditions.checkArgument( size() > input.size(), "Size of monomial must be strictly greater than input." );
        Monomial result = wrap( this.partFromTo( 0, input.size() - 1 ) );
        Monomial check = result.clone();
        result.and( input );
        if ( result.equals( check ) ) {
            return wrap( this.partFromTo( input.size(), size() - 1 ) );
        } else {
            return null;
        }
    }

    private static Monomial wrap( BitVector v ) {
        return new Monomial( v.elements(), v.size() );
    }

    public Monomial product( Monomial monomial ) {
        Preconditions.checkArgument(
                this.size() == monomial.size(),
                "Cannot compute product due to polynomial ring mismatch." );
        Monomial result = clone();
        result.or( monomial );
        return result;
    }

    public Monomial inplaceAnd( Monomial monomial ) {
        this.and( monomial );
        return this;
    }

    public Monomial inplaceProd( Monomial monomial ) {
        this.or( monomial );
        return this;
    }

    /**
     * Creates a list of N choose K monomials, where N is the order of this monomial and K is a chosen order &lt; N.
     * 
     * @param order
     * @return
     */
    public Set<Monomial> subsets( int order ) {
        int len = size();
        Set<Monomial> subsets = Sets.newHashSet( Monomial.constantMonomial( len ) );
        for ( int ss = 0; ss < order; ++ss ) {
            Set<Monomial> nextSubsets = Sets.newHashSet();
            for ( Monomial m : subsets ) {
                for ( int i = 0; i < len; ++i ) {
                    if ( this.get( i ) && !m.get( i ) ) {
                        nextSubsets.add( m.clone().chainSet( i ) );
                    }
                }
            }
            subsets = nextSubsets;
        }
        if ( order == 0 ) {
            subsets.add( Monomial.constantMonomial( len ) );
        }
        return subsets;
    }

    public Monomial chainSet( int index ) {
        super.set( index );
        return this;
    }

    @Override
    public Monomial clone() {
        long[] e = this.elements();
        return new Monomial( Arrays.copyOf( e, e.length ), this.size() );
    }

    public Monomial extend( int newSize ) {
        Monomial copy = clone();
        copy.setSize( newSize );
        return copy;
    }

    public Monomial extendAndMapRanges( int newSize, int[] srcRanges, int[][] dstRanges ) {
        Preconditions.checkArgument(
                srcRanges.length == dstRanges.length,
                "Source and destination ranges must be of the same length." );
        Monomial result = new Monomial( newSize );
        result.setSize( newSize );
        for ( int i = 0; i < srcRanges.length; ++i ) {
            result.replaceFromToWith( dstRanges[ i ][ 0 ], dstRanges[ i ][ 1 ], this, srcRanges[ i ] );
        }
        return result;
    }

    public static Monomial randomMonomial( int size, int maxOrder ) {

        int order = r.nextInt( maxOrder ) + 1;
        Monomial monomial = new Monomial( size );

        Set<Integer> terms = Sets.newHashSet();
        while ( terms.size() < order ) {
            terms.add( r.nextInt( size ) );
        }

        for ( int term : terms ) {
            monomial.set( term );
        }

        return monomial;
    }

    public Monomial extendAndShift( int newSize, int shiftSize ) {
        return extendAndShift( newSize, 0, shiftSize );
    }

    public Monomial extendAndShift( int newSize, int baseIndex, int shiftSize ) {
        BitVector extendedShifted = extendAndShift( this, newSize, baseIndex, shiftSize );
        return new Monomial( extendedShifted.elements(), extendedShifted.size() );
    }

    public static BitVector extendAndShift( BitVector v, int newSize, int shiftSize ) {
        return extendAndShift( v, newSize, 0, shiftSize );
    }

    public static BitVector extendAndShift( BitVector v, int newSize, int baseIndex, int shiftSize ) {
        // Preconditions.checkArgument( baseIndex + shiftSize + v.size() <= newSize,
        // "Size difference must be greater than shift size." );
        Monomial m = new Monomial( newSize );
        int startIndex = baseIndex + shiftSize;
        int endIndex = startIndex + v.size() - baseIndex - 1;
        if ( baseIndex > 0 ) {
            BitVector unshifted = v.partFromTo( 0, baseIndex - 1 );
            m.replaceFromToWith( 0, baseIndex - 1, unshifted, 0 );
        }
        BitVector shifted = v.partFromTo( baseIndex, v.size() - 1 );
        m.replaceFromToWith( startIndex, endIndex, shifted, 0 );
        return m;
    }

    public static Monomial constantMonomial( int size ) {
        return new Monomial( size );
    }

    public static Monomial linearMonomial( int size, int term ) {
        Monomial m = new Monomial( size );
        m.set( term );
        return m;
    }

    public static MonomialOrderHomogeneityPredicate getLinearHomogeneityPredicate() {
        return linearHomogeneityPredicate;
    }

    public String toStringMonomial() {
        return toStringMonomial( "x" );
    }

    public String toLatexStringMonomial( String var ) {
        return "\\mathbf " + toStringMonomial( var + "_", " " );
    }

    public String toStringMonomial( String var ) {
        return toStringMonomial( var, "*" );
    }

    public String toStringMonomial( String var, String separator ) {
        StringBuilder rep = new StringBuilder();
        boolean first = true;
        for ( int i = 0; i < size(); ++i ) {
            if ( get( i ) ) {
                if ( !first ) {
                    rep.append( separator );
                } else {
                    first = false;
                }
                rep.append( var ).append( i + 1 );
            }
        }

        return rep.toString();
    }

    public static Monomial fromString( int size, String monomialString ) {
        Iterable<String> components = Splitter.on( CharMatcher.anyOf( "*x" ) ).trimResults().omitEmptyStrings()
                .split( monomialString );
        Monomial m = new Monomial( size );
        for ( String component : components ) {
            m.set( Integer.parseInt( component ) - 1 );
        }

        return m;
    }
}
