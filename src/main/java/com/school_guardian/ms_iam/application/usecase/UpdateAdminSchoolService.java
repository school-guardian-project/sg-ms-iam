package com.school_guardian.ms_iam.application.usecase;

import com.school_guardian.ms_iam.domain.port.out.ProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateAdminSchoolService {
    private final ProfileRepository profileRepository;

    @Transactional
    public void execute(UUID personId, UUID schoolId) {
        profileRepository.assignSchoolByPersonId(personId, schoolId);
    }
}
