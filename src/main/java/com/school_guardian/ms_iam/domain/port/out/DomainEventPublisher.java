package com.school_guardian.ms_iam.domain.port.out;

public interface DomainEventPublisher {
    void publish(Object event, String topic);
}
