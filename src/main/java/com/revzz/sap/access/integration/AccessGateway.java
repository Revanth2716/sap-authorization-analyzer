package com.revzz.sap.access.integration;

import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.UserRoleAssignment;

import java.util.List;

/**
 * The boundary between the analyzer and a real SAP landscape.
 *
 * <p>Everything above this interface works on plain domain objects and knows
 * nothing about RFC, JCo or ABAP. Everything below it knows how to ask a
 * real system for its roles and assignments.</p>
 *
 * <p>Two implementations exist:</p>
 * <ul>
 *   <li>{@code LocalAccessGateway} - the default, reads the local
 *       database. It is what the demo runs on.</li>
 *   <li>{@code RfcAccessGateway} - the live path, calls the RFC-enabled
 *       ABAP function module {@code Z_AUTH_EXPOSURE} through the SAP Java
 *       Connector. It is only compiled when the {@code with-jco} Maven
 *       profile is active, because the SAP Java Connector is not published
 *       on Maven Central.</li>
 * </ul>
 */
public interface AccessGateway {

    /** A short description of where the data comes from, shown in the UI. */
    String description();

    /** The systems of the landscape. */
    List<AccessSystem> fetchSystems();

    /** The roles of one system. */
    List<RoleProfile> fetchRoles(String systemId);

    /** Who holds what, in one system. */
    List<UserRoleAssignment> fetchAssignments(String systemId);
}
