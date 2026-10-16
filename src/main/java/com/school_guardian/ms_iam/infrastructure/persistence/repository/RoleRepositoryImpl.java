package com.school_guardian.ms_iam.infrastructure.persistence.repository;

import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.domain.port.out.RoleRepository;
import com.school_guardian.ms_iam.infrastructure.persistence.entity.RoleEntity;
import com.school_guardian.ms_iam.shared.Status;
import org.springframework.stereotype.Component;

@Component
public class RoleRepositoryImpl implements RoleRepository {
    private final RoleJpaRepository roleJpaRepository;

    public RoleRepositoryImpl(RoleJpaRepository roleJpaRepository) {
        this.roleJpaRepository = roleJpaRepository;
    }

    @Override
    public Role findByName(String name) {
        RoleEntity entity = roleJpaRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));

        Role role = new Role();
        role.setId(entity.getId());
        role.setName(entity.getName());
        role.setDescription(entity.getDescription());
        role.setStatus(Status.valueOf(entity.getStatus()));

        return role;
    }
}
