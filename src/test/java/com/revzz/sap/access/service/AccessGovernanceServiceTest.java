package com.revzz.sap.access.service;

import com.revzz.sap.access.config.AppProperties;
import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.AuthObject;
import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.AuthorizationFinding;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.RoleGrant;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.Severity;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.revzz.sap.access.repo.AuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The access rules, tested one at a time.
 *
 * <p>Each test builds exactly one situation, so that a failure says which
 * rule broke rather than "the score changed".</p>
 */
class AccessGovernanceServiceTest {

    private AuthorizationRepository repository;
    private final List<AccessSystem> systems = new ArrayList<AccessSystem>();
    private final List<RoleProfile> roles = new ArrayList<RoleProfile>();
    private final List<AuthObject> objects = new ArrayList<AuthObject>();
    private final List<AuthValue> values = new ArrayList<AuthValue>();
    private final List<RoleGrant> grants = new ArrayList<RoleGrant>();
    private final List<UserRoleAssignment> assignments = new ArrayList<UserRoleAssignment>();
    private final List<SodRule> sodRules = new ArrayList<SodRule>();

    @BeforeEach
    void setUp() {
        repository = mock(AuthorizationRepository.class);
        when(repository.findSystems()).thenReturn(systems);
        when(repository.findRoles()).thenReturn(roles);
        when(repository.findObjects()).thenReturn(objects);
        when(repository.findValues()).thenReturn(values);
        when(repository.findGrants()).thenReturn(grants);
        when(repository.findAssignments()).thenReturn(assignments);
        when(repository.findSodRules()).thenReturn(sodRules);

        systems.add(system("DEV", false));
        systems.add(system("PRD", true));
    }

    // -- the rules -----------------------------------------------------------

