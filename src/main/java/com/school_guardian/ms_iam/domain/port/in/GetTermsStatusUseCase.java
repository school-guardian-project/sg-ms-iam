package com.school_guardian.ms_iam.domain.port.in;

public interface GetTermsStatusUseCase {
    TermsStatus execute(GetTermsStatus command);

    record GetTermsStatus(String accessToken) {}

    record TermsStatus(String termsVersion, boolean accepted) {}
}
