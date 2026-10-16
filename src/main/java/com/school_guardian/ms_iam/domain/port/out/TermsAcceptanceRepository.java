package com.school_guardian.ms_iam.domain.port.out;

import com.school_guardian.ms_iam.domain.model.TermsAcceptance;

import java.util.List;
import java.util.UUID;

public interface TermsAcceptanceRepository {
    TermsAcceptance save(TermsAcceptance acceptance);
    List<TermsAcceptance> findByProfileId(UUID profileId);
    List<TermsAcceptance> findByStudentProfileId(UUID studentProfileId);
}
