package de.hpi.ddm.util;

import akka.remote.EndpointManager;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

@NoArgsConstructor
public class PasswordCracker {
    private CharOpenHashSet passwordCharacters;
    private int passwordLength;
    private String passwordHash;

    PasswordCracker(char[] passwordChars, int passwordLength, String passwordHash) {
        this();
        this.passwordCharacters = new CharOpenHashSet(passwordChars);
        this.passwordLength = passwordLength;
        this.passwordHash = passwordHash;
    }

    public PasswordCracker(char[] passwordChars, int passwordLength, String passwordHash, Iterable<String> hints) {
        this(passwordChars, passwordLength, passwordHash);
        applyHints(hints);
    }

    public String crack() {
        return findPassword(possiblePassword -> Sha256.fits(possiblePassword, passwordHash));
    }

    void applyHints(Iterable<String> hints) {
        hints.forEach(hint -> passwordCharacters.retainAll(new CharOpenHashSet(hint.toCharArray())));
    }

    // The method that prints all possible strings of length k.
    // It is mainly a wrapper over recursive function printAllKLengthRec()
    // https://www.geeksforgeeks.org/print-all-combinations-of-given-length/
    private String findPassword(Predicate<String> isPassword) {
        char[] set = passwordCharacters.toCharArray();
        return findPassword(set, "", set.length, passwordLength, isPassword);
    }

    // The main recursive method to print all possible strings of length k
    // https://www.geeksforgeeks.org/print-all-combinations-of-given-length/
    private static String findPassword(char[] set, String prefix, int n, int k, Predicate<String> isPassword) {
        // Base case: k is 0,
        // print prefix
        if (k == 0) {
            return isPassword.test(prefix) ? prefix : null;
        }

        // One by one add all characters
        // from set and recursively
        // call for k equals to k-1
        for (int i = 0; i < n; ++i) {
            // Next character of input added
            String newPrefix = prefix + set[i];

            // k is decreased, because
            // we have added a new character
            String possiblePassword = findPassword(set, newPrefix, n, k - 1, isPassword);

            if (possiblePassword != null) {
                return possiblePassword;
            }
        }

        return null;
    }
}
