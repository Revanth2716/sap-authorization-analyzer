package com.revzz.sap.access.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * What one person can actually reach.
 *
 * <p>This is the blast radius. A role name on its own says nothing; what
 * matters is which sensitive authorisation objects the person ends up with
 * once the composite roles are expanded, and whether they collide with
 * somebody else's responsibilities.</p>
 */
public class UserExposure {

    private String userId;
    private String systemId;
    private List<String> roles = new ArrayList<String>();
    private List<String> atomicRoles = new ArrayList<String>();
    private List<String> sensitiveObjects = new ArrayList<String>();
    private List<SodRule> conflicts = new ArrayList<SodRule>();

    public boolean hasSensitiveAccess() {
        return !sensitiveObjects.isEmpty();
    }

    public String getRoleList() {
        StringBuilder out = new StringBuilder();
        for (String role : roles) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(role);
        }
        return out.toString();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getAtomicRoles() {
        return atomicRoles;
    }

    public void setAtomicRoles(List<String> atomicRoles) {
        this.atomicRoles = atomicRoles;
    }

    public List<String> getSensitiveObjects() {
        return sensitiveObjects;
    }

    public void setSensitiveObjects(List<String> sensitiveObjects) {
        this.sensitiveObjects = sensitiveObjects;
    }

    public List<SodRule> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<SodRule> conflicts) {
        this.conflicts = conflicts;
    }
}
