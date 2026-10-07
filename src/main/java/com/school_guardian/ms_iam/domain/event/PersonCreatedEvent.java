package com.school_guardian.ms_iam.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonCreatedEvent {
    @JsonProperty("EventId")
    private UUID eventId;

    @JsonProperty("PersonId")
    private UUID personId;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("IdentificationNumber")
    private String identificationNumber;
}
