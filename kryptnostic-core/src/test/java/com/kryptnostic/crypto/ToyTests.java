package com.kryptnostic.crypto;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.google.common.collect.Sets;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.PolynomialLabeling;
import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.BasePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class ToyTests {
    private static final Logger  logger        = LoggerFactory.getLogger( ToyTests.class );
    private static PrivateKey    privKey;
    private static PublicKey     pubKey;

    private static final Integer INPUT_LENGTH  = 4;
    private static final Integer OUTPUT_LENGTH = 8;

    @BeforeClass
    public static void generateKeys() {
        privKey = new PrivateKey( OUTPUT_LENGTH, INPUT_LENGTH );
        while ( Sets.newHashSet( privKey.getE1().getRows() ).size() != privKey.getE1().getRows().size()
                || Sets.newHashSet( privKey.getE2().getRows() ).size() != privKey.getE2().getRows().size() ) {
            privKey = new PrivateKey( OUTPUT_LENGTH, INPUT_LENGTH );
        }

        pubKey = new PublicKey( privKey );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void printToySystem() throws SingularMatrixException {
        BitVector input = BitVectors.randomVector( INPUT_LENGTH );
        BitVector nonce = BitVectors.randomVector( INPUT_LENGTH );
        Pair<String, Integer> mLabel = Pair.of( "\\mathbf m", INPUT_LENGTH );
        Pair<String, Integer> rLabel = Pair.of( "\\mathbf r", INPUT_LENGTH );
        Pair<String, Integer> xLabel = Pair.of( "\\mathbf x", OUTPUT_LENGTH );
        Pair<String, Integer> s0Label = Pair.of( "\\mathbf s", INPUT_LENGTH );
        Pair<String, Integer> s1Label = Pair.of( "\\mathbf f", OUTPUT_LENGTH );
        Pair<String, Integer> ss0Label = Pair.of( "\\mathbf s", OUTPUT_LENGTH );

        logger.info(
                "\n\\begin{equation}\n\\mu={}\n\\end{equation}",
                BitVectors.asLatexString( privKey.getMixingConstant() ) );

        logger.info( "\n{}", privKey.getA().toLatexString( "\\mathbf A" ) );

        logger.info( "\n{}", privKey.getB().toLatexString( "\\mathbf B" ) );

        logger.info( "\n{}", privKey.getE1().toLatexString( "\\mathcal E_1" ) );

        logger.info( "\n{}", privKey.getE2().toLatexString( "\\mathcal E_2" ) );

        logger.info( "\n{}", privKey.getL().toLatexString( "\\mathbf L" ) );

        logger.info( "\n{}", privKey.getD().toLatexString( "\\mathbf D" ) );

        BasePolynomialFunction g = (BasePolynomialFunction) privKey.getG();
        logger.info( "\n{}", g.toLatexString( "\\mathbf g", new PolynomialLabeling( mLabel, rLabel ) ) );

        SimplePolynomialFunction X = SimplePolynomialFunctions.identity( privKey.getE1().rows() );
        BasePolynomialFunction gOfX = (BasePolynomialFunction) privKey.getA().add( privKey.getB() ).inverse()
                .multiply( privKey.getL().add( privKey.getD() ) ).multiply( X );
        logger.info( "\n\\begin{equation} \\mathbf g( \\mathbf x ) = (\\mathbf A+\\mathbf B)^{-1}(\\mathbf L+ \\mathbf D)\\mathbf x \\end{equation}" );
        logger.info( "\n{}", gOfX.toLatexString( "\\mathbf g", new PolynomialLabeling( xLabel ) ) );

        BasePolynomialFunction f1 = (BasePolynomialFunction) privKey.getComplexityChain()[ 0 ];
        logger.info( "C_0: \n{}", f1.toLatexString( "\\mathcal F_1", new PolynomialLabeling( s0Label ) ) );

        BasePolynomialFunction f2 = (BasePolynomialFunction) privKey.getComplexityChain()[ 1 ];
        logger.info( "C_1: \n{}", f2.toLatexString( "\\mathcal F_2", new PolynomialLabeling( s0Label ) ) );

        logger.info( "Evaluation section.\n\\section{Evaluation}\nIn this section we demonstrate how encryption and decryption operates using the following two vectors.  The vector $\\mathbf m$ represents a message and the vector $\\mathbf r$ is used to randomize the encryption of the message." );

        logger.info(
                "Plaintext: \n\\begin{equation}\n \\mathbf m = {}\\end{equation}",
                BitVectors.asLatexString( input ) );
        logger.info( "Nonce: \n\\begin{equation}\n \\mathbf r = {}\\end{equation}", BitVectors.asLatexString( nonce ) );

        logger.info( "Encryption subsection.\n\\subsection{Encryption Example}" );
        ParameterizedPolynomialFunctionGF2 enc = (ParameterizedPolynomialFunctionGF2) pubKey.getEncrypter();
        List<CompoundPolynomialFunction> chains = enc.getPipelines();
        logger.info(
                "encryptor: \n{}",
                enc.toLatexString( "\\mathcal K_{pub}", new PolynomialLabeling( mLabel, rLabel, s1Label ) ) );

        BitVector chainInput = BitVectors.concatenate( input, nonce );
        int index = 1;
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            String functionName = new StringBuilder( "\\mathcal F_{" ).append( index ).append( "}" ).toString();
            BitVector chainOutput = pf.apply( chainInput );
            if ( index == 1 ) {
                logger.info(
                        "\n\\begin{equation}\n{}( \\mathbf m , \\mathbf r ) = \\textrm{RandomPartition}({})(\\mathbf g( \\mathbf m, \\mathbf r )+\\mu) \n\\end{equation}",
                        functionName,
                        functionName );
                logger.info( "Cpf: \n{}", ( (BasePolynomialFunction) pf ).toLatexString(
                        functionName,
                        new PolynomialLabeling( mLabel, rLabel ) ) );
                logger.info(
                        "F_1(m,r) \n\\begin{equation} \n\\mathbf s={}\\left( {}, {} \\right) = {} \n\\end{equation}",
                        functionName,
                        BitVectors.asLatexString( input ),
                        BitVectors.asLatexString( nonce ),
                        BitVectors.asLatexString( chainOutput ) );
            } else {
                logger.info( "Cpf: \n{}", ( (BasePolynomialFunction) pf ).toLatexString(
                        functionName,
                        new PolynomialLabeling( ss0Label ) ) );
                logger.info(
                        "F_{}(m,r) \n\\begin{equation} \n\\mathbf f={}\\left( {} \\right) = {} \n\\end{equation}",
                        index,
                        functionName,
                        BitVectors.asLatexString( chainInput ),
                        BitVectors.asLatexString( chainOutput ) );
            }

            chainInput = chainOutput;
            index++;
        }

        BitVector ciphertext = pubKey.getEncrypter().apply( BitVectors.concatenate( input, nonce ) );
        logger.info(
                "\n\\begin{equation} \\mathbf x = \\mathcal K_{pub}\\left({},{},{} \\right) = {}\n\\end{equation} ",
                BitVectors.asLatexString( input ),
                BitVectors.asLatexString( nonce ),
                BitVectors.asLatexString( chainInput ),
                BitVectors.asLatexString( ciphertext ) );

        logger.info( "Decryption subsection.\n\\subsection{Decryption Example}" );

        logger.info(
                "Ciphertext\n\\begin{equation}\n \\mathbf x = {}\\end{equation}",
                BitVectors.asLatexString( ciphertext ) );

        ParameterizedPolynomialFunctionGF2 dec = (ParameterizedPolynomialFunctionGF2) privKey.getDecryptor();
        chains = dec.getPipelines();
        logger.info( "decryptor: \n{}", dec.toLatexString( "\\mathcal D", new PolynomialLabeling( xLabel, s1Label ) ) );

        chainInput = BitVectors.concatenate( input, nonce );
        index = 1;
        for ( PolynomialFunction pf : chains.iterator().next().getFunctions() ) {
            String functionName = new StringBuilder( "\\mathcal F_{" ).append( index ).append( "}" ).toString();
            BitVector chainOutput = pf.apply( chainInput );
            if ( index == 1 ) {
                logger.info(
                        "\n\\begin{equation}\n{}( \\mathbf x ) = \\textrm{RandomPartition}({})(\\mathbf g( \\mathbf x ) + \\mu ) \n\\end{equation}",
                        functionName,
                        functionName );
                logger.info(
                        "Cpf: \n{}",
                        ( (BasePolynomialFunction) pf ).toLatexString( functionName, new PolynomialLabeling( xLabel ) ) );
                logger.info(
                        "F_1(m,r) \n\\begin{equation} \n\\mathbf s={}\\left( {} \\right) = {}\n\\end{equation}",
                        functionName,
                        BitVectors.asLatexString( ciphertext ),
                        BitVectors.asLatexString( chainOutput ) );
            } else {
                logger.info( "Cpf: \n{}", ( (BasePolynomialFunction) pf ).toLatexString(
                        functionName,
                        new PolynomialLabeling( ss0Label ) ) );
                logger.info(
                        "F_{}(m,r) \n\\begin{equation} \n\\mathbf f={}\\left( {} \\right) = {} \n\\end{equation}",
                        index,
                        functionName,
                        BitVectors.asLatexString( chainInput ),
                        BitVectors.asLatexString( chainOutput ) );
            }

            chainInput = chainOutput;
            index++;
        }

        BitVector decrypted = privKey.getDecryptor().apply( ciphertext );

        logger.info(
                "\n\\begin{equation} \\mathcal D\\left( {} \\right) = {}\n\\end{equation} ",
                BitVectors.asLatexString( ciphertext ),
                BitVectors.asLatexString( decrypted ) );

        Assert.assertEquals( input, decrypted );
    }
}
