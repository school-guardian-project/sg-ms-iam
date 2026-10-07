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

    /**
     * Sede de la persona. Llega en student.created, driver.created y
     * parent.created; en admin.created va null porque un admin pertenece a un
     * colegio, no a una sede.
     *
     * <p>Opcional a proposito: los eventos ya publicados no lo traian y un
     * consumidor antiguo sigue funcionando si no lo encuentra.
     */
    @JsonProperty("CampusId")
    private UUID campusId;

    /**
     * Colegio que administra el admin. Solo llega en admin.created. Con el,
     * ms-iam registra despues la relacion en School.SchoolAdmin mediante gRPC.
     */
    @JsonProperty("SchoolId")
    private UUID schoolId;
}
