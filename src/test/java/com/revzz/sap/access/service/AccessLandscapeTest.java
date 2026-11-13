package com.revzz.sap.access.service;

import com.revzz.sap.access.domain.AuthorizationFinding;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.Severity;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserExposure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seeded access landscape.
 *
 * <p>These tests check the story the demo data has to tell. If the seeding
 * changes and the story changes with it, these tests are the ones that say
 * so first.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AccessLandscapeTest {

    @Autowired
    private AccessGovernanceService governanceService;

    @Test
    @DisplayName("the landscape holds nine roles")
    void nineRoles() {
        assertEquals(9, governanceService.roles().size());
        assertEquals(9, governanceService.assessments().size());
    }

    @Test
    @DisplayName("the table maintenance role is critical and nobody holds it directly")
    void tableMaintenanceIsCritical() {
        RoleAssessment assessment = governanceService.assess("ZFI_TABLE_ADMIN");

        assertNotNull(assessment);
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertTrue(assessment.hasFinding("SENSITIVE_EXPOSURE"));
        assertEquals(1, assessment.getSensitiveValues().size());
    }

    @Test
    @DisplayName("the ABAP administration composite bundles a segregation conflict")
    void abapAdminBundlesAConflict() {
        RoleAssessment assessment = governanceService.assess("ZABAP_ADMIN");

        assertNotNull(assessment);
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertTrue(assessment.hasFinding("CONFLICT_BUNDLED_IN_COMPOSITE"));
        assertTrue(assessment.hasFinding("SO_D_CONFLICT"));
        assertEquals(2, assessment.getAtomicRoles().size());
    }

    @Test
    @DisplayName("the payroll role is critical - inactive, still assigned and in a conflict")
    void payrollIsCritical() {
        RoleAssessment assessment = governanceService.assess("ZHR_PAYROLL");

        assertNotNull(assessment);
        assertEquals(Severity.CRITICAL, assessment.getSeverity());
        assertTrue(assessment.hasFinding("INACTIVE_ROLE_ASSIGNED"));
        assertTrue(assessment.hasFinding("SO_D_CONFLICT"));
        assertTrue(assessment.hasFinding("SENSITIVE_EXPOSURE"));
        assertEquals(5, assessment.getScore());
    }

    @Test
    @DisplayName("the financial posting role has drifted between development and production")
    void postingRoleHasDrifted() {
        RoleAssessment assessment = governanceService.assess("ZFI_GL_POSTER");

        assertNotNull(assessment);
        assertEquals(Severity.HIGH, assessment.getSeverity());
        assertTrue(assessment.hasFinding("AUTHORIZATION_DRIFT"));
        assertEquals(1, assessment.getMissingInTarget().size());
        assertEquals("4000", assessment.getMissingInTarget().get(0).getFieldValue());
        assertEquals(1, assessment.getDriftSystems().size());
        assertEquals("DEV", assessment.getDriftSystems().get(0));
    }

    @Test
    @DisplayName("the purchasing role is orphaned - nobody holds it")
    void purchasingIsOrphaned() {
        RoleAssessment assessment = governanceService.assess("ZMM_PURCHASER");

        assertNotNull(assessment);
        assertTrue(assessment.hasFinding("ORPHAN_ROLE"));
        assertTrue(assessment.getUsers().isEmpty());
    }

    @Test
    @DisplayName("the read-only role is clean - the control")
    void readOnlyIsClean() {
        RoleAssessment assessment = governanceService.assess("ZFI_READONLY");

        assertNotNull(assessment);
        assertEquals(100, assessment.getScore());
        assertEquals(Severity.INFO, assessment.getSeverity());
        assertTrue(assessment.getFindings().isEmpty(),
                "ZFI_READONLY should have no findings but had " + assessment.getFindings());
    }

    @Test
    @DisplayName("the wide composite expands to six atomic roles and bundles three conflicts")
    void wideCompositeExpandsTooFar() {
        RoleAssessment assessment = governanceService.assess("ZALL_INTEGRATION");

        assertNotNull(assessment);
        assertTrue(assessment.hasFinding("COMPOSITE_EXPANSION"));
        assertTrue(assessment.hasFinding("CONFLICT_BUNDLED_IN_COMPOSITE"));
        assertEquals(6, assessment.getAtomicRoles().size());
    }

    @Test
    @DisplayName("three users in production can reach a sensitive object, two of them in conflict")
    void theBlastRadius() {
        assertEquals(3, governanceService.exposedUserCount());
        assertEquals(2, governanceService.conflictingUserCount());
    }

    @Test
    @DisplayName("the developer in production reaches the table maintenance object")
    void theDeveloperInProductionReachesTableMaintenance() {
        boolean found = false;
        for (UserExposure exposure : governanceService.exposures()) {
            if ("DEVADMIN".equals(exposure.getUserId()) && "PRD".equals(exposure.getSystemId())) {
                found = true;
                assertTrue(exposure.getSensitiveObjects().contains("F_OB01"),
                        "the composite must hand the person the object underneath");
                assertEquals(1, exposure.getConflicts().size());
            }
        }
        assertTrue(found, "DEVADMIN should have an exposure in production");
    }

    @Test
    @DisplayName("every rule on the board has somebody breaking it")
    void everyRuleHasSomebodyBreakingIt() {
        Map<SodRule, Map<String, List<String>>> board = governanceService.conflictBoard();

        assertEquals(3, board.size());
        for (Map.Entry<SodRule, Map<String, List<String>>> entry : board.entrySet()) {
            assertFalse(entry.getValue().isEmpty(),
                    "the demo data breaks " + entry.getKey().getRuleId());
        }
    }

    @Test
    @DisplayName("the counts add up to the landscape")
    void countsAddUp() {
        int counted = governanceService.countBySeverity(Severity.CRITICAL)
                + governanceService.countBySeverity(Severity.HIGH)
                + governanceService.countBySeverity(Severity.MEDIUM)
                + governanceService.countBySeverity(Severity.LOW)
                + governanceService.countBySeverity(Severity.INFO);

        assertEquals(governanceService.roles().size(), counted);
        assertEquals(6, governanceService.countBySeverity(Severity.CRITICAL));
        assertEquals(2, governanceService.countBySeverity(Severity.HIGH));
        assertEquals(1, governanceService.countBySeverity(Severity.INFO));
    }

    @Test
    @DisplayName("the landscape is sorted worst first")
    void sortedWorstFirst() {
        List<RoleAssessment> assessments = governanceService.assessments();

        assertEquals("ZHR_PAYROLL", assessments.get(0).getRole().getRoleName());
        assertEquals("ZFI_READONLY",
                assessments.get(assessments.size() - 1).getRole().getRoleName());
    }

    @Test
    @DisplayName("every finding carries a recommended action")
    void everyFindingHasAnAction() {
        for (AuthorizationFinding finding : governanceService.allFindings()) {
            assertNotNull(finding.getRecommendation());
            assertFalse(finding.getRecommendation().trim().isEmpty(),
                    "finding " + finding.getCategory() + " has no recommendation");
            assertFalse(finding.getMessage().trim().isEmpty());
        }
    }

    @Test
    @DisplayName("the report names every role, the conflicts and who reaches what")
    void reportCoversEverything() {
        String report = governanceService.renderReport();

        assertTrue(report.contains("# Access governance report"));
        for (String role : new String[]{"ZFI_TABLE_ADMIN", "ZABAP_ADMIN", "ZHR_PAYROLL",
                "ZFI_GL_POSTER", "ZALL_INTEGRATION"}) {
            assertTrue(report.contains(role), "the report does not mention " + role);
        }
        assertTrue(report.contains("## Who can reach what"));
        assertTrue(report.contains("## Segregation of duties"));
        assertTrue(report.contains("SD01"));
    }
}
