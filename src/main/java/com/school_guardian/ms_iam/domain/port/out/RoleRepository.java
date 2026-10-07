package com.school_guardian.ms_iam.domain.port.out;

import com.school_guardian.ms_iam.domain.model.Role;

public interface RoleRepository {
    Role findByName(String name);
    Role findById(Byte id);
}
