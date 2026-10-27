package com.school_guardian.ms_iam.application.mapper;

import com.school_guardian.ms_iam.application.dto.AcceptTermsRequest;
import com.school_guardian.ms_iam.application.dto.AcceptanceResponse;
import com.school_guardian.ms_iam.domain.model.TermsAcceptance;
import com.school_guardian.ms_iam.domain.model.TermsChannel;
import com.school_guardian.ms_iam.domain.port.in.RecordTermsAcceptanceUseCase.RecordTermsAcceptance;

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

    /**
     * Builds the use-case command from the HTTP request. The client IP is resolved
     * by the web layer (behind the trusted proxies) and the channel comes from the
     * request, defaulting to {@link TermsChannel#UNSPECIFIED} instead of a guess.
     */
    public static RecordTermsAcceptance toCommand(AcceptTermsRequest request, String accessToken, String clientIp) {
        return new RecordTermsAcceptance(
            accessToken,
            request.termsVersion(),
            request.studentProfileIds(),
            clientIp,
            TermsChannel.from(request.channel())
        );
    }
}
