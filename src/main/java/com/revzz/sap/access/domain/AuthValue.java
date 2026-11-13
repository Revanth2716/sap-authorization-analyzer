package com.revzz.sap.access.domain;

/**
 * One authorisation value — a field value of an object.
 *
 * <p>{@code F_BKPF_BUK / BUKRS / 1000} is "posting to company code 1000".
 * The set of values a role holds is what the role can actually do, which is
 * why the tool works with the values and not with the object names.</p>
 */
public class AuthValue {

    private String valueId;
    private String objectName;
    private String authName;
    private String fieldName;
    private String fieldValue;

    /** How it reads on a screen: F_BKPF_BUK / BUKRS = 1000 */
    public String getDisplay() {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return authName;
        }
        return authName + " / " + fieldName + " = " + fieldValue;
    }

    public String getValueId() {
        return valueId;
    }

    public void setValueId(String valueId) {
        this.valueId = valueId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getAuthName() {
        return authName;
    }

    public void setAuthName(String authName) {
        this.authName = authName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
}
