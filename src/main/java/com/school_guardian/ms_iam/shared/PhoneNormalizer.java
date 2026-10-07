package com.school_guardian.ms_iam.shared;

/**
 * Normalizes phone numbers to the national format stored in {@code UserManagement.Person.Phone}
 * (an INT with no country code, e.g. 3001234567).
 *
 * <p>Accepts E.164 ({@code +573001234567}), international without plus ({@code 573001234567})
 * or national ({@code 3001234567}) input. When the digits start with the configured default
 * country code and what remains looks like a national number, the country code is stripped.
 * Known limitation: the schema stores no country code, so only the default country's numbers
 * are unambiguous.
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    public static long toNationalNumber(String rawPhone, String defaultCountryCode) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }

        String countryCode = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
        if (!countryCode.isEmpty()
                && digits.startsWith(countryCode)
                && digits.length() - countryCode.length() >= 8
                && digits.length() - countryCode.length() <= 12) {
            digits = digits.substring(countryCode.length());
        }

        if (!digits.matches("\\d{6,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        return Long.parseLong(digits);
    }
}
