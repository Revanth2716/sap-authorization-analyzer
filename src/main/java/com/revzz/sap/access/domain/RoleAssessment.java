package com.revzz.sap.access.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The governance assessment of one role.
 *
 * <p>The score is for sorting; the findings are what make it actionable.
 * The severity is the worst finding, so a sensitive exposure can never hide
 * behind an otherwise healthy role.</p>
 */
public class RoleAssessment {

    private RoleProfile role;
    private int score;
    private Severity severity;
    private List<AuthorizationFinding> findings = new ArrayList<AuthorizationFinding>();

    /** The values the role grants, per system. */
    private Map<String, List<AuthValue>> valuesBySystem =
            new LinkedHashMap<String, List<AuthValue>>();

    /** Everyone who holds the role, in any system. */
    private List<String> users = new ArrayList<String>();

    /** The atomic roles a composite resolves to. */
    private List<String> atomicRoles = new ArrayList<String>();

    /** The values the role has in one system but not in the target. */
    private List<AuthValue> missingInTarget = new ArrayList<AuthValue>();

    /** The values the role has in the target but not in the other systems. */
    private List<AuthValue> extraInTarget = new ArrayList<AuthValue>();

    /** The sensitive values the role grants. */
    private List<AuthValue> sensitiveValues = new ArrayList<AuthValue>();

    /** The systems where this role differs from the target. */
    private List<String> driftSystems = new ArrayList<String>();

    public boolean hasFinding(String category) {
        for (AuthorizationFinding finding : findings) {
            if (category.equals(finding.getCategory())) {
                return true;
            }
        }
        return false;
    }

    public int findingCount() {
        return findings.size();
    }

    /** The number of distinct authorisation values the role grants. */
    public int valueCount() {
        int count = 0;
        for (List<AuthValue> values : valuesBySystem.values()) {
            count = Math.max(count, values.size());
        }
        return count;
    }

    public RoleProfile getRole() {
        return role;
    }

    public void setRole(RoleProfile role) {
        this.role = role;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public List<AuthorizationFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<AuthorizationFinding> findings) {
        this.findings = findings;
    }

    public Map<String, List<AuthValue>> getValuesBySystem() {
        return valuesBySystem;
    }

    public void setValuesBySystem(Map<String, List<AuthValue>> valuesBySystem) {
        this.valuesBySystem = valuesBySystem;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public List<String> getAtomicRoles() {
        return atomicRoles;
    }

    public void setAtomicRoles(List<String> atomicRoles) {
        this.atomicRoles = atomicRoles;
    }

    public List<AuthValue> getMissingInTarget() {
        return missingInTarget;
    }

    public void setMissingInTarget(List<AuthValue> missingInTarget) {
        this.missingInTarget = missingInTarget;
    }

    public List<AuthValue> getExtraInTarget() {
        return extraInTarget;
    }

    public void setExtraInTarget(List<AuthValue> extraInTarget) {
        this.extraInTarget = extraInTarget;
    }

    public List<AuthValue> getSensitiveValues() {
        return sensitiveValues;
    }

    public void setSensitiveValues(List<AuthValue> sensitiveValues) {
        this.sensitiveValues = sensitiveValues;
    }

    public List<String> getDriftSystems() {
        return driftSystems;
    }

    public void setDriftSystems(List<String> driftSystems) {
        this.driftSystems = driftSystems;
    }
}
