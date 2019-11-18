package de.hpi.ddm.util;

import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
class HintCracker {
    private char[] possibleChars;
    private String target;

    HintCracker(char[] possibleChars, String targetHash) {
        this();
        this.possibleChars = possibleChars;
        this.target = targetHash;
    }

    String crack() {
        List<String> permutations = new ArrayList<>();
        heapPermutation(
                Arrays.copyOf(possibleChars, possibleChars.length),
                possibleChars.length,
                possibleChars.length,
                permutations
        );

        for (String permutation : permutations) {
            if (Sha256.hash(permutation).equals(target)) {
                return permutation;
            }
        }
        return null;
    }

    // Generating all permutations of an array using Heap's Algorithm
    // https://en.wikipedia.org/wiki/Heap's_algorithm
    // https://www.geeksforgeeks.org/heaps-algorithm-for-generating-permutations/
    private static void heapPermutation(char[] a, int size, int n, List<String> l) {
        // If size is 1, store the obtained permutation
        if (size == 1)
            l.add(new String(a));

        for (int i = 0; i < size; i++) {
            heapPermutation(a, size - 1, n, l);

            // If size is odd, swap first and last element
            if (size % 2 == 1) {
                char temp = a[0];
                a[0] = a[size - 1];
                a[size - 1] = temp;
            }

            // If size is even, swap i-th and last element
            else {
                char temp = a[i];
                a[i] = a[size - 1];
                a[size - 1] = temp;
            }
        }
    }
}
