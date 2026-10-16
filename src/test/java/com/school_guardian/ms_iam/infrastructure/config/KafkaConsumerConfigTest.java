package com.school_guardian.ms_iam.infrastructure.config;

import com.school_guardian.ms_iam.domain.event.AdminSchoolUpdatedEvent;
import com.school_guardian.ms_iam.domain.event.PersonCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KafkaConsumerConfigTest {
    @Test
    void adminUpdateTopicUsesItsOwnEventTypeAndPreservesSchool() {
        var config = new KafkaConsumerConfig();
        var factory = config.adminSchoolUpdatedKafkaListenerContainerFactory("localhost:9092");
        var consumer = assertInstanceOf(DefaultKafkaConsumerFactory.class, factory.getConsumerFactory());
        var schoolId = UUID.randomUUID();
        var personId = UUID.randomUUID();
        var json = """
            {"EventId":"%s","PersonId":"%s","SchoolId":"%s"}
            """.formatted(UUID.randomUUID(), personId, schoolId);
        var event = assertInstanceOf(AdminSchoolUpdatedEvent.class,
            consumer.getValueDeserializer().deserialize("admin.school.updated", json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(schoolId, event.getSchoolId());
        assertEquals(personId, event.getPersonId());
    }

    @Test
    void creationTopicsDeserializeSelectedCampus() {
        var config = new KafkaConsumerConfig();
        var consumer = assertInstanceOf(DefaultKafkaConsumerFactory.class, config.consumerFactory("localhost:9092"));
        var campusId = UUID.randomUUID();
        var json = """
            {"EventId":"%s","PersonId":"%s","Email":"test@example.invalid","IdentificationNumber":"123456","CampusId":"%s"}
            """.formatted(UUID.randomUUID(), UUID.randomUUID(), campusId);
        var event = assertInstanceOf(PersonCreatedEvent.class,
            consumer.getValueDeserializer().deserialize("student.created", json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(campusId, event.getCampusId());
    }
}
