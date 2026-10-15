package com.school_guardian.ms_iam.infrastructure.messaging;

import com.school_guardian.ms_iam.application.usecase.UpdateProfileStatusService;
import com.school_guardian.ms_iam.domain.event.PersonStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class PersonStatusChangedConsumerTest {

    private final UpdateProfileStatusService service = mock(UpdateProfileStatusService.class);
    private final PersonStatusChangedConsumer consumer = new PersonStatusChangedConsumer(service);

    @Test
    void delegatesToUseCase() {
        UUID personId = UUID.randomUUID();
        consumer.onStatusChanged(new PersonStatusChangedEvent(UUID.randomUUID(), personId, "a@b.com", "Inactive"));

        verify(service).execute(personId, "Inactive");
    }

    @Test
    void swallowsFailuresSoTheConsumerKeepsRunning() {
        UUID personId = UUID.randomUUID();
        doThrow(new RuntimeException("boom")).when(service).execute(personId, "Inactive");

        consumer.onStatusChanged(new PersonStatusChangedEvent(UUID.randomUUID(), personId, "a@b.com", "Inactive"));

        verify(service).execute(personId, "Inactive");
    }
}
