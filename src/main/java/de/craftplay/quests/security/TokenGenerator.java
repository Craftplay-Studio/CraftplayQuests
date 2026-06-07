package de.craftplay.quests.security;

import java.security.SecureRandom;
import java.util.Locale;

public final class TokenGenerator {

    private static final char[] TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String installToken() {
        return "CPQ-" + block(4) + "-" + block(4) + "-" + block(4);
    }

    public static String recoveryCode() {
        return block(4) + "-" + block(4) + "-" + block(4);
    }

    public static String apiToken(String name) {
        return "cpq_" + name.toLowerCase(Locale.ROOT) + "_" + block(8) + block(8) + block(8) + block(8);
    }

    private static String block(int length) {
        StringBuilder token = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            token.append(TOKEN_ALPHABET[SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length)]);
        }
        return token.toString();
    }
}
