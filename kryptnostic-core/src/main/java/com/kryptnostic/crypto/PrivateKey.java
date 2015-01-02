package com.kryptnostic.crypto;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.NonSquareMatrixException;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.ParameterizedPolynomialFunctions;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

/**
 * Private key class for decrypting data.
 * 
 * @author Matthew Tamayo-Rios
 */
public class PrivateKey {
    private static final String D_PROPERTY = "d";
    private static final String L_PROPERTY = "l";
    private static final String E1_PROPERTY = "e1";
    private static final String E2_PROPERTY = "e2";
    private static final String A_PROPERTY = "a";
    private static final String B_PROPERTY = "b";
    private static final String F_PROPERTY = "f";
    private static final String G_PROPERTY = "g";
    private static final String DECRYPTOR_PROPERTY = "decryptor";
    private static final String MIRRORED_DECRYPTOR_PROPERTY = "mirrored-decryptor";
    private static final String COMPLEXITY_CHAIN_PROPERTY = "complexity-chain";
    private static final String LONGS_PER_BLOCK_PROPERTY = "longs-per-block";

    private static final int DEFAULT_CHAIN_LENGTH = 2; // The value of this constant is fairly arbitrary.
    private static final Logger logger = LoggerFactory.getLogger(PrivateKey.class);
    // private static ObjectMapper mapper = new ObjectMapper();
    private final EnhancedBitMatrix D;
    private final EnhancedBitMatrix L;
    private final EnhancedBitMatrix E1;
    private final EnhancedBitMatrix E2;

    private final EnhancedBitMatrix A;
    private final EnhancedBitMatrix B;

    //TODO: Remove F as it is no longer used.
    private final SimplePolynomialFunction F;
    private final SimplePolynomialFunction G;
    private final SimplePolynomialFunction decryptor;
    private final SimplePolynomialFunction mirroredDecryptor;
    private final SimplePolynomialFunction[] complexityChain;
    private final int longsPerBlock;

    public PrivateKey(int cipherTextBlockLength, int plainTextBlockLength) {
        this(cipherTextBlockLength, plainTextBlockLength, DEFAULT_CHAIN_LENGTH);
    }

    @JsonCreator
    public PrivateKey(@JsonProperty(D_PROPERTY) EnhancedBitMatrix D, @JsonProperty(L_PROPERTY) EnhancedBitMatrix L,
            @JsonProperty(E1_PROPERTY) EnhancedBitMatrix E1, @JsonProperty(E2_PROPERTY) EnhancedBitMatrix E2,
            @JsonProperty(A_PROPERTY) EnhancedBitMatrix A, @JsonProperty(B_PROPERTY) EnhancedBitMatrix B,
            @JsonProperty(F_PROPERTY) SimplePolynomialFunction F, @JsonProperty(G_PROPERTY) SimplePolynomialFunction G,
            @JsonProperty(DECRYPTOR_PROPERTY) SimplePolynomialFunction decryptor,
            @JsonProperty(MIRRORED_DECRYPTOR_PROPERTY) SimplePolynomialFunction mirroredDecryptor,
            @JsonProperty(COMPLEXITY_CHAIN_PROPERTY) SimplePolynomialFunction[] complexityChain,
            @JsonProperty(LONGS_PER_BLOCK_PROPERTY) int longsPerBlock) {
        this.D = D;
        this.L = L;
        this.E1 = E1;
        this.E2 = E2;
        this.A = A;
        this.B = B;
        this.F = F;
        this.G = G;
        this.decryptor = decryptor;
        this.mirroredDecryptor = mirroredDecryptor;
        this.complexityChain = complexityChain;
        this.longsPerBlock = longsPerBlock;
    }

