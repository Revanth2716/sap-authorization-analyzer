package com.revzz.sap.access.integration;

import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.sap.jco.JCoDestination;
import com.sap.jco.JCoException;
import com.sap.jco.JCoFunction;
import com.sap.jco.JCoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Live SAP gateway - RFC through the SAP Java Connector.
 *
 * <p><b>Only compiled when the {@code with-jco} Maven profile is active</b>,
 * because the SAP Java Connector is published on SAP's own Maven repository
 * rather than on Maven Central. The default build therefore compiles and
 * tests anywhere.</p>
 *
 * <pre>
 *   mvn -Pwith-jco clean package
 * </pre>
 *
 * <p>The remote function module {@code Z_AUTH_EXPOSURE} must be
 * <b>Remote Enabled</b> in transaction SE37, otherwise SAP will refuse the
 * call with {@code RFC function module not found} - which is the single most
 * common reason an RFC adapter that "should work" does not.</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "access.gateway", havingValue = "jco")
public class RfcAccessGateway implements AccessGateway {

    private static final Logger LOG = LoggerFactory.getLogger(RfcAccessGateway.class);

    /** RFC-enabled ABAP function module that reads the roles and assignments. */
    private static final String FUNCTION_AUTH_EXPOSURE = "Z_AUTH_EXPOSURE";

    private final String destinationName;

    public RfcAccessGateway() {
        this("SAPPRD");
    }

    public RfcAccessGateway(String destinationName) {
        this.destinationName = destinationName;
    }

    @Override
    public String description() {
        return "SAP system over RFC (" + destinationName + ")";
    }

    @Override
    public List<AccessSystem> fetchSystems() {
        List<AccessSystem> systems = new ArrayList<AccessSystem>();
        try {
            JCoDestination destination = buildDestination();
            JCoFunction function = JCoFunction.getInstance(destination,
                    FUNCTION_AUTH_EXPOSURE);
            function.execute(destination);

            JCoTable table = function.getTable("IT_ROLES");
            if (table == null) {
                return systems;
            }
            table.first();
            while (!table.isEof()) {
                AccessSystem system = new AccessSystem();
                system.setSystemId(table.getField("SYSTEM_ID").getString());
                system.setDescription(table.getField("DESCRIPTION").getString());
                system.setTarget(true);
                systems.add(system);
                table.next();
            }
        } catch (JCoException exception) {
            LOG.error("Could not read the systems over RFC: {}", exception.getMessage());
        }
        return systems;
    }

    @Override
    public List<RoleProfile> fetchRoles(String systemId) {
        List<RoleProfile> roles = new ArrayList<RoleProfile>();
        try {
            JCoDestination destination = buildDestination();
            JCoFunction function = JCoFunction.getInstance(destination,
                    FUNCTION_AUTH_EXPOSURE);
            function.getParameter("IV_SYSTEM").setString(systemId);
            function.execute(destination);

            JCoTable table = function.getTable("IT_ROLES");
            if (table == null) {
                return roles;
            }
            table.first();
            while (!table.isEof()) {
                RoleProfile role = new RoleProfile();
                role.setRoleName(table.getField("ROLE_NAME").getString());
                role.setDescription(table.getField("DESCRIPTION").getString());
                role.setRoleType(table.getField("ROLE_TYPE").getString());
                role.setParentRole(table.getField("PARENT_ROLE").getString());
                role.setOwner(table.getField("OWNER").getString());
                role.setStatus(table.getField("STATUS").getString());
                roles.add(role);
                table.next();
            }
            LOG.info("Fetched {} roles over RFC for {}", roles.size(), systemId);
        } catch (JCoException exception) {
            LOG.error("Could not read the roles of {} over RFC: {}",
                    systemId, exception.getMessage());
        }
        return roles;
    }

    @Override
    public List<UserRoleAssignment> fetchAssignments(String systemId) {
        List<UserRoleAssignment> assignments = new ArrayList<UserRoleAssignment>();
        try {
            JCoDestination destination = buildDestination();
            JCoFunction function = JCoFunction.getInstance(destination,
                    FUNCTION_AUTH_EXPOSURE);
            function.getParameter("IV_SYSTEM").setString(systemId);
            function.execute(destination);

            JCoTable table = function.getTable("IT_USERS");
            if (table == null) {
                return assignments;
            }
            table.first();
            while (!table.isEof()) {
                UserRoleAssignment assignment = new UserRoleAssignment();
                assignment.setUserId(table.getField("USER_ID").getString());
                assignment.setRoleName(table.getField("ROLE_NAME").getString());
                assignment.setSystemId(table.getField("SYSTEM_ID").getString());
                assignments.add(assignment);
                table.next();
            }
        } catch (JCoException exception) {
            LOG.error("Could not read the assignments of {} over RFC: {}",
                    systemId, exception.getMessage());
        }
        return assignments;
    }

    private JCoDestination buildDestination() throws JCoException {
        Properties properties = new Properties();
        try {
            java.io.InputStream stream = new java.io.FileInputStream("jco.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (java.io.IOException exception) {
            throw new JCoException("jco.properties not found - copy jco.properties.example");
        }

        JCoDestination destination = JCoDestination.createDestination(destinationName);
        destination.setApplicationServer(properties.getProperty("sap.applicationServer"));
        destination.setSystemNumber(properties.getProperty("sap.systemNumber"));
        destination.setClient(properties.getProperty("sap.client"));
        destination.setUser(properties.getProperty("sap.user"));
        destination.setPassword(properties.getProperty("sap.password"));
        destination.setPoolSize(2);
        return destination;
    }
}
