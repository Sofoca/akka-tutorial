package de.hpi.ddm.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class Sha256Test {
    private String hintHash = "1582824a01c4b842e207a51e3cfc47212885e58eb147e33ea29ba212e611904d";
    private String hintValue = "HJKGDEFBIC";

    @Test
    public void hash() {
        assertEquals(hintHash, Sha256.hash(hintValue));
    }

    @Test
    public void fits() {
        assertTrue(Sha256.fits(hintValue, hintHash));
    }
}