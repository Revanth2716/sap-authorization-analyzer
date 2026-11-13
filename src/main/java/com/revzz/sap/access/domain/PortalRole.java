package com.revzz.sap.access.domain;

/** The portal's own roles - not SAP authorisation roles. */
public enum PortalRole {

    /** Signs in and sees the dashboard. */
    VIEWER,

    /** Everything a viewer sees, plus the role detail and the SoD board. */
    ANALYST,

    /** Everything, including revoking a role assignment and exporting the report. */
    SECURITY_ADMIN;

    public boolean canAnalyse() {
        return this != VIEWER;
    }

    public boolean canOperate() {
        return this == SECURITY_ADMIN;
    }

    public String cssClass() {
        return name().toLowerCase();
    }
}
