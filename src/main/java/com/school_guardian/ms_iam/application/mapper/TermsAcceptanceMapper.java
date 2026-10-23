package com.school_guardian.ms_iam.application.mapper;

import com.school_guardian.ms_iam.application.dto.AcceptanceResponse;
import com.school_guardian.ms_iam.domain.model.TermsAcceptance;

/**
 * Maps the {@link TermsAcceptance} domain model to the API {@link AcceptanceResponse}.
 *
 * <p>Single source of truth for the acceptance projection: {@code ipAddress} is deliberately
 * not exposed, exactly as the original inline mapping in the terms endpoints did.
 */
public final class TermsAcceptanceMapper {

    private TermsAcceptanceMapper() {
    }

    public static AcceptanceResponse toResponse(TermsAcceptance acceptance) {
        return new AcceptanceResponse(
            acceptance.getId(),
            acceptance.getProfileId(),
            acceptance.getStudentProfileId(),
            acceptance.getTermsVersion(),
            acceptance.getAcceptedAt(),
            acceptance.getChannel()
        );
    }
}