    @Test
    @DisplayName("a role that grants a critical object is a critical exposure")
    void criticalObjectIsACriticalExposure() {
        RoleProfile role = role("ZFI_TABLE_ADMIN", RoleProfile.TYPE_SINGLE, "");
        object("F_OB01", "Direct table maintenance", true,
                AuthObject.SENSITIVITY_CRITICAL);
        value("V06", "F_OB01", "F_OB01", "ACTVT", "*");
        grant("ZFI_TABLE_ADMIN", "PRD", "V06");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("SENSITIVE_EXPOSURE"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("a role that grants a merely high object is a high exposure")
    void highObjectIsAHighExposure() {
        RoleProfile role = role("ZABAP_DEV", RoleProfile.TYPE_SINGLE, "");
        object("S_DEVELOP", "ABAP development objects", true,
                AuthObject.SENSITIVITY_HIGH);
        value("V07", "S_DEVELOP", "S_DEVELOP", "ACTVT", "*");
        grant("ZABAP_DEV", "PRD", "V07");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("SENSITIVE_EXPOSURE"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
    }

    @Test
    @DisplayName("a role that grants nothing sensitive is not an exposure")
    void normalObjectIsNotAnExposure() {
        RoleProfile role = role("ZFI_GL_POSTER", RoleProfile.TYPE_SINGLE, "");
        object("F_BKPF_BUK", "Posting to financial documents", false,
                AuthObject.SENSITIVITY_NORMAL);
        value("V01", "F_BKPF_BUK", "F_BKPF_BUK", "ACTVT", "*");
        grant("ZFI_GL_POSTER", "PRD", "V01");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertFalse(assessment.hasFinding("SENSITIVE_EXPOSURE"));
    }

    @Test
    @DisplayName("two people holding both sides of a rule is a conflict")
    void holdingBothSidesIsAConflict() {
        RoleProfile role = role("ZHR_ADMIN", RoleProfile.TYPE_SINGLE, "");
        role("ZHR_PAYROLL", RoleProfile.TYPE_SINGLE, "");
        sodRules.add(rule("SD01", "ZHR_ADMIN", "ZHR_PAYROLL", "CRITICAL"));
        assign("PRD", "ANIL.HR", "ZHR_ADMIN");
        assign("PRD", "ANIL.HR", "ZHR_PAYROLL");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("SO_D_CONFLICT"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertTrue(finding(assessment, "SO_D_CONFLICT").getMessage().contains("ANIL.HR"));
    }

    @Test
    @DisplayName("a composite that holds both sides of a rule bundles the conflict")
    void compositeBundlesTheConflict() {
        RoleProfile composite = role("ZABAP_ADMIN", RoleProfile.TYPE_COMPOSITE, "");
        RoleProfile dev = role("ZABAP_DEV", RoleProfile.TYPE_SINGLE, "ZABAP_ADMIN");
        RoleProfile table = role("ZFI_TABLE_ADMIN", RoleProfile.TYPE_SINGLE, "ZABAP_ADMIN");
        roles.add(dev);
        roles.add(table);
        sodRules.add(rule("SD02", "ZABAP_DEV", "ZFI_TABLE_ADMIN", "CRITICAL"));

        RoleAssessment assessment = service().assess(composite, snapshot());

        assertTrue(assessment.hasFinding("CONFLICT_BUNDLED_IN_COMPOSITE"));
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("a composite that expands too far is a finding")
    void wideCompositeIsAFinding() {
        RoleProfile composite = role("ZALL_INTEGRATION", RoleProfile.TYPE_COMPOSITE, "");
        for (int i = 0; i < 7; i++) {
            RoleProfile member = role("ZROLE" + i, RoleProfile.TYPE_SINGLE,
                    "ZALL_INTEGRATION");
            roles.add(member);
        }

        RoleAssessment assessment = service().assess(composite, snapshot());

        assertTrue(assessment.hasFinding("COMPOSITE_EXPANSION"));
        assertEquals(Severity.MEDIUM, assessment.getSeverity());
    }

    @Test
    @DisplayName("the same role with different values in two systems is drift")
    void differentValuesInTwoSystemsIsDrift() {
        RoleProfile role = role("ZFI_GL_POSTER", RoleProfile.TYPE_SINGLE, "");
        object("F_BKPF_BUKR", "Company codes in postings", false,
                AuthObject.SENSITIVITY_NORMAL);
        value("V02", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "1000");
        value("V03", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "2000");
        grant("ZFI_GL_POSTER", "PRD", "V02");
        grant("ZFI_GL_POSTER", "DEV", "V02", "V03");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("AUTHORIZATION_DRIFT"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
        assertEquals(1, assessment.getMissingInTarget().size());
        assertEquals("2000", assessment.getMissingInTarget().get(0).getFieldValue());
    }

    @Test
    @DisplayName("the same role with the same values everywhere is not drift")
    void sameValuesEverywhereIsNotDrift() {
        RoleProfile role = role("ZFI_GL_POSTER", RoleProfile.TYPE_SINGLE, "");
        object("F_BKPF_BUKR", "Company codes in postings", false,
                AuthObject.SENSITIVITY_NORMAL);
        value("V02", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "1000");
        grant("ZFI_GL_POSTER", "PRD", "V02");
        grant("ZFI_GL_POSTER", "DEV", "V02");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertFalse(assessment.hasFinding("AUTHORIZATION_DRIFT"));
        assertTrue(assessment.getDriftSystems().isEmpty());
    }

    @Test
    @DisplayName("an inactive role that is still assigned is a finding")
    void inactiveRoleStillAssignedIsAFinding() {
        RoleProfile role = role("ZHR_PAYROLL", RoleProfile.TYPE_SINGLE, "");
        role.setStatus(RoleProfile.STATUS_INACTIVE);
        assign("PRD", "ANIL.HR", "ZHR_PAYROLL");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("INACTIVE_ROLE_ASSIGNED"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
    }

    @Test
    @DisplayName("an active role nobody holds is a finding")
    void activeRoleNobodyHoldsIsAFinding() {
        RoleProfile role = role("ZMM_PURCHASER", RoleProfile.TYPE_SINGLE, "");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("ORPHAN_ROLE"));
        assertEquals(Severity.MEDIUM, assessment.getSeverity());
    }

    @Test
    @DisplayName("a less severe conflict rule still costs something")
    void aHighSeverityRuleStillCostsSomething() {
        RoleProfile role = role("ZABAP_ADMIN", RoleProfile.TYPE_SINGLE, "");
        sodRules.add(rule("SD03", "ZABAP_ADMIN", "ZHR_ADMIN", "HIGH"));
        assign("PRD", "DEV.TWO", "ZABAP_ADMIN");
        assign("PRD", "DEV.TWO", "ZHR_ADMIN");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.hasFinding("SO_D_CONFLICT"));
        assertEquals(Severity.HIGH, assessment.getSeverity());
        assertTrue(assessment.getScore() < 100,
                "a finding that costs nothing is a finding nobody has to act on");
    }

    @Test
    @DisplayName("a role with nothing wrong produces no findings at all")
    void cleanRoleHasNoFindings() {
        RoleProfile role = role("ZFI_READONLY", RoleProfile.TYPE_SINGLE, "");
        object("F_BKPF_BUK", "Posting to financial documents", false,
                AuthObject.SENSITIVITY_NORMAL);
        value("V11", "F_BKPF_BUK", "F_BKPF_BUK", "ACTVT", "03");
        grant("ZFI_READONLY", "PRD", "V11");
        grant("ZFI_READONLY", "DEV", "V11");
        assign("PRD", "HANA.SVC", "ZFI_READONLY");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertTrue(assessment.getFindings().isEmpty(),
                "expected no findings but got " + assessment.getFindings());
        assertEquals(100, assessment.getScore());
        assertEquals(Severity.INFO, assessment.getSeverity());
    }

    @Test
    @DisplayName("the severity is the worst finding, not the score band")
    void severityIsTheWorstFindingNotTheScoreBand() {
        // A critical exposure costs 35 points, so the score is 65 - and 65
        // sits in the "medium" band. The severity has to say "critical".
        RoleProfile role = role("ZFI_TABLE_ADMIN", RoleProfile.TYPE_SINGLE, "");
        object("F_OB01", "Direct table maintenance", true,
                AuthObject.SENSITIVITY_CRITICAL);
        value("V06", "F_OB01", "F_OB01", "ACTVT", "*");
        grant("ZFI_TABLE_ADMIN", "PRD", "V06");
        assign("PRD", "BASIS.LEAD", "ZFI_TABLE_ADMIN");

        RoleAssessment assessment = service().assess(role, snapshot());

        assertEquals(65, assessment.getScore());
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
    }

    @Test
    @DisplayName("a composite role grants what the roles underneath it grant")
    void compositeGrantsWhatIsUnderneath() {
        RoleProfile composite = role("ZABAP_ADMIN", RoleProfile.TYPE_COMPOSITE, "");
        RoleProfile dev = role("ZABAP_DEV", RoleProfile.TYPE_SINGLE, "ZABAP_ADMIN");
        roles.add(dev);
        object("S_DEVELOP", "ABAP development objects", true,
                AuthObject.SENSITIVITY_HIGH);
        value("V07", "S_DEVELOP", "S_DEVELOP", "ACTVT", "*");
        grant("ZABAP_DEV", "PRD", "V07");
        assign("PRD", "DEVADMIN", "ZABAP_ADMIN");

        AccessSnapshot snapshot = snapshot();
        assertEquals(1, snapshot.atomicRoles("ZABAP_ADMIN").size());
        assertEquals("ZABAP_DEV", snapshot.atomicRoles("ZABAP_ADMIN").get(0));
    }

    @Test
    @DisplayName("a person holding a composite reaches what is underneath it")
    void compositeHoldersReachWhatIsUnderneath() {
        RoleProfile composite = role("ZABAP_ADMIN", RoleProfile.TYPE_COMPOSITE, "");
        RoleProfile table = role("ZFI_TABLE_ADMIN", RoleProfile.TYPE_SINGLE, "ZABAP_ADMIN");
        roles.add(table);
        object("F_OB01", "Direct table maintenance", true,
                AuthObject.SENSITIVITY_CRITICAL);
        value("V06", "F_OB01", "F_OB01", "ACTVT", "*");
        grant("ZFI_TABLE_ADMIN", "PRD", "V06");
        assign("PRD", "DEVADMIN", "ZABAP_ADMIN");

        AccessGovernanceService service = service();
        boolean reached = false;
        boolean conflict = false;
        for (com.revzz.sap.access.domain.UserExposure exposure : service.exposures()) {
            if ("DEVADMIN".equals(exposure.getUserId())) {
                reached = exposure.getSensitiveObjects().contains("F_OB01");
                conflict = !exposure.getConflicts().isEmpty();
            }
        }
        assertTrue(reached, "the composite must hand the person the object underneath");
        assertFalse(conflict, "there is no rule in this scenario");
    }

    // -- helpers -------------------------------------------------------------

    private AccessGovernanceService service() {
        return new AccessGovernanceService(repository, new AppProperties());
    }

    private AccessSnapshot snapshot() {
        return AccessSnapshot.load(repository);
    }

    private AccessSystem system(String id, boolean target) {
        AccessSystem system = new AccessSystem();
        system.setSystemId(id);
        system.setDescription("System " + id);
        system.setSystemRole(target ? AccessSystem.ROLE_PRODUCTION
                : AccessSystem.ROLE_DEVELOPMENT);
        system.setTarget(target);
        return system;
    }

    private RoleProfile role(String name, String type, String parent) {
        RoleProfile role = new RoleProfile();
        role.setRoleName(name);
        role.setDescription("Role " + name);
        role.setRoleType(type);
        role.setParentRole(parent);
        role.setOwner("BASIS.LEAD");
        role.setStatus(RoleProfile.STATUS_ACTIVE);
        roles.add(role);
        return role;
    }

    private AuthObject object(String name, String description, boolean sensitive,
                              String sensitivity) {
        AuthObject object = new AuthObject();
        object.setObjectName(name);
        object.setDescription(description);
        object.setSensitive(sensitive);
        object.setSensitivity(sensitivity);
        objects.add(object);
        return object;
    }

    private AuthValue value(String id, String objectName, String authName, String fieldName,
                             String fieldValue) {
        AuthValue value = new AuthValue();
        value.setValueId(id);
        value.setObjectName(objectName);
        value.setAuthName(authName);
        value.setFieldName(fieldName);
        value.setFieldValue(fieldValue);
        values.add(value);
        return value;
    }

    private void grant(String roleName, String systemId, String... valueIds) {
        for (String valueId : valueIds) {
            RoleGrant grant = new RoleGrant();
            grant.setSystemId(systemId);
            grant.setRoleName(roleName);
            grant.setValueId(valueId);
            grants.add(grant);
        }
    }

    private void assign(String systemId, String userId, String roleName) {
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setSystemId(systemId);
        assignment.setUserId(userId);
        assignment.setRoleName(roleName);
        assignments.add(assignment);
    }

    private SodRule rule(String id, String roleA, String roleB, String severity) {
        SodRule rule = new SodRule();
        rule.setRuleId(id);
        rule.setRoleA(roleA);
        rule.setRoleB(roleB);
        rule.setSeverity(severity);
        rule.setDescription("Rule " + id);
        sodRules.add(rule);
        return rule;
    }

    private AuthorizationFinding finding(RoleAssessment assessment, String category) {
        for (AuthorizationFinding finding : assessment.getFindings()) {
            if (category.equals(finding.getCategory())) {
                return finding;
            }
        }
        throw new AssertionError("no finding with category " + category);
    }
}
