package de.hpi.ddm.util;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@NoArgsConstructor
public class HintCracker {
    private List<char[]> possibleCharExclusions;

    public HintCracker(char[] possibleChars) {
        this.possibleCharExclusions = getCharExclusions(possibleChars);
    }

    public static List<char[]> getCharExclusions(char[] possibleChars) {
        List<char[]> possibleCharExclusions = new ArrayList<>();
        for (int i = 0; i < possibleChars.length; i++) {
            swap(possibleChars, i, possibleChars.length - 1);
            possibleCharExclusions.add(Arrays.copyOfRange(possibleChars, 0, possibleChars.length - 1));
            swap(possibleChars, i, possibleChars.length - 1);
        }

        return possibleCharExclusions;
    }

    public String crack(String targetHash) {
        for (int i = 0; i < possibleCharExclusions.size(); i++) {
            String possibleResult = crack(targetHash, i);
            //System.out.printf("result of charexclusion %d is %s%n", i, possibleResult);
            if (possibleResult != null) {
                return possibleResult;
            }
        }

        return null;
    }

    private String crack(String targetHash, char[] possibleChars) {
        return heapPermutation(
                Arrays.copyOf(possibleChars, possibleChars.length),
                possibleChars.length,
                permutation -> Sha256.fits(permutation, targetHash)
        );
    }

    private String crack(String targetHash, int charExclusion) {
        return crack(targetHash, possibleCharExclusions.get(charExclusion));
    }

    private static void swap(char[] a, int i, int j) {
        char temp = a[i];
        a[i] = a[j];
        a[j] = temp;
    }

    // Generating all permutations of an array using Heap's Algorithm
    // https://en.wikipedia.org/wiki/Heap's_algorithm
    // https://www.geeksforgeeks.org/heaps-algorithm-for-generating-permutations/
    private static String heapPermutation(char a[], int size, Predicate<String> isHint) {
        // if size becomes 1 then prints the obtained
        // permutation
        if (size == 1) {
            if (isHint.test(new String(a))) {
                return new String(a);
            }
        }

        for (int i = 0; i < size; i++) {
            String possibleResult = heapPermutation(a, size - 1, isHint);
            if (possibleResult != null) {
                return possibleResult;
            }

            // if size is odd, swap first and last
            // element
            if (size % 2 == 1) {
                swap(a, 0, size - 1);
            }

            // If size is even, swap ith and last
            // element
            else {
                swap(a, i, size - 1);
            }
        }

        return null;
    }
}