    /**
     * Construct a private key instance that can be used for decrypting data encrypted with the public key.
     * 
     * @param cipherTextBlockLength
     *            Length of the ciphertext output block, should be multiples of 64 bits.
     * @param plainTextBlockLength
     *            Length of the ciphertext output block, should be multiples of 64 bits.
     * @param complexityChainLength
     *            Number of multivariate quadratic equations in the complexity chain.
     */
    public PrivateKey(int cipherTextBlockLength, int plainTextBlockLength, int complexityChainLength) {
        Preconditions.checkArgument(cipherTextBlockLength > plainTextBlockLength,
                "Ciphertext block length must be greater than plaintext block length.");
        boolean initialized = false;
        int rounds = 100000;
        EnhancedBitMatrix e2gen = null, dgen = null, e1gen = null, lgen = null;
        while (!initialized && ( ( --rounds ) != 0 )) {

            /*
             * Loop until valid matrices have been generated.
             */
            try {
                e1gen = EnhancedBitMatrix.randomMatrix(cipherTextBlockLength, plainTextBlockLength);

                dgen = e1gen.getLeftNullifyingMatrix();
                Preconditions.checkState(dgen.multiply(e1gen).isZero(), "Generated D matrix must nullify E1.");

                e2gen = dgen.rightInverse();
                Preconditions.checkState(dgen.multiply(e2gen).isIdentity(),
                        "Generated D matrix must be left generalized inverse of E2.");

                lgen = buildL(e1gen, e2gen);

                initialized = true;

                logger.info("E1GEN: {} x {}", e1gen.rows(), e1gen.cols());
                logger.info("E2GEN: {} x {}", e2gen.rows(), e2gen.cols());
                logger.info("DGEN: {} x {}", dgen.rows(), dgen.cols());
                // logger.info("LGEN: {} x {}" , lgen.rows(), lgen.cols() );
            } catch (SingularMatrixException e1) {
                continue;
            }
        }

        Preconditions.checkState(initialized,
                "Unable to generate private key. Make sure cipherTextBlockLength > plainTextBlockLength ");

        D = dgen;
        L = lgen;
        E1 = e1gen;
        E2 = e2gen;

        EnhancedBitMatrix Agen;
        EnhancedBitMatrix Bgen;

        try {
            do {
                Agen = EnhancedBitMatrix.randomInvertibleMatrix(plainTextBlockLength);
                Bgen = EnhancedBitMatrix.randomInvertibleMatrix(plainTextBlockLength);
            } while (!EnhancedBitMatrix.determinant(Agen.add(Bgen)));
        } catch (NonSquareMatrixException e) {
            // This should never happen.
            throw new Error("Encountered non-square matrix, where non-should exist.");
        }

        A = Agen;
        B = Bgen;
        complexityChain = SimplePolynomialFunctions.arrayOfRandomMultivariateQuadratics(plainTextBlockLength,
                plainTextBlockLength, DEFAULT_CHAIN_LENGTH);
        F = SimplePolynomialFunctions.randomFunction(plainTextBlockLength, plainTextBlockLength, 10, 3);
        G = SimplePolynomialFunctions.randomManyToOneLinearCombination(plainTextBlockLength);

        try {
            decryptor = buildDecryptor();
            mirroredDecryptor = buildMirroredDecryptor();
        } catch (SingularMatrixException e) {
            logger.error("Unable to generate decryptor function due to a singular matrix exception during generation process.");
            throw new InvalidParameterException("Unable to generate decryptor function for private key.");
        }
        longsPerBlock = cipherTextBlockLength >>> 6;
    }

    public SimplePolynomialFunction encryptBinary(SimplePolynomialFunction plaintextFunction) {
        int plaintextLen = E1.cols();
        SimplePolynomialFunction R = SimplePolynomialFunctions.randomFunction(plaintextFunction.getInputLength(),
                plaintextLen);
        SimplePolynomialFunction lhsR = F.compose(R);

        return E1.multiply(plaintextFunction.xor(lhsR)).xor(E2.multiply(R));
    }

    SimplePolynomialFunction encrypt(SimplePolynomialFunction input) {
        return encrypt(input, G);
    }

    public SimplePolynomialFunction encrypt(SimplePolynomialFunction input, SimplePolynomialFunction g) {
        Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipeline = SimplePolynomialFunctions
                .buildNonlinearPipeline(g, complexityChain);

        SimplePolynomialFunction E = E1.multiply(input.xor(A.multiply(g))).xor(E2.multiply(input.xor(B.multiply(g))));
        return E.xor(ParameterizedPolynomialFunctions.fromUnshiftedVariables(g.getInputLength(),
                E1.multiply(pipeline.getLeft()).xor(E2.multiply(pipeline.getLeft())), pipeline.getRight()));
    }

    public SimplePolynomialFunction computeHomomorphicFunction(SimplePolynomialFunction f) {
        return encrypt(f.compose(decryptor), SimplePolynomialFunctions.randomManyToOneLinearCombination(E1.cols()));
    }

