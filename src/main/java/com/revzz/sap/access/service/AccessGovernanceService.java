package com.revzz.sap.access.service;

import com.revzz.sap.access.config.AppProperties;
import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.AuthObject;
import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.AuthorizationFinding;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.Severity;
import com.revzz.sap.access.domain.UserExposure;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.revzz.sap.access.repo.AuthorizationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The access governance.
 *
 * <p>Eight questions, one per role:</p>
 *
 * <ol>
 *   <li><b>What can this role actually do?</b> — the authorisation values
 *       it holds, and whether any of them is sensitive enough to be a
 *       finding on its own.</li>
 *   <li><b>Who holds it together with what?</b> — the
 *       segregation-of-duties conflicts, and <em>who</em> is in them. That
 *       is the blast radius.</li>
 *   <li><b>Does the composite bundle a conflict?</b> — a composite role
 *       that holds both sides of a rule hands the conflict to everybody who
 *       holds the composite.</li>
 *   <li><b>Is it the same in every system?</b> — the same role with a
 *       different set of values in development and production is
 *       authorisation drift, and somebody changed it in one place only.</li>
 *   <li><b>Is an inactive role still assigned?</b></li>
 *   <li><b>How far does the composite expand?</b></li>
 *   <li><b>Does anybody hold it?</b></li>
 *   <li><b>Does it have an owner?</b></li>
 * </ol>
 *
 * <p>The score is 100 minus the penalties and exists only so the roles can
 * be sorted. The <em>severity</em> is the worst finding, so a critical
 * exposure can never hide behind an otherwise healthy role.</p>
 */
@Service
public class AccessGovernanceService {

    /** Penalty for each finding, and the severity it carries. */
    private static final int PENALTY_SENSITIVE_CRITICAL = 35;
    private static final int PENALTY_SENSITIVE_HIGH = 20;
    private static final int PENALTY_SOD_CONFLICT = 35;
    private static final int PENALTY_SOD_CONFLICT_HIGH = 25;
    private static final int PENALTY_BUNDLED_CONFLICT = 35;
    private static final int PENALTY_DRIFT = 30;
    private static final int PENALTY_INACTIVE_ASSIGNED = 25;
    private static final int PENALTY_EXPANSION = 20;
    private static final int PENALTY_ORPHAN = 15;
    private static final int PENALTY_NO_OWNER = 10;

    private final AuthorizationRepository repository;
    private final AppProperties properties;

