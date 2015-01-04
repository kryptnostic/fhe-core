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
        logger.info( "A: \n{}", privKey.getA().toLatexString() );

        logger.info( "B: \n{}", privKey.getB().toLatexString() );

        logger.info( "g: \n{}", privKey.getG() );

        logger.info( "E1: \n{}", privKey.getE1().toLatexString() );

        logger.info( "E2: \n{}", privKey.getE2().toLatexString() );
        
        logger.info( "L: \n{}", privKey.getL().toLatexString() );

        logger.info( "D: \n{}", privKey.getD().toLatexString() );

        logger.info( "C_0: \n{}", privKey.getComplexityChain()[ 0 ] );

        logger.info( "C_1: \n{}", privKey.getComplexityChain()[ 1 ] );

        ParameterizedPolynomialFunctionGF2 enc = (ParameterizedPolynomialFunctionGF2) pubKey.getEncrypter();
        List<CompoundPolynomialFunction> chains = enc.getPipelines();
        logger.info( "encryptor: \n{}", enc.toLatexString() );
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            logger.info( "Cpf: \n{}", ( (BasePolynomialFunction) pf ).toLatexString() );
        }

        ParameterizedPolynomialFunctionGF2 dec = (ParameterizedPolynomialFunctionGF2) pubKey.getEncrypter();
        chains = dec.getPipelines();
        logger.info( "decryptor: \n{}", dec.toLatexString() );
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            logger.info( "Cpf: \n{}", ( (BasePolynomialFunction) pf ).toLatexString() );
        }

        BitVector input = BitVectors.randomVector( 4 );
        BitVector nonce = BitVectors.randomVector( 4 );

        logger.info( "Plaintext: \n{}", BitVectors.asBitString( input ) );
        logger.info( "Nonce: \n{}", BitVectors.asBitString( nonce ) );

        BitVector ciphertext = pubKey.getEncrypter().apply( BitVectors.concatenate( input, nonce ) );
        logger.info( "Ciphertext: \n{}", BitVectors.asBitString( ciphertext ) );

        BitVector decrypted = privKey.getDecryptor().apply( ciphertext );
        logger.info( "Decrypted: \n{}", BitVectors.asBitString( decrypted ) );
        Assert.assertEquals( input, decrypted );
    }
}
