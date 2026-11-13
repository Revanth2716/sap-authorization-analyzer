package com.revzz.sap.access.domain;

/**
 * One user holding one role, in one system.
 *
 * <p>The unit of exposure: a user in production holding a role that grants
 * direct table maintenance is a finding about that user, not about the
 * role.</p>
 */
public class UserRoleAssignment {

    private String systemId;
    private String userId;
    private String roleName;

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
