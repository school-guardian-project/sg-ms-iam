package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.domain.model.TermsAcceptance;

import java.util.List;

public interface ListTermsAcceptancesUseCase {
    List<TermsAcceptance> execute(ListTermsAcceptances command);

    record ListTermsAcceptances(String accessToken) {}
}
