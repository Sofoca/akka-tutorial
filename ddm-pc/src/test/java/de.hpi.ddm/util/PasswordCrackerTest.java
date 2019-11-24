package de.hpi.ddm.util;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class PasswordCrackerTest {
    private String password = "GGGFGFFFFG";
    @SuppressWarnings("unused")
    private String passwordHash = "c4712866799881ac48ca55bf78a9540b1883ae033b52109169eb784969be09d5";
    @SuppressWarnings("unused")
    private String[] hintHashes = new String[]{
            "1582824a01c4b842e207a51e3cfc47212885e58eb147e33ea29ba212e611904d",
            "e91aca467f5a2a280213a46aa11842d92577322b5c9899c8e6ffb3dc4b1d80a1",
            "52be0093f91b90872aa54533b8ee9b38f794999bae9371834eca23ce51139b99",
            "8052d9420a20dfc6197d514263f7f0d67f1296569f3c0708fa9030b08a4a908a",
            "ca70f765d8c1b9b7a2162b19ea8e2b410166840f67ee276d297d0ab3bc05f425",
            "570d3ada41deeb943fde8f397076eaef39862f12b0496eadee5b090face72eb5",
            "f224061bd0359a5ca697570df620c34ecda0454fde04f511e4c8608b1f19acb8",
            "01d8adcfdb790125e585e7ed9b77741cd09596dd7c15dcfdc75382a31a4f74ad",
            "4b47ac115f6a91120d444638be98a97d009b9c13fa820d66796d2dad30d18975"
    };
    private Iterable<String> hints = Arrays.asList("HJKGDEFBIC",
            "FCJADEKGHI",
            "FAJBDIEKGH",
            "AGCJEHFKIB",
            "BHKICGFADJ",
            "JIFAGKDBCE",
            "GAHDKJBCEF",
            "EBIKHGDAFC",
            "DJHAFGICBE");
    private String passwordChars = "ABCDEFGHIJK";

    @Test
    public void crack() {
        PasswordCracker passwordCracker = new PasswordCracker(
                passwordChars.toCharArray(),
                2,
                "2097d9edbebf7ccc834ac87b2bf4f2b871908a7f8ef80b35a8608cbb08e0ce07");
        assertEquals("FG", passwordCracker.crack());
    }

    @Test
    public void applyHints() {
        PasswordCracker passwordCracker = new PasswordCracker(
                passwordChars.toCharArray(),
                10,
                "c4712866799881ac48ca55bf78a9540b1883ae033b52109169eb784969be09d5");
        passwordCracker.applyHints(hints);
        assertEquals(password, passwordCracker.crack());
    }

    @Test
    public void testCustomPassword() {
        PasswordCracker passwordCracker = new PasswordCracker(
                "ABCDEFGHIJK".toCharArray(),
                10,
                "fdb4bb3257417896b8e313eaf4979255e74f141fa2ea76bcbe76fa70d3643c7d");
        passwordCracker.applyHints(Arrays.asList(
                "ABCDEFGHIJ",
                "ABKDEFGHIJ",
                "ABCKEFGHIJ",
                "ABCDKFGHIJ",
                "ABCDEKGHIJ",
                "ABCDEFKHIJ",
                "ABCDEFGKIJ",
                "ABCDEFGHKJ",
                "ABCDEFGHIK"
                )
        );
        assertEquals("ABABABABAB", passwordCracker.crack());
    }

    @Test @Ignore
    public void testCustomPassword2() {
        PasswordCracker passwordCracker = new PasswordCracker(
                "ABCDEFGHIJK".toCharArray(),
                10,
                "7c0a495cb43e040309aa1ad7537e74641536ab2bddf38d181a7a8e63d7f022f4");
        passwordCracker.applyHints(Collections.singletonList("ABCDEFGHIJ"));
        assertEquals("ABCDEFGHII", passwordCracker.crack());
    }

    @Test
    public void applyHintsConstructor() {
        PasswordCracker passwordCracker = new PasswordCracker(
                passwordChars.toCharArray(),
                10,
                "c4712866799881ac48ca55bf78a9540b1883ae033b52109169eb784969be09d5",
                hints);
        assertEquals(password, passwordCracker.crack());
    }

    @Test
    public void applyHintsMultiple() {
        PasswordCracker passwordCracker = new PasswordCracker(
                passwordChars.toCharArray(),
                10,
                "c4712866799881ac48ca55bf78a9540b1883ae033b52109169eb784969be09d5",
                hints);
        passwordCracker.applyHints(hints);
        assertEquals(password, passwordCracker.crack());
    }
}