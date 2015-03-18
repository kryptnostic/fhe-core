package com.kryptnostic.multivariate.util;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.multivariate.gf2.CompoundPolynomialFunction;
import com.kryptnostic.multivariate.gf2.Monomial;
import com.kryptnostic.multivariate.gf2.PolynomialFunction;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.CompoundPolynomialFunctionGF2;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;

/**
 * Utility class containing static methods to generate a {@link SimplePolynomialFunction} for the basic boolean and
 * algebraic operators.
 * 
 * @author Nick Hewitt &lt;nick@kryptnostic.com&gt;
 *
 */
public class SimplePolynomialOperators {
    
    public static PolynomialFunction ADDER(int length) {
        return ADDER(length, BINARY_XOR(length), LSH(length, 1).compose(BINARY_AND(length)));
    }

    // TODO: Finish adder generation.
    public static PolynomialFunction ADDER(int length, SimplePolynomialFunction xor, SimplePolynomialFunction carry) {
        CompoundPolynomialFunction cpf = new CompoundPolynomialFunctionGF2();

        /*
         * Actually building out the algebraic representation of an adder is prohibitively expensive. Initialization:
         * carry = ( x & y ) << 1; 256 -> 128 current = x + y; 256 -> 128
         */

        SimplePolynomialFunction halfAdder = SimplePolynomialFunctions.concatenate(xor, carry);

        for (int i = 0; i < length - 1; ++i) {
            cpf.prefix(halfAdder);
        }
        cpf.suffix(xor);
        return cpf;
    }

    public static SimplePolynomialFunction AND(int andLength) {
        int inputLength = andLength >>> 1;
        Monomial[] monomials = new Monomial[inputLength];
        BitVector[] contributions = new BitVector[inputLength];

        for (int i = 0; i < inputLength; ++i) {
            int offsetIndex = i + inputLength;
            monomials[i] = Monomial.linearMonomial(andLength, i).inplaceProd(
                    Monomial.linearMonomial(andLength, offsetIndex));
            BitVector contribution = new BitVector(andLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(andLength, andLength, monomials, contributions);
    }

    public static SimplePolynomialFunction BINARY_AND(int andLength) {
        int inputLength = andLength << 1;
        Monomial[] monomials = new Monomial[andLength];
        BitVector[] contributions = new BitVector[andLength];

        for (int i = 0; i < andLength; ++i) {
            int offsetIndex = i + andLength;
            monomials[i] = Monomial.linearMonomial(inputLength, i).inplaceProd(
                    Monomial.linearMonomial(inputLength, offsetIndex));
            BitVector contribution = new BitVector(andLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, andLength, monomials, contributions);
    }

    /**
     * Generates a {@link SimplePolynomialFunction} which computes the xor of the first half of the input bits with the
     * second half of the inputs, and outputs a vector with the result.
     */
    public static SimplePolynomialFunction BINARY_XOR(int xorLength) {
        int inputLength = xorLength << 1;
        Monomial[] monomials = new Monomial[inputLength];
        BitVector[] contributions = new BitVector[inputLength];

        for (int i = 0; i < xorLength; ++i) {
            int offsetIndex = i + xorLength;
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            monomials[offsetIndex] = Monomial.linearMonomial(inputLength, offsetIndex);
            BitVector contribution = new BitVector(xorLength);
            contribution.set(i);
            contributions[i] = contribution;
            // TODO: In theory everything else makes a copy so we could cheat
            // here and save memory.
            contributions[offsetIndex] = contribution.copy();
        }

        return new OptimizedPolynomialFunctionGF2(inputLength, xorLength, monomials, contributions);
    }

    public static SimplePolynomialFunction HALF_ADDER(int length) {
        return SimplePolynomialFunctions.concatenate(BINARY_XOR(length), LSH(length, 1).compose(BINARY_AND(length)));
    }

    public static SimplePolynomialFunction LSH(int inputLength, int shiftLength) {
        Monomial[] monomials = new Monomial[inputLength - shiftLength];
        BitVector[] contributions = new BitVector[inputLength - shiftLength];
        int upperLimit = inputLength - shiftLength;
        for (int i = 0; i < upperLimit; ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(i + shiftLength);
            contributions[i] = contribution;
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    public static SimplePolynomialFunction NEG(int inputLength) {
        Monomial[] monomials = new Monomial[inputLength + 1];
        BitVector[] contributions = new BitVector[inputLength + 1];
        for (int i = 0; i < ( monomials.length - 1 ); ++i) {
            monomials[i] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(i);
            contributions[i] = contribution;
        }

        monomials[inputLength] = new Monomial(inputLength);
        contributions[inputLength] = new BitVector(inputLength);
        contributions[inputLength].not();
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    public static SimplePolynomialFunction RSH(int inputLength, int shiftLength) {
        Monomial[] monomials = new Monomial[inputLength - shiftLength];
        BitVector[] contributions = new BitVector[inputLength - shiftLength];
        int index;
        for (int i = shiftLength; i < inputLength; ++i) {
            index = i - shiftLength;
            monomials[index] = Monomial.linearMonomial(inputLength, i);
            BitVector contribution = new BitVector(inputLength);
            contribution.set(index);
            contributions[index] = contribution;
        }
        return new OptimizedPolynomialFunctionGF2(inputLength, inputLength, monomials, contributions);
    }

    /**
     * Generates a {@link SimplePolynomialFunction} which computes the xor of the first half of the input bits with the
     * second half of the input bits, and outputs a vector of the same size with the result in the first half of the
     * bits.
     */
    public static SimplePolynomialFunction XOR(int xorLength) {
        int inputLength = xorLength >>> 1;
        Monomial[] monomials = new Monomial[xorLength];
        BitVector[] contributions = new BitVector[xorLength];

        for (int i = 0; i < inputLength; ++i) {
            int offsetIndex = i + inputLength;
            monomials[i] = Monomial.linearMonomial(xorLength, i);
            monomials[offsetIndex] = Monomial.linearMonomial(xorLength, offsetIndex);
            BitVector contribution = new BitVector(xorLength);
            contribution.set(i);
            contributions[i] = contribution;
            /*
             * In theory everything else makes a copy so we could cheat here and save memory.
             */
            contributions[offsetIndex] = contribution.copy();
        }

        return new OptimizedPolynomialFunctionGF2(xorLength, xorLength, monomials, contributions);
    }

    private SimplePolynomialOperators() {
    }
}