    public AccessGovernanceService(AuthorizationRepository repository,
                                   AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public List<AccessSystem> systems() {
        return repository.findSystems();
    }

    public List<RoleProfile> roles() {
        return repository.findRoles();
    }

    public List<SodRule> sodRules() {
        return repository.findSodRules();
    }

    /** The governance assessment of every role, worst first. */
    public List<RoleAssessment> assessments() {
        AccessSnapshot snapshot = AccessSnapshot.load(repository);
        List<RoleAssessment> results = new ArrayList<RoleAssessment>();
        for (RoleProfile role : snapshot.roles().values()) {
            results.add(assess(role, snapshot));
        }
        Collections.sort(results, new Comparator<RoleAssessment>() {
            @Override
            public int compare(RoleAssessment left, RoleAssessment right) {
                int bySeverity = left.getSeverity().rank() - right.getSeverity().rank();
                return bySeverity != 0 ? bySeverity : left.getScore() - right.getScore();
            }
        });
        return results;
    }

    /** The governance assessment of one role, or null when it does not exist. */
    public RoleAssessment assess(String roleName) {
        AccessSnapshot snapshot = AccessSnapshot.load(repository);
        RoleProfile role = snapshot.role(roleName);
        return role == null ? null : assess(role, snapshot);
    }

    /** The governance assessment of one role. */
    public RoleAssessment assess(RoleProfile role, AccessSnapshot snapshot) {
        RoleAssessment assessment = new RoleAssessment();
        assessment.setRole(role);

        List<AuthorizationFinding> findings = new ArrayList<AuthorizationFinding>();
        int penalty = 0;

        Map<String, List<AuthValue>> valuesBySystem =
                new LinkedHashMap<String, List<AuthValue>>();
        for (Map.Entry<String, List<String>> entry : snapshot.grantsOf(role.getRoleName())
                .entrySet()) {
            List<AuthValue> values = new ArrayList<AuthValue>();
            for (String valueId : entry.getValue()) {
                AuthValue value = snapshot.value(valueId);
                if (value != null) {
                    values.add(value);
                }
            }
            valuesBySystem.put(entry.getKey(), values);
        }
        assessment.setValuesBySystem(valuesBySystem);
        assessment.setUsers(snapshot.holdersOf(role.getRoleName()));
        assessment.setAtomicRoles(snapshot.atomicRoles(role.getRoleName()));

        // -- 1. what can this role actually do? ------------------------------
        Set<String> seenValues = new HashSet<String>();
        List<AuthValue> sensitiveValues = new ArrayList<AuthValue>();
        for (List<AuthValue> values : valuesBySystem.values()) {
            for (AuthValue value : values) {
                if (!seenValues.add(value.getValueId())) {
                    continue;
                }
                AuthObject object = snapshot.object(value.getObjectName());
                if (object != null && object.isSensitive()) {
                    sensitiveValues.add(value);
                }
            }
        }
        assessment.setSensitiveValues(sensitiveValues);

        if (!sensitiveValues.isEmpty()) {
            Severity severity = Severity.HIGH;
            int cost = PENALTY_SENSITIVE_HIGH;
            StringBuilder names = new StringBuilder();
            for (AuthValue value : sensitiveValues) {
                AuthObject object = snapshot.object(value.getObjectName());
                if (object != null && object.isCritical()) {
                    severity = Severity.CRITICAL;
                    cost = PENALTY_SENSITIVE_CRITICAL;
                }
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(object == null ? value.getObjectName() : object.getObjectName());
            }
            penalty += cost;
            findings.add(new AuthorizationFinding(severity, "SENSITIVE_EXPOSURE",
                    role.getRoleName(),
                    role.getRoleName() + " grants the sensitive authorisation object "
                            + names + ".",
                    "Take the sensitive object out of the role and give it to a smaller, "
                            + "named group. A role that carries table maintenance or payroll "
                            + "is an access request, not a job description."));
        }

        // -- 2. who holds it together with what? ------------------------------
        List<String> conflictDescriptions = new ArrayList<String>();
        int conflictPenalty = 0;
        Severity conflictSeverity = Severity.HIGH;
        for (SodRule rule : snapshot.sodRules()) {
            if (!rule.getRoleA().equals(role.getRoleName())
                    && !rule.getRoleB().equals(role.getRoleName())) {
                continue;
            }
            Map<String, List<String>> conflicting = snapshot.conflictingUsers(rule);
            if (conflicting.isEmpty()) {
                continue;
            }
            if ("CRITICAL".equals(rule.getSeverity())) {
                conflictSeverity = Severity.CRITICAL;
                conflictPenalty = PENALTY_SOD_CONFLICT;
            } else {
                // A less severe rule still costs something - a finding that
                // costs nothing is a finding nobody has to act on.
                conflictPenalty = Math.max(conflictPenalty, PENALTY_SOD_CONFLICT_HIGH);
            }
            conflictDescriptions.add(rule.getRuleId() + " (" + rule.getDescription() + ") held "
                    + "by " + join(conflicting.keySet()));
        }
        if (!conflictDescriptions.isEmpty()) {
            penalty += conflictPenalty;
            findings.add(new AuthorizationFinding(conflictSeverity, "SO_D_CONFLICT",
                    role.getRoleName(),
                    role.getRoleName() + " is part of "
                            + conflictDescriptions.size() + " segregation-of-duties conflict(s): "
                            + join(conflictDescriptions) + ".",
                    "Split the role or change the assignment. The conflict is in the people, "
                            + "not in the role - find out who holds both sides and move one "
                            + "of them."));
        }

        // -- 3. does the composite bundle a conflict? --------------------------
        if (role.isComposite()) {
            List<String> bundled = new ArrayList<String>();
            List<String> effective = snapshot.effectiveRoles(role.getRoleName());
            for (SodRule rule : snapshot.sodRules()) {
                if (effective.contains(rule.getRoleA()) && effective.contains(rule.getRoleB())) {
                    bundled.add(rule.getRoleA() + " + " + rule.getRoleB()
                            + " (" + rule.getRuleId() + ")");
                }
            }
            if (!bundled.isEmpty()) {
                penalty += PENALTY_BUNDLED_CONFLICT;
                findings.add(new AuthorizationFinding(Severity.CRITICAL,
                        "CONFLICT_BUNDLED_IN_COMPOSITE", role.getRoleName(),
                        "The composite role " + role.getRoleName() + " holds both sides of "
                                + bundled.size() + " segregation-of-duties rule(s): "
                                + join(bundled) + ".",
                        "Take one of the two roles out of the composite. Everybody who holds "
                                + role.getRoleName() + " currently holds the conflict, whether "
                                + "they know it or not."));
            }
        }

        // -- 4. is it the same in every system? --------------------------------
        AccessSystem target = snapshot.targetSystem();
        if (target != null) {
            Set<String> targetValues = new HashSet<String>(
                    snapshot.grantsOf(role.getRoleName()).get(target.getSystemId()) == null
                            ? Collections.<String>emptyList()
                            : snapshot.grantsOf(role.getRoleName()).get(target.getSystemId()));
            List<String> driftSystems = new ArrayList<String>();
            List<AuthValue> missing = new ArrayList<AuthValue>();
            List<AuthValue> extra = new ArrayList<AuthValue>();

            for (AccessSystem other : snapshot.systems()) {
                if (other.getSystemId().equals(target.getSystemId())) {
                    continue;
                }
                List<String> otherValues = snapshot.grantsOf(role.getRoleName())
                        .get(other.getSystemId());
                if (otherValues == null) {
                    // The role does not exist in this system at all. That is
                    // not drift - drift means the same role holds different
                    // values, and you cannot compare a role with a role that
                    // is not there.
                    continue;
                }
                Set<String> otherSet = new HashSet<String>(otherValues);
                if (otherSet.equals(targetValues)) {
                    continue;
                }
                driftSystems.add(other.getSystemId());
                for (String valueId : otherSet) {
                    if (!targetValues.contains(valueId)) {
                        AuthValue value = snapshot.value(valueId);
                        if (value != null) {
                            missing.add(value);
                        }
                    }
                }
                for (String valueId : targetValues) {
                    if (!otherSet.contains(valueId)) {
                        AuthValue value = snapshot.value(valueId);
                        if (value != null) {
                            extra.add(value);
                        }
                    }
                }
            }
            assessment.setDriftSystems(driftSystems);
            assessment.setMissingInTarget(missing);
            assessment.setExtraInTarget(extra);

            if (!driftSystems.isEmpty()
                    && driftSystems.size() > properties.getPolicy().getMaxDriftSystems()) {
                penalty += PENALTY_DRIFT;
                findings.add(new AuthorizationFinding(Severity.HIGH, "AUTHORIZATION_DRIFT",
                        role.getRoleName(),
                        "The role is not the same in every system. It differs between "
                                + target.getSystemId() + " and " + join(driftSystems) + ".",
                        "Decide which one is right and transport the difference. A role that "
                                + "differs between development and production means the two "
                                + "systems can behave differently for the same reason."));
            }
        }

        // -- 5. is an inactive role still assigned? -----------------------------
        if (!role.isActive() && !assessment.getUsers().isEmpty()) {
            penalty += PENALTY_INACTIVE_ASSIGNED;
            findings.add(new AuthorizationFinding(Severity.HIGH, "INACTIVE_ROLE_ASSIGNED",
                    role.getRoleName(),
                    role.getRoleName() + " is inactive but is still assigned to "
                            + assessment.getUsers().size() + " user(s): "
                            + join(assessment.getUsers()) + ".",
                    "Remove the assignment. An inactive role that is still held is a role that "
                            + "nobody decided to remove."));
        }

        // -- 6. how far does the composite expand? ------------------------------
        if (role.isComposite()
                && assessment.getAtomicRoles().size()
                > properties.getPolicy().getMaxCompositeExpansion()) {
            penalty += PENALTY_EXPANSION;
            findings.add(new AuthorizationFinding(Severity.MEDIUM, "COMPOSITE_EXPANSION",
                    role.getRoleName(),
                    "The composite role expands to " + assessment.getAtomicRoles().size()
                            + " atomic roles.",
                    "Break it up. A composite that expands this far cannot be reviewed, so "
                            + "nobody ever reviews what it actually grants."));
        }

        // -- 7. does anybody hold it? --------------------------------------------
        if (role.isActive() && assessment.getUsers().isEmpty()) {
            penalty += PENALTY_ORPHAN;
            findings.add(new AuthorizationFinding(Severity.MEDIUM, "ORPHAN_ROLE",
                    role.getRoleName(),
                    "The role is active but nobody holds it.",
                    "Delete it, or find out why it exists. An unused role is still an "
                            + "authorisation somebody can be given by accident."));
        }

        // -- 8. does it have an owner? --------------------------------------------
        if (role.getOwner() == null || role.getOwner().trim().isEmpty()) {
            penalty += PENALTY_NO_OWNER;
            findings.add(new AuthorizationFinding(Severity.LOW, "NO_OWNER",
                    role.getRoleName(),
                    "The role has no owner.",
                    "Give it one. A role without an owner is never reviewed."));
        }

        // -- score and severity -----------------------------------------------------
        int score = Math.max(0, 100 - penalty);
        assessment.setScore(score);
        assessment.setFindings(findings);
        assessment.setSeverity(worstSeverity(findings, score));
        return assessment;
    }

    /** The severity of a role is the worst finding it has. */
    private Severity worstSeverity(List<AuthorizationFinding> findings, int score) {
        Severity worst = null;
        for (AuthorizationFinding finding : findings) {
            if (worst == null || finding.getSeverity().rank() < worst.rank()) {
                worst = finding.getSeverity();
            }
        }
        if (worst != null) {
            return worst;
        }
        if (score >= 80) {
            return Severity.INFO;
        }
        if (score >= 60) {
            return Severity.MEDIUM;
        }
        if (score >= 35) {
            return Severity.HIGH;
        }
        return Severity.CRITICAL;
    }

    // -- the blast radius ------------------------------------------------------

    /**
     * What every person can actually reach.
     *
     * <p>This is the question a security team really asks. A role name on its
     * own says nothing; what matters is which sensitive objects the person
     * ends up with once the composites are expanded, and whether they
     * collide with somebody else's responsibilities.</p>
     */
    public List<UserExposure> exposures() {
        AccessSnapshot snapshot = AccessSnapshot.load(repository);
        List<UserExposure> exposures = new ArrayList<UserExposure>();

        Map<String, Map<String, List<String>>> byUser =
                new LinkedHashMap<String, Map<String, List<String>>>();
        for (UserRoleAssignment assignment : snapshot.assignments()) {
            Map<String, List<String>> bySystem = byUser.get(assignment.getUserId());
            if (bySystem == null) {
                bySystem = new LinkedHashMap<String, List<String>>();
                byUser.put(assignment.getUserId(), bySystem);
            }
            List<String> roles = bySystem.get(assignment.getSystemId());
            if (roles == null) {
                roles = new ArrayList<String>();
                bySystem.put(assignment.getSystemId(), roles);
            }
            if (!roles.contains(assignment.getRoleName())) {
                roles.add(assignment.getRoleName());
            }
        }

        for (Map.Entry<String, Map<String, List<String>>> entry : byUser.entrySet()) {
            for (Map.Entry<String, List<String>> systemEntry : entry.getValue().entrySet()) {
                UserExposure exposure = new UserExposure();
                exposure.setUserId(entry.getKey());
                exposure.setSystemId(systemEntry.getKey());
                exposure.setRoles(systemEntry.getValue());

                List<String> atomic = new ArrayList<String>();
                Set<String> sensitiveObjects = new HashSet<String>();
                for (String roleName : systemEntry.getValue()) {
                    for (String atomicRole : snapshot.atomicRoles(roleName)) {
                        if (!atomic.contains(atomicRole)) {
                            atomic.add(atomicRole);
                        }
                        for (List<String> valueIds : snapshot.grantsOf(atomicRole).values()) {
                            for (String valueId : valueIds) {
                                AuthValue value = snapshot.value(valueId);
                                if (value == null) {
                                    continue;
                                }
                                AuthObject object = snapshot.object(value.getObjectName());
                                if (object != null && object.isSensitive()) {
                                    sensitiveObjects.add(object.getObjectName());
                                }
                            }
                        }
                    }
                }
                exposure.setAtomicRoles(atomic);

                List<String> sensitive = new ArrayList<String>(sensitiveObjects);
                Collections.sort(sensitive);
                exposure.setSensitiveObjects(sensitive);

                List<SodRule> conflicts = new ArrayList<SodRule>();
                List<String> effective = new ArrayList<String>();
                for (String roleName : systemEntry.getValue()) {
                    for (String held : snapshot.effectiveRoles(roleName)) {
                        if (!effective.contains(held)) {
                            effective.add(held);
                        }
                    }
                }
                for (SodRule rule : snapshot.sodRules()) {
                    if (effective.contains(rule.getRoleA())
                            && effective.contains(rule.getRoleB())) {
                        conflicts.add(rule);
                    }
                }
                exposure.setConflicts(conflicts);
                exposures.add(exposure);
            }
        }
        Collections.sort(exposures, new Comparator<UserExposure>() {
            @Override
            public int compare(UserExposure left, UserExposure right) {
                int bySensitive = right.getSensitiveObjects().size()
                        - left.getSensitiveObjects().size();
                if (bySensitive != 0) {
                    return bySensitive;
                }
                int byConflicts = right.getConflicts().size() - left.getConflicts().size();
                if (byConflicts != 0) {
                    return byConflicts;
                }
                return left.getUserId().compareTo(right.getUserId());
            }
        });
        return exposures;
    }

    /** The segregation-of-duties board: every rule and who breaks it. */
    public Map<SodRule, Map<String, List<String>>> conflictBoard() {
        AccessSnapshot snapshot = AccessSnapshot.load(repository);
        Map<SodRule, Map<String, List<String>>> board =
                new LinkedHashMap<SodRule, Map<String, List<String>>>();
        for (SodRule rule : snapshot.sodRules()) {
            board.put(rule, snapshot.conflictingUsers(rule));
        }
        return board;
    }

    /** Every finding across the landscape, most severe first. */
    public List<AuthorizationFinding> allFindings() {
        List<AuthorizationFinding> findings = new ArrayList<AuthorizationFinding>();
        for (RoleAssessment assessment : assessments()) {
            findings.addAll(assessment.getFindings());
        }
        Collections.sort(findings, new Comparator<AuthorizationFinding>() {
            @Override
            public int compare(AuthorizationFinding left, AuthorizationFinding right) {
                int bySeverity = left.getSeverity().rank() - right.getSeverity().rank();
                return bySeverity != 0 ? bySeverity
                        : left.getSubject().compareTo(right.getSubject());
            }
        });
        return findings;
    }

    /** How many roles are in each severity band. */
    public int countBySeverity(Severity severity) {
        int count = 0;
        for (RoleAssessment assessment : assessments()) {
            if (assessment.getSeverity() == severity) {
                count++;
            }
        }
        return count;
    }

    /** The average governance score across the landscape. */
    public int averageScore() {
        List<RoleAssessment> all = assessments();
        if (all.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (RoleAssessment assessment : all) {
            total += assessment.getScore();
        }
        return Math.round((float) total / all.size());
    }

    /** The production system - the one the counts are about. */
    public String targetSystemId() {
        AccessSystem target = AccessSnapshot.load(repository).targetSystem();
        return target == null ? "" : target.getSystemId();
    }

    /**
     * How many users in production can reach a sensitive authorisation.
     *
     * <p>Counted in the target system, because a developer with table
     * maintenance in development is doing their job; the same person in
     * production is an incident waiting to happen.</p>
     */
    public int exposedUserCount() {
        String target = targetSystemId();
        Set<String> users = new HashSet<String>();
        for (UserExposure exposure : exposures()) {
            if (exposure.hasSensitiveAccess() && exposure.getSystemId().equals(target)) {
                users.add(exposure.getUserId());
            }
        }
        return users.size();
    }

    /** How many users in production are in a segregation-of-duties conflict. */
    public int conflictingUserCount() {
        String target = targetSystemId();
        Set<String> users = new HashSet<String>();
        for (UserExposure exposure : exposures()) {
            if (!exposure.getConflicts().isEmpty() && exposure.getSystemId().equals(target)) {
                users.add(exposure.getUserId());
            }
        }
        return users.size();
    }

    private static String join(java.util.Collection<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(value);
        }
        return out.toString();
    }

    /** The access governance report, as a Markdown document. */
    public String renderReport() {
        List<RoleAssessment> assessments = assessments();
        StringBuilder out = new StringBuilder();
        out.append("# Access governance report\n\n");
        out.append("Generated by SAP Authorisation & Access Governance Analyzer.\n\n");

        out.append("## Summary\n\n");
        out.append("| Role | Type | Owner | Status | Score | Severity | Findings |\n");
        out.append("|---|---|---|---|---|---|---|\n");
        for (RoleAssessment assessment : assessments) {
            RoleProfile role = assessment.getRole();
            out.append("| ").append(role.getRoleName())
                    .append(" | ").append(role.getRoleType())
                    .append(" | ").append(role.getOwner())
                    .append(" | ").append(role.getStatus())
                    .append(" | ").append(assessment.getScore())
                    .append(" | ").append(assessment.getSeverity())
                    .append(" | ").append(assessment.findingCount())
                    .append(" |\n");
        }
        out.append("\n");

        out.append("## Findings\n\n");
        for (RoleAssessment assessment : assessments) {
            if (assessment.getFindings().isEmpty()) {
                continue;
            }
            RoleProfile role = assessment.getRole();
            out.append("### ").append(role.getRoleName())
                    .append(" - ").append(role.getDescription()).append("\n\n");
            for (AuthorizationFinding finding : assessment.getFindings()) {
                out.append("- **").append(finding.getSeverity()).append(" - ")
                        .append(finding.getCategory()).append("**: ")
                        .append(finding.getMessage()).append("  \n  ")
                        .append(finding.getRecommendation()).append("\n");
            }
            out.append("\n");
        }

        out.append("## Who can reach what\n\n");
        out.append("| User | System | Roles | Atomic roles | Sensitive objects | Conflicts |\n");
        out.append("|---|---|---|---|---|---|\n");
        for (UserExposure exposure : exposures()) {
            out.append("| ").append(exposure.getUserId())
                    .append(" | ").append(exposure.getSystemId())
                    .append(" | ").append(exposure.getRoleList())
                    .append(" | ").append(exposure.getAtomicRoles().size())
                    .append(" | ").append(exposure.getSensitiveObjects().isEmpty()
                            ? "-" : join(exposure.getSensitiveObjects()))
                    .append(" | ").append(exposure.getConflicts().isEmpty()
                            ? "-" : String.valueOf(exposure.getConflicts().size()))
                    .append(" |\n");
        }
        out.append("\n");

        out.append("## Segregation of duties\n\n");
        out.append("| Rule | Role A | Role B | Severity | Who breaks it |\n");
        out.append("|---|---|---|---|---|\n");
        for (Map.Entry<SodRule, Map<String, List<String>>> entry
                : conflictBoard().entrySet()) {
            SodRule rule = entry.getKey();
            out.append("| ").append(rule.getRuleId())
                    .append(" | ").append(rule.getRoleA())
                    .append(" | ").append(rule.getRoleB())
                    .append(" | ").append(rule.getSeverity())
                    .append(" | ").append(entry.getValue().isEmpty()
                            ? "nobody"
                            : join(entry.getValue().keySet()))
                    .append(" |\n");
        }
        out.append("\n");

        out.append("---\n\n");
        out.append("> A role name is not an authorisation. The authorisation is the whole "
                + "expansion, held by a person, in a system.\n");
        return out.toString();
    }
}
