package com.school_guardian.ms_iam.domain.exception;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException() {
        super("Email already in use");
    }
}