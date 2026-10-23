package com.school_guardian.ms_iam.application.mapper;

import com.school_guardian.ms_iam.application.dto.AuthenticationData;
import com.school_guardian.ms_iam.domain.model.Profile;
import com.school_guardian.ms_iam.domain.model.Role;
import com.school_guardian.ms_iam.shared.Status;

/**
 * Builds the transient {@link Profile} used for credential and status checks from the
 * {@link AuthenticationData} row returned by the authentication repository.
 *
 * <p>Shared by the login and refresh flows so both derive the exact same profile:
 * id, person id, password hash, role id and status (parsed from the raw status string).
 */
public final class ProfileAuthMapper {

    private ProfileAuthMapper() {
    }

    public static Profile toProfile(AuthenticationData data) {
        Profile profile = new Profile();
        profile.setId(data.profileId);
        profile.setPersonId(data.personId);
        profile.setPasswordHash(data.passwordHash);
        Role role = new Role();
        role.setId(data.roleId);
        profile.setRole(role);
        profile.setStatus(Status.valueOf(data.status));
        return profile;
    }
}
