package com.revzz.sap.access.integration;

import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.revzz.sap.access.repo.AuthorizationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The default gateway: reads the access data from the local database.
 *
 * <p>This is what the application runs with when there is no SAP system to
 * talk to. It is also the implementation the unit tests exercise, which is
 * why the rest of the code never depends on RFC being available.</p>
 */
@Component
@ConditionalOnProperty(name = "access.gateway", havingValue = "local", matchIfMissing = true)
public class LocalAccessGateway implements AccessGateway {

    private final AuthorizationRepository repository;

    public LocalAccessGateway(AuthorizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String description() {
        return "Local database";
    }

    @Override
    public List<AccessSystem> fetchSystems() {
        return repository.findSystems();
    }

    @Override
    public List<RoleProfile> fetchRoles(String systemId) {
        return repository.findRoles();
    }

    @Override
    public List<UserRoleAssignment> fetchAssignments(String systemId) {
        return repository.findAssignments();
    }
}
