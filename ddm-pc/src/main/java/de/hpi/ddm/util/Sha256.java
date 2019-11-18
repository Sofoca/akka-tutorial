package de.hpi.ddm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class Sha256 {
    static String hash(String line) {
        try {
            byte[] hashedBytes = MessageDigest.getInstance("SHA-256").digest(String.valueOf(line).getBytes(StandardCharsets.UTF_8));

            StringBuilder stringBuffer = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                stringBuffer.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean fits(String raw, String hash) {
        return hash(raw).equals(hash);
    }
}
