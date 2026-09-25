package com.school_guardian.ms_iam.domain.port.in;

import com.school_guardian.ms_iam.domain.event.PersonCreatedEvent;

public interface CreateProfileUseCase {
    void execute(PersonCreatedEvent event, String roleName);
}
