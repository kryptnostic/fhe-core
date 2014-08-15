package com.kryptnostic.multivariate;

import java.io.FileWriter;
import java.io.IOException;

import com.kryptnostic.crypto.PrivateKey;
import com.kryptnostic.crypto.PublicKey;


public class Main {
    public static void main(String[] args) throws IOException {
        PrivateKey privateKey = new PrivateKey( 256 , 128 );
        PublicKey publicKey = new PublicKey(privateKey);
        
        System.out.println( "Writing to publicKey.txt" );
        FileWriter fileWriter = new FileWriter("publicKey.txt");
        fileWriter.write( publicKey.getEncrypter().toString() );
        fileWriter.close();
        System.out.println( "Done writing to publicKey.txt" );
    }
}
