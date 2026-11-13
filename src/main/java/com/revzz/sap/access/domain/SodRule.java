package com.revzz.sap.access.domain;

/**
 * A segregation-of-duties rule.
 *
 * <p>Two roles that must never be held by the same person. The rule is
 * about the <em>pair</em>, and the finding is about the people who hold
 * both.</p>
 */
public class SodRule {

    private String ruleId;
    private String roleA;
    private String roleB;
    private String severity;
    private String description;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRoleA() {
        return roleA;
    }

    public void setRoleA(String roleA) {
        this.roleA = roleA;
    }

    public String getRoleB() {
        return roleB;
    }

    public void setRoleB(String roleB) {
        this.roleB = roleB;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
