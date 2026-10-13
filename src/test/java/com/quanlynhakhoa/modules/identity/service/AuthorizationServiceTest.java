package com.quanlynhakhoa.modules.identity.service;

import com.quanlynhakhoa.modules.identity.enums.Permission;
import com.quanlynhakhoa.modules.identity.enums.Role;
import com.quanlynhakhoa.modules.identity.exception.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationServiceTest {
    private final AuthorizationService service = new AuthorizationService(new RolePermissionPolicy());

    @Test
    void systemAdminHasAllDeclaredFunctionalPermissions() {
        for (Permission permission : Permission.values()) {
            assertTrue(service.can(Set.of(Role.SYSTEM_ADMIN), permission), permission.name());
        }
    }

    @Test
    void clinicManagerIsNotAutomaticallySystemAdmin() {
        assertFalse(service.can(Set.of(Role.CLINIC_MANAGER), Permission.ROLE_MANAGE));
    }

    @Test
    void receptionistCanCreatePatientButCannotEditMedicalHistory() {
        assertTrue(service.can(Set.of(Role.RECEPTIONIST), Permission.PATIENT_CREATE));
        assertFalse(service.can(Set.of(Role.RECEPTIONIST), Permission.MEDICAL_RECORD_UPDATE));
    }

    @Test
    void combinesPermissionsForMultipleRoles() {
        Set<Role> roles = Set.of(Role.RECEPTIONIST, Role.DOCTOR);
        assertTrue(service.can(roles, Permission.PATIENT_CREATE));
        assertTrue(service.can(roles, Permission.MEDICAL_RECORD_UPDATE));
    }

    @Test
    void anonymousOrUnassignedUserIsDenied() {
        assertFalse(service.can(Set.of(), Permission.PATIENT_READ));
        assertThrows(PermissionDeniedException.class,
                () -> service.require(Set.of(), Permission.PATIENT_READ));
    }

    @Test
    void nullInputNeverGrantsPermission() {
        assertThrows(NullPointerException.class,
                () -> service.can(null, Permission.PATIENT_READ));
        assertThrows(NullPointerException.class,
                () -> service.can(Set.of(Role.DOCTOR), null));
    }

    @Test
    void callersCannotMutateGrantedPermissions() {
        Set<Permission> granted = new RolePermissionPolicy().permissionsFor(Set.of(Role.DOCTOR));
        assertThrows(UnsupportedOperationException.class,
                () -> granted.add(Permission.ROLE_MANAGE));
    }
}
