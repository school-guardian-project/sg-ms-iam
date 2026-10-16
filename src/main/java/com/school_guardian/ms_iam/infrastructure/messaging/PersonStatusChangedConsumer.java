package com.school_guardian.ms_iam.infrastructure.messaging;

import com.school_guardian.ms_iam.application.usecase.UpdateProfileStatusService;
import com.school_guardian.ms_iam.domain.event.PersonStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code user.status.changed} from ms-user-management and applies it to
 * the matching IAM profile. Uses its own listener container factory because the
 * payload type differs from the person-created events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonStatusChangedConsumer {

    private final UpdateProfileStatusService updateProfileStatusService;

    @KafkaListener(
        topics = "user.status.changed",
        groupId = "iam",
        containerFactory = "userStatusChangedKafkaListenerContainerFactory")
    public void onStatusChanged(PersonStatusChangedEvent event) {
        try {
            updateProfileStatusService.execute(event.getPersonId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to apply status {} for person {}: {}",
                    event.getStatus(), event.getPersonId(), e.getMessage(), e);
        }
    }
}
