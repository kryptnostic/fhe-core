package com.kryptnostic.multivariate;

import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        long[] test = Arrays.copyOfRange(new long[]{0,1,2,3,4,5} , 0, 2 );
        System.out.println( "Length: " + test.length );
        System.out.println( "t[0]: " + test[0] );
        System.out.println( "t[1]: " + test[1] );
        System.out.println( "t[2]: " + test[2] );
    }
}
