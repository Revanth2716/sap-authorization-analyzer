package com.revzz.sap.access.domain;

/**
 * An authorisation object.
 *
 * <p>The sensitivity is the interesting part. Some objects are how a
 * business is run and nobody should hold them alone; some are how the
 * database is administered and holding them is an incident waiting to
 * happen.</p>
 */
public class AuthObject {

    public static final String SENSITIVITY_CRITICAL = "CRITICAL";
    public static final String SENSITIVITY_HIGH = "HIGH";
    public static final String SENSITIVITY_NORMAL = "NORMAL";

    private String objectName;
    private String description;
    private boolean sensitive;
    private String sensitivity;

    public boolean isCritical() {
        return SENSITIVITY_CRITICAL.equals(sensitivity);
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }
}
