package com.kryptnostic.multivariate;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;

/**
 * Inefficient variable labeling scheme for generating human readable output.
 * 
 * @author Matthew Tamayo-Rios &lt;matthew@kryptnostic.com&gt;
 *
 */
public class PolynomialLabeling {
    public static String   DEFAULT_LABEL = "\\mathbf x";
    private final String[] labels;
    private final int[]    offsets;
    private final String   varList;

    public PolynomialLabeling( int inputLength ) {
        Preconditions.checkArgument( inputLength > 0, "Input length must be > 0." );
        labels = new String[ inputLength ];
        offsets = new int[ labels.length ];
        varList = DEFAULT_LABEL;
        for ( int i = 0; i < inputLength; ++i ) {
            labels[ i ] = DEFAULT_LABEL;
            offsets[ i ] = 0;
        }
    }

    public PolynomialLabeling( Pair<String, Integer>... baseLabels ) {
        Preconditions.checkNotNull( baseLabels, "Base labels cannot be null." );
        Preconditions.checkArgument( baseLabels.length > 0, "Must provide at least one base label." );

        final StringBuilder varListBuilder = new StringBuilder();
        final Pair<String, Integer> stopLabel = baseLabels[ baseLabels.length - 1 ];

        labels = new String[ getTotalLength( baseLabels ) ];
        offsets = new int[labels.length];
        
        int index = 0;
        int offset = 0;
        
        for ( Pair<String, Integer> baseLabel : baseLabels ) {
            varListBuilder.append( baseLabel.getLeft() );
            if ( !baseLabel.equals( stopLabel ) ) {
                varListBuilder.append( "," );
            }
            for ( int i = 0; i < baseLabel.getRight(); ++i ) {
                offsets[ index ] = offset; 
                labels[ index++ ] = baseLabel.getLeft();
                
            }
            offset+=baseLabel.getRight();
        }
        varList = varListBuilder.toString();
    }

    public String getLabelForIndex( int index ) {
        Preconditions.checkArgument( labels.length > index, "Index is out of bounds for this variable labeling." );
        return labels[ index ];
    }

    public int getAdjustedIndex( int index ) {
        return index-offsets[index] + 1;
    }

    public String getVarList() {
        return varList;
    }

    private int getTotalLength( Pair<String, Integer>... baseLabels ) {
        int totalLength = 0;
        for ( Pair<String, Integer> baseLabel : baseLabels ) {
            totalLength += baseLabel.getRight();
        }
        return totalLength;
    }
}
