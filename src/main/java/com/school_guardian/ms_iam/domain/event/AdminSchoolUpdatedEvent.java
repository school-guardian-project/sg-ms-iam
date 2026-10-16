package com.school_guardian.ms_iam.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AdminSchoolUpdatedEvent {
    @JsonProperty("EventId")
    private UUID eventId;

    @JsonProperty("PersonId")
    private UUID personId;

    @JsonProperty("SchoolId")
    private UUID schoolId;
}
