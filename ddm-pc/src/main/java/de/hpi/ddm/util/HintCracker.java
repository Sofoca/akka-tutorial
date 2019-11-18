package de.hpi.ddm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HintCracker {
    private final char[] possibleChars;
    private final String target;

    public HintCracker(char[] possibleChars, String target) {
        this.possibleChars = possibleChars;
        this.target = target;
    }

    public String crack() {
        List<String> permutations = new ArrayList<>();
        heapPermutation(
                Arrays.copyOf(possibleChars, possibleChars.length),
                possibleChars.length,
                possibleChars.length,
                permutations
                );

        for (String permutation : permutations) {
            if (hash(permutation).equals(target)) {
                return permutation;
            }
        }
        return null;
    }

    private static String hash(String line) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(String.valueOf(line).getBytes(StandardCharsets.UTF_8));

            StringBuilder stringBuffer = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                stringBuffer.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
            }
            return stringBuffer.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
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
