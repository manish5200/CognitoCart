package com.manish.smartcart.util;

public class PhoneUtil {

    /**
     * Normalize phone to E.164 format.
     *
     * Examples:
     *  Input: "+91 9876543210" → Output: "+919876543210"
     *  Input: "9876543210" and defaultCountry="+91" → Output: "+919876543210"
     */
    public static String normalize(String phone, String defaultCountryCode) {

        if (phone == null || phone.isBlank()) return null;

        // Remove spaces, hyphens, parentheses
        String cleaned = phone.replaceAll("[\\s()-]", "");

        // If number has no +, add default country
        if (!cleaned.startsWith("+")) {
            cleaned = defaultCountryCode + cleaned;
        }

        return cleaned;
    }
}
