package com.revzz.sap.access.domain;

/**
 * One authorisation value granted by one role, in one system.
 *
 * <p>The same role can hold different values in different systems. That
 * difference is the authorisation drift, and it is why the grant carries
 * the system with it.</p>
 */
public class RoleGrant {

    private String systemId;
    private String roleName;
    private String valueId;

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getValueId() {
        return valueId;
    }

    public void setValueId(String valueId) {
        this.valueId = valueId;
    }
}
