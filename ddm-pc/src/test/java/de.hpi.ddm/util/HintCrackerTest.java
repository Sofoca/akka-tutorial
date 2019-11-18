package de.hpi.ddm.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class HintCrackerTest {

    @Test
    public void crack() {
        String hintHash = "1582824a01c4b842e207a51e3cfc47212885e58eb147e33ea29ba212e611904d";
        String hintValue = "HJKGDEFBIC";
        HintCracker hintCracker = new HintCracker(hintValue.toCharArray(), hintHash);
        String crackResult = hintCracker.crack();
        assertEquals(crackResult, hintValue);
    }
}