    public SimplePolynomialFunction computeBinaryHomomorphicFunction(SimplePolynomialFunction f) {
        return encryptBinary(f.compose(SimplePolynomialFunctions.concatenateInputsAndOutputs(decryptor, decryptor)));
    }

    public EnhancedBitMatrix getD() {
        return D;
    }

    public EnhancedBitMatrix getL() {
        return L;
    }

    public EnhancedBitMatrix getE1() {
        return E1;
    }

    public EnhancedBitMatrix getE2() {
        return E2;
    }

    public EnhancedBitMatrix getA() {
        return A;
    }

    public EnhancedBitMatrix getB() {
        return B;
    }

    public SimplePolynomialFunction getG() {
        return G;
    }
    
    public SimplePolynomialFunction getF() {
        return F;
    }
    
    public SimplePolynomialFunction[] getComplexityChain() {
        return complexityChain;
    }
    
    public int getLongsPerBlock() {
        return longsPerBlock;
    }

    public EnhancedBitMatrix randomizedL() throws SingularMatrixException {
        EnhancedBitMatrix randomL = Preconditions.checkNotNull(E2, "E2 must not be null.").getLeftNullifyingMatrix();
        return randomL.multiply(Preconditions.checkNotNull(E1, "E1 must not be null.")).inverse().multiply(randomL); // Normalize
    }

    byte[] decrypt(byte[] ciphertext) {
        ByteBuffer buffer = ByteBuffer.wrap(ciphertext);
        ByteBuffer decryptedBytes = ByteBuffer.allocate(ciphertext.length >>> 1);
        while (buffer.hasRemaining()) {
            BitVector X = fromBuffer(buffer, longsPerBlock);
            BitVector plaintextVector = decryptor.apply(X);
            toBuffer(decryptedBytes, plaintextVector);
        }
        return decryptedBytes.array();
    }

    public SimplePolynomialFunction getDecryptor() {
        return decryptor;
    }

    public SimplePolynomialFunction getMirroredDecryptor() {
        return mirroredDecryptor;
    }

    public SimplePolynomialFunction buildDecryptor() throws SingularMatrixException {
        /*
         * G( x ) = Inv( A + B ) (L + D) x D( x ) = L x + A G( x ) + c'_1 h'_1 + c'_2 h'_2
         */
        SimplePolynomialFunction X = SimplePolynomialFunctions.identity(E1.rows());
        SimplePolynomialFunction GofX = A.add(B).inverse().multiply(L.add(D)).multiply(X);

        Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipeline = SimplePolynomialFunctions
                .buildNonlinearPipeline(GofX, complexityChain);
        SimplePolynomialFunction DofX = L
                .multiply(X)
                .xor(A.multiply(GofX))
                .xor(ParameterizedPolynomialFunctions.fromUnshiftedVariables(GofX.getInputLength(), pipeline.getLeft(),
                        pipeline.getRight()));
        return DofX;
    }

    public SimplePolynomialFunction buildMirroredDecryptor() throws SingularMatrixException {
        /*
         * G( x ) = Inv( A + B ) (L + D) x D( x ) = L x + A G( x ) + c'_1 h'_1 + c'_2 h'_2
         */
        SimplePolynomialFunction X = SimplePolynomialFunctions.identity(E1.rows());
        SimplePolynomialFunction GofX = A.add(B).inverse().multiply(L.add(D)).multiply(X);

        // SimplePolynomialFunction GofY = new OptimizedPolynomialFunctionGF2( GofX.getInputLength() ,
        // GofX.getOutputLength() , Arrays.copyOf( GofX.getMonomials() , GofX.getMonomials().length ), Arrays.copyOf(
        // GofX.getContributions() , GofX.getContributions().length ) );

        Pair<SimplePolynomialFunction, SimplePolynomialFunction[]> pipeline = SimplePolynomialFunctions
                .buildNonlinearPipeline(GofX, complexityChain);

        SimplePolynomialFunction[] pipelines = pipeline.getRight();
        SimplePolynomialFunction[] mirroredPipelines = new SimplePolynomialFunction[pipelines.length];

        for (int i = 0; i < pipelines.length; ++i) {
            mirroredPipelines[i] = mirror(pipelines[i]);
        }

        SimplePolynomialFunction DofX = mirror(L.multiply(X).xor(A.multiply(GofX))).xor(
                ParameterizedPolynomialFunctions.fromUnshiftedVariables(GofX.getInputLength() << 1,
                        mirror(pipeline.getLeft()), mirroredPipelines));

        return DofX;
    }

