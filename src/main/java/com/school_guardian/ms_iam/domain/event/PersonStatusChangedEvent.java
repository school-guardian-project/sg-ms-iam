package com.school_guardian.ms_iam.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Status change of a person account, published by ms-user-management on the
 * {@code user.status.changed} topic. IAM uses it to mirror the status onto
 * {@code Iam.Profile} so login (active profiles only) and driver assignments
 * stay consistent.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonStatusChangedEvent {
    @JsonProperty("EventId")
    private UUID eventId;

    @JsonProperty("PersonId")
    private UUID personId;

    @JsonProperty("Email")
    private String email;

    @JsonProperty("Status")
    private String status;
}
