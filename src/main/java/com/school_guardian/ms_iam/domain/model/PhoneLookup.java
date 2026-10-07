package com.school_guardian.ms_iam.domain.model;

import java.util.UUID;

/** Minimal identity of an active profile resolved by phone (national digits, no country code). */
public record PhoneLookup(UUID profileId, String phone) {
}
