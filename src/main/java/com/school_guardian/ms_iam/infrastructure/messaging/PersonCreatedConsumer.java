package com.school_guardian.ms_iam.infrastructure.messaging;

import com.school_guardian.ms_iam.domain.event.PersonCreatedEvent;
import com.school_guardian.ms_iam.domain.port.in.CreateProfileUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonCreatedConsumer {
    private final CreateProfileUseCase createProfileUseCase;

    @KafkaListener(topics = "student.created", groupId = "iam")
    public void onStudent(PersonCreatedEvent event) { handle(event, "Student"); }

    @KafkaListener(topics = "driver.created", groupId = "iam")
    public void onDriver(PersonCreatedEvent event) { handle(event, "Driver"); }

    @KafkaListener(topics = "admin.created", groupId = "iam")
    public void onAdmin(PersonCreatedEvent event) { handle(event, "Admin"); }

    @KafkaListener(topics = "parent.created", groupId = "iam")
    public void onParent(PersonCreatedEvent event) { handle(event, "Parent"); }

    private void handle(PersonCreatedEvent event, String roleName) {
        try {
            createProfileUseCase.execute(event, roleName);
        } catch (Exception e) {
            log.error("Failed to create profile for person {} role {}: {}",
                    event.getPersonId(), roleName, e.getMessage(), e);
        }
    }
}
