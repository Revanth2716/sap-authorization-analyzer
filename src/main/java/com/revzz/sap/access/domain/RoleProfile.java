package com.revzz.sap.access.domain;

/**
 * One role.
 *
 * <p>A <b>single</b> role holds authorisations directly. A <b>composite</b>
 * role holds other roles, and the interesting question about it is how far it
 * expands — a composite of ten composites can quietly hand a person a
 * hundred atomic roles.</p>
 */
public class RoleProfile {

    public static final String TYPE_SINGLE = "SINGLE";
    public static final String TYPE_COMPOSITE = "COMPOSITE";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String roleName;
    private String description;
    private String roleType;
    private String parentRole;
    private String owner;
    private String status;

    public boolean isComposite() {
        return TYPE_COMPOSITE.equals(roleType);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getParentRole() {
        return parentRole;
    }

    public void setParentRole(String parentRole) {
        this.parentRole = parentRole;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
