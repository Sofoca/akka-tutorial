package de.hpi.ddm.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class HintCrackerTest {
    private String possibleChars = "AHJKGDEFBIC";

    @Test
    public void crack() {
        String hintHash = "1582824a01c4b842e207a51e3cfc47212885e58eb147e33ea29ba212e611904d";
        String hintValue = "HJKGDEFBIC";
        HintCracker hintCracker = new HintCracker(possibleChars.toCharArray());
        String crackResult = hintCracker.crack(hintHash);
        assertEquals(crackResult, hintValue);
    }

    @Test @Ignore
    public void crack2() {
        String hintHash = "1582824a01c4b842e207a51e3cfc47212885e58eb147e33ea29ba212e611904d";
        String hintValue = "HJKGDEFBIC";
        HintCracker hintCracker = new HintCracker("KBCDEFGHIJA".toCharArray());
        String crackResult = hintCracker.crack(hintHash);
        assertEquals(crackResult, hintValue);
    }


    @Test @Ignore
    public void testCustomPassword() {
        HintCracker hintCracker = new HintCracker("ABCDEFGHIJK".toCharArray());
        String crackResult = hintCracker.crack("cb52cadb4b9628e419e4c79ea0721d3d9aecfbcb7022a78eb7f2dbefe5b6f1f8");
        assertEquals(crackResult, "ABCDEFGHIK");
    }

    @Test
    public void crack3() {
        String hintHash = "2097d9edbebf7ccc834ac87b2bf4f2b871908a7f8ef80b35a8608cbb08e0ce07";
        String hintValue = "FG";
        String possibleChars = "AGF";
        HintCracker hintCracker = new HintCracker(possibleChars.toCharArray());
        String crackResult = hintCracker.crack(hintHash);
        assertEquals(crackResult, hintValue);
    }

    @Test
    public void getCharExclusions() {
        char[] possibleChars = "ABCD".toCharArray();
        List<char[]> exclusions = HintCracker.getCharExclusions(possibleChars);
    }
}