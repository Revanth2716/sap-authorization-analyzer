package com.revzz.sap.access.repo;

import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.AuthObject;
import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.RoleGrant;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserRoleAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * The access data: the roles, the authorisation values, the grants, the
 * assignments and the segregation-of-duties rules.
 *
 * <p>All statements are portable ANSI SQL, so the same repository runs
 * against the local H2 database and against a live SAP HANA instance.</p>
 */
@Repository
public class AuthorizationRepository {

    private final JdbcTemplate jdbc;

    public AuthorizationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AccessSystem> SYSTEM_MAPPER =
            new RowMapper<AccessSystem>() {
                @Override
                public AccessSystem mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AccessSystem system = new AccessSystem();
                    system.setSystemId(rs.getString("SYSTEM_ID"));
                    system.setDescription(rs.getString("DESCRIPTION"));
                    system.setSystemRole(rs.getString("SYS_ROLE"));
                    system.setTarget("Y".equalsIgnoreCase(rs.getString("IS_TARGET")));
                    return system;
                }
            };

    private static final RowMapper<RoleProfile> ROLE_MAPPER =
            new RowMapper<RoleProfile>() {
                @Override
                public RoleProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
                    RoleProfile role = new RoleProfile();
                    role.setRoleName(rs.getString("ROLE_NAME"));
                    role.setDescription(rs.getString("DESCRIPTION"));
                    role.setRoleType(rs.getString("ROLE_TYPE"));
                    role.setParentRole(rs.getString("PARENT_ROLE"));
                    role.setOwner(rs.getString("OWNER"));
                    role.setStatus(rs.getString("STATUS"));
                    return role;
                }
            };

    private static final RowMapper<AuthObject> OBJECT_MAPPER =
            new RowMapper<AuthObject>() {
                @Override
                public AuthObject mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AuthObject object = new AuthObject();
                    object.setObjectName(rs.getString("OBJECT_NAME"));
                    object.setDescription(rs.getString("DESCRIPTION"));
                    object.setSensitive("Y".equalsIgnoreCase(rs.getString("SENSITIVE")));
                    object.setSensitivity(rs.getString("SENSITIVITY"));
                    return object;
                }
            };

    private static final RowMapper<AuthValue> VALUE_MAPPER =
            new RowMapper<AuthValue>() {
                @Override
                public AuthValue mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AuthValue value = new AuthValue();
                    value.setValueId(rs.getString("VALUE_ID"));
                    value.setObjectName(rs.getString("OBJECT_NAME"));
                    value.setAuthName(rs.getString("AUTH_NAME"));
                    value.setFieldName(rs.getString("FIELD_NAME"));
                    value.setFieldValue(rs.getString("FIELD_VALUE"));
                    return value;
                }
            };

    private static final RowMapper<RoleGrant> GRANT_MAPPER =
            new RowMapper<RoleGrant>() {
                @Override
                public RoleGrant mapRow(ResultSet rs, int rowNum) throws SQLException {
                    RoleGrant grant = new RoleGrant();
                    grant.setSystemId(rs.getString("SYSTEM_ID"));
                    grant.setRoleName(rs.getString("ROLE_NAME"));
                    grant.setValueId(rs.getString("VALUE_ID"));
                    return grant;
                }
            };

    private static final RowMapper<UserRoleAssignment> ASSIGNMENT_MAPPER =
            new RowMapper<UserRoleAssignment>() {
                @Override
                public UserRoleAssignment mapRow(ResultSet rs, int rowNum) throws SQLException {
                    UserRoleAssignment assignment = new UserRoleAssignment();
                    assignment.setSystemId(rs.getString("SYSTEM_ID"));
                    assignment.setUserId(rs.getString("USERID"));
                    assignment.setRoleName(rs.getString("ROLE_NAME"));
                    return assignment;
                }
            };

    private static final RowMapper<SodRule> SOD_MAPPER =
            new RowMapper<SodRule>() {
                @Override
                public SodRule mapRow(ResultSet rs, int rowNum) throws SQLException {
                    SodRule rule = new SodRule();
                    rule.setRuleId(rs.getString("RULE_ID"));
                    rule.setRoleA(rs.getString("ROLE_A"));
                    rule.setRoleB(rs.getString("ROLE_B"));
                    rule.setSeverity(rs.getString("SEVERITY"));
                    rule.setDescription(rs.getString("DESCRIPTION"));
                    return rule;
                }
            };

    // -- the landscape --------------------------------------------------------

    public List<AccessSystem> findSystems() {
        return jdbc.query("SELECT SYSTEM_ID, DESCRIPTION, SYS_ROLE, IS_TARGET FROM AUTH_SYSTEM "
                + "ORDER BY IS_TARGET DESC, SYSTEM_ID", SYSTEM_MAPPER);
    }

    public AccessSystem findSystem(String systemId) {
        List<AccessSystem> rows = jdbc.query("SELECT SYSTEM_ID, DESCRIPTION, SYS_ROLE, IS_TARGET "
                + "FROM AUTH_SYSTEM WHERE SYSTEM_ID = ?", SYSTEM_MAPPER, systemId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertSystem(AccessSystem system) {
        jdbc.update("INSERT INTO AUTH_SYSTEM (SYSTEM_ID, DESCRIPTION, SYS_ROLE, IS_TARGET) "
                + "VALUES (?,?,?,?)",
                system.getSystemId(), system.getDescription(), system.getSystemRole(),
                system.isTarget() ? "Y" : "N");
    }

    // -- roles ----------------------------------------------------------------

    public List<RoleProfile> findRoles() {
        return jdbc.query("SELECT ROLE_NAME, DESCRIPTION, ROLE_TYPE, PARENT_ROLE, OWNER, STATUS "
                + "FROM AUTH_ROLE ORDER BY ROLE_TYPE DESC, ROLE_NAME", ROLE_MAPPER);
    }

    public RoleProfile findRole(String roleName) {
        List<RoleProfile> rows = jdbc.query("SELECT ROLE_NAME, DESCRIPTION, ROLE_TYPE, "
                + "PARENT_ROLE, OWNER, STATUS FROM AUTH_ROLE WHERE ROLE_NAME = ?",
                ROLE_MAPPER, roleName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int countRoles() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM AUTH_ROLE", Integer.class);
        return count == null ? 0 : count;
    }

    public void insertRole(RoleProfile role) {
        jdbc.update("INSERT INTO AUTH_ROLE (ROLE_NAME, DESCRIPTION, ROLE_TYPE, PARENT_ROLE, "
                        + "OWNER, STATUS) VALUES (?,?,?,?,?,?)",
                role.getRoleName(), role.getDescription(), role.getRoleType(),
                role.getParentRole(), role.getOwner(), role.getStatus());
    }

    // -- authorisation objects and values --------------------------------------

    public List<AuthObject> findObjects() {
        return jdbc.query("SELECT OBJECT_NAME, DESCRIPTION, SENSITIVE, SENSITIVITY "
                + "FROM AUTH_OBJECT ORDER BY SENSITIVITY DESC, OBJECT_NAME", OBJECT_MAPPER);
    }

    public AuthObject findObject(String objectName) {
        List<AuthObject> rows = jdbc.query("SELECT OBJECT_NAME, DESCRIPTION, SENSITIVE, "
                + "SENSITIVITY FROM AUTH_OBJECT WHERE OBJECT_NAME = ?", OBJECT_MAPPER, objectName);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertObject(AuthObject object) {
        jdbc.update("INSERT INTO AUTH_OBJECT (OBJECT_NAME, DESCRIPTION, SENSITIVE, SENSITIVITY) "
                        + "VALUES (?,?,?,?)",
                object.getObjectName(), object.getDescription(),
                object.isSensitive() ? "Y" : "N", object.getSensitivity());
    }

    public List<AuthValue> findValues() {
        return jdbc.query("SELECT VALUE_ID, OBJECT_NAME, AUTH_NAME, FIELD_NAME, FIELD_VALUE "
                + "FROM AUTH_VALUE ORDER BY AUTH_NAME, FIELD_NAME", VALUE_MAPPER);
    }

    public AuthValue findValue(String valueId) {
        List<AuthValue> rows = jdbc.query("SELECT VALUE_ID, OBJECT_NAME, AUTH_NAME, FIELD_NAME, "
                + "FIELD_VALUE FROM AUTH_VALUE WHERE VALUE_ID = ?", VALUE_MAPPER, valueId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertValue(AuthValue value) {
        jdbc.update("INSERT INTO AUTH_VALUE (VALUE_ID, OBJECT_NAME, AUTH_NAME, FIELD_NAME, "
                        + "FIELD_VALUE) VALUES (?,?,?,?,?)",
                value.getValueId(), value.getObjectName(), value.getAuthName(),
                value.getFieldName(), value.getFieldValue());
    }

    // -- grants ---------------------------------------------------------------

    public List<RoleGrant> findGrants() {
        return jdbc.query("SELECT SYSTEM_ID, ROLE_NAME, VALUE_ID FROM AUTH_ROLE_VALUE "
                + "ORDER BY ROLE_NAME, SYSTEM_ID, VALUE_ID", GRANT_MAPPER);
    }

    public void insertGrant(RoleGrant grant) {
        jdbc.update("INSERT INTO AUTH_ROLE_VALUE (SYSTEM_ID, ROLE_NAME, VALUE_ID) "
                + "VALUES (?,?,?)", grant.getSystemId(), grant.getRoleName(), grant.getValueId());
    }

    // -- assignments ----------------------------------------------------------

    public List<UserRoleAssignment> findAssignments() {
        return jdbc.query("SELECT SYSTEM_ID, USERID, ROLE_NAME FROM AUTH_USER_ROLE "
                + "ORDER BY SYSTEM_ID, USERID, ROLE_NAME", ASSIGNMENT_MAPPER);
    }

    public void insertAssignment(UserRoleAssignment assignment) {
        jdbc.update("INSERT INTO AUTH_USER_ROLE (SYSTEM_ID, USERID, ROLE_NAME) VALUES (?,?,?)",
                assignment.getSystemId(), assignment.getUserId(), assignment.getRoleName());
    }

    public void deleteAssignment(String systemId, String userId, String roleName) {
        jdbc.update("DELETE FROM AUTH_USER_ROLE WHERE SYSTEM_ID = ? AND USERID = ? "
                + "AND ROLE_NAME = ?", systemId, userId, roleName);
    }

    // -- segregation of duties ------------------------------------------------

    public List<SodRule> findSodRules() {
        return jdbc.query("SELECT RULE_ID, ROLE_A, ROLE_B, SEVERITY, DESCRIPTION FROM AUTH_SOD "
                + "ORDER BY SEVERITY DESC, RULE_ID", SOD_MAPPER);
    }

    public void insertSodRule(SodRule rule) {
        jdbc.update("INSERT INTO AUTH_SOD (RULE_ID, ROLE_A, ROLE_B, SEVERITY, DESCRIPTION) "
                        + "VALUES (?,?,?,?,?)",
                rule.getRuleId(), rule.getRoleA(), rule.getRoleB(), rule.getSeverity(),
                rule.getDescription());
    }
}
