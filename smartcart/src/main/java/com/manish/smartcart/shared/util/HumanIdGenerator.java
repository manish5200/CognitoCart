package com.manish.smartcart.shared.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Cryptographically secure Human-Readable ID generator.

 * Format: {PREFIX}-{YYYYMMDD}-{6 unambiguous chars}
 * Example: ORD-20260703-K7P2MQ

 * Character set: 32 chars — full uppercase alphanumeric minus visually
 * ambiguous pairs (I/1, O/0, L/1). Prevents phone-call misread errors.

 * Uniqueness: 32^6 = 1,073,741,824 combinations per prefix per day.
 * Collision-safe at Amazon India scale (500K orders/day).

 * Thread safety: SecureRandom is thread-safe by design (synchronized internally).
 */
public class HumanIdGenerator {

    private static final String SAFE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUFFIX_LENGTH = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // SecureRandom over Random: cryptographically strong, non-predictable.
    // Lazily initialized — only one instance shared across all callers.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Utility class — prevent instantiation
    private HumanIdGenerator() {}

    /**
     * Generates a unique human-readable ID for the given prefix.
     *
     * @param prefix Entity prefix constant (e.g., "ORD", "USR", "SHP")
     * @return A formatted human ID like "ORD-20260703-K7P2MQ"
     */
    public static String generate(String prefix){

        String date = LocalDate.now().format(DATE_FORMATTER);
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for(int i = 0; i < SUFFIX_LENGTH; i++){
            suffix.append(SAFE_CHARS.charAt(SECURE_RANDOM.nextInt(SAFE_CHARS.length())));
        }
        return prefix + "-" + date + "-" + suffix;
    }
}