    private static SimplePolynomialFunction mirror(SimplePolynomialFunction f) {
        return SimplePolynomialFunctions.concatenateInputsAndOutputs(f, f);
    }

    public byte[] decryptFromEnvelope(Ciphertext ciphertext) {
        /*
         * Decrypt using the message length to discard unneeded bytes.
         */
        return Arrays.copyOf(decrypt(ciphertext.getContents()),
                (int) decryptor.apply(new BitVector(ciphertext.getLength(), longsPerBlock << 6)).elements()[0]);
    }

    protected static void toBuffer(ByteBuffer output, BitVector plaintextVector) {
        long[] plaintextLongs = plaintextVector.elements();
        for (long l : plaintextLongs) {
            output.putLong(l);
        }
    }

    protected static BitVector fromBuffer(ByteBuffer buffer, int longsPerBlock) {
        long[] cipherLongs = new long[longsPerBlock];
        for (int i = 0; i < longsPerBlock; ++i) {
            cipherLongs[i] = buffer.getLong();
            logger.debug("Read the following ciphertext: {}", cipherLongs[i]);
        }

        return new BitVector(cipherLongs, longsPerBlock << 6);
    }

    static EnhancedBitMatrix buildL(EnhancedBitMatrix E1, EnhancedBitMatrix E2) throws SingularMatrixException {
        EnhancedBitMatrix L = E2.getLeftNullifyingMatrix();
        Preconditions.checkState(L.multiply(E2).isZero(), "Generated L matrix must nullify E2.");
        L = L.multiply(E1).inverse().multiply(L); // Normalize
        Preconditions.checkState(L.multiply(E1).isIdentity(),
                "Generated D matrix must be left generalized inverse of E2.");
        return L;
    }
    // public abstract Object decryptObject( Object object , Class<?> clazz );

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( A == null ) ? 0 : A.hashCode() );
        result = prime * result + ( ( B == null ) ? 0 : B.hashCode() );
        result = prime * result + ( ( D == null ) ? 0 : D.hashCode() );
        result = prime * result + ( ( E1 == null ) ? 0 : E1.hashCode() );
        result = prime * result + ( ( E2 == null ) ? 0 : E2.hashCode() );
        result = prime * result + ( ( F == null ) ? 0 : F.hashCode() );
        result = prime * result + ( ( G == null ) ? 0 : G.hashCode() );
        result = prime * result + ( ( L == null ) ? 0 : L.hashCode() );
        result = prime * result + Arrays.hashCode(complexityChain);
        result = prime * result + ( ( decryptor == null ) ? 0 : decryptor.hashCode() );
        result = prime * result + longsPerBlock;
        result = prime * result + ( ( mirroredDecryptor == null ) ? 0 : mirroredDecryptor.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PrivateKey other = (PrivateKey) obj;
        if (A == null) {
            if (other.A != null)
                return false;
        } else if (!A.equals(other.A))
            return false;
        if (B == null) {
            if (other.B != null)
                return false;
        } else if (!B.equals(other.B))
            return false;
        if (D == null) {
            if (other.D != null)
                return false;
        } else if (!D.equals(other.D))
            return false;
        if (E1 == null) {
            if (other.E1 != null)
                return false;
        } else if (!E1.equals(other.E1))
            return false;
        if (E2 == null) {
            if (other.E2 != null)
                return false;
        } else if (!E2.equals(other.E2))
            return false;
        if (F == null) {
            if (other.F != null)
                return false;
        } else if (!F.equals(other.F))
            return false;
        if (G == null) {
            if (other.G != null)
                return false;
        } else if (!G.equals(other.G))
            return false;
        if (L == null) {
            if (other.L != null)
                return false;
        } else if (!L.equals(other.L))
            return false;
        if (!Arrays.equals(complexityChain, other.complexityChain))
            return false;
        if (decryptor == null) {
            if (other.decryptor != null)
                return false;
        } else if (!decryptor.equals(other.decryptor))
            return false;
        if (longsPerBlock != other.longsPerBlock)
            return false;
        if (mirroredDecryptor == null) {
            if (other.mirroredDecryptor != null)
                return false;
        } else if (!mirroredDecryptor.equals(other.mirroredDecryptor))
            return false;
        return true;
    }
}