package com.school_guardian.ms_iam.domain.exception;

import java.util.UUID;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(UUID personId) {
        super("Profile already exists for person " + personId);
    }
}
