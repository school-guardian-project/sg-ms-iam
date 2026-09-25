package com.school_guardian.ms_iam.domain.model;

import com.school_guardian.ms_iam.shared.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Byte id;
    private String name;
    private String description;
    private Status status;
}
