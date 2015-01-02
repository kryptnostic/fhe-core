package com.kryptnostic.crypto;

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.BasePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;

public class ToyTests {
    private static final Logger             logger = LoggerFactory.getLogger( ToyTests.class );
    private static PrivateKey               privKey;
    private static PublicKey                pubKey;
    private static SimplePolynomialFunction decryptor;
    private static SimplePolynomialFunction encryptor;

    private static final Integer            LENGTH = 64;

    @BeforeClass
    public static void generateKeys() {
        privKey = new PrivateKey( 8, 4 );
        pubKey = new PublicKey( privKey );
        decryptor = privKey.getDecryptor();
        encryptor = pubKey.getEncrypter();
    }

    @Test
    public void printToySystem() {
        logger.info( "A: {}", privKey.getA() );

        logger.info( "B: {}", privKey.getB() );

        logger.info( "g: {}", privKey.getG() );

        logger.info( "L: {}", privKey.getL() );

        logger.info( "D: {}", privKey.getD() );

        logger.info( "C_0: {}", privKey.getComplexityChain()[ 0 ] );

        logger.info( "C_1: {}", privKey.getComplexityChain()[ 1 ] );

        ParameterizedPolynomialFunctionGF2 enc = (ParameterizedPolynomialFunctionGF2) pubKey.getEncrypter();
        List<CompoundPolynomialFunction> chains = enc.getPipelines();
        logger.info( "encryptor: {}", enc.toLatexString() );
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            logger.info( "Cpf: {}", ( (BasePolynomialFunction) pf ).toLatexString() );
        }

        ParameterizedPolynomialFunctionGF2 dec = (ParameterizedPolynomialFunctionGF2) pubKey.getEncrypter();
        chains = dec.getPipelines();
        logger.info( "decryptor: {}", dec );
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            logger.info( "Cpf: {}", ( (BasePolynomialFunction) pf ).toLatexString() );
        }

        BitVector input = BitVectors.randomVector( 4 );
        BitVector nonce = BitVectors.randomVector( 4 );

        logger.info( "Plaintext: {}", input );
        logger.info( "Nonce: {}", nonce );

        BitVector ciphertext = pubKey.getEncrypter().apply( BitVectors.concatenate( input, nonce ) );
        logger.info( "Ciphertext: {}", ciphertext );

        BitVector decrypted = privKey.getDecryptor().apply( ciphertext );
        logger.info( "Decrypted: {}", decrypted );
        Assert.assertEquals( input, decrypted );
    }
}
