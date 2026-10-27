package com.school_guardian.ms_iam.domain.model;

import java.util.Locale;

/**
 * Client through which a terms acceptance was recorded. Stored in
 * {@code Iam.TermsAcceptance.Channel} as its lower-case value, which keeps the
 * historical {@code "mobile"} rows valid.
 *
 * <p>{@link #UNSPECIFIED} is the explicit default when a client does not say which
 * one it is: recording a guess would make the acceptance evidence unreliable.
 */
public enum TermsChannel {
    WEB("web"),
    MOBILE("mobile"),
    UNSPECIFIED("unspecified");

    private final String value;

    TermsChannel(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * Case-insensitive lookup by name or stored value.
     *
     * @throws IllegalArgumentException when the text is not a known channel
     */
    public static TermsChannel from(String text) {
        if (text == null || text.isBlank()) {
            return UNSPECIFIED;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        for (TermsChannel channel : values()) {
            if (channel.value.equals(normalized)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown terms channel: " + text);
    }
}
