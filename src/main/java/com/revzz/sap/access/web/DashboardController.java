package com.revzz.sap.access.web;

import com.revzz.sap.access.domain.AuthorizationFinding;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.Severity;
import com.revzz.sap.access.domain.UserExposure;
import com.revzz.sap.access.service.AccessGovernanceService;
import com.revzz.sap.access.service.AuditService;
import com.revzz.sap.access.support.AccessChartRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * The governance dashboard.
 *
 * <p>The first screen answers one question: who in production can reach
 * something they should not, and which roles are quietly drifting apart
 * between the systems.</p>
 */
@Controller
public class DashboardController {

    private final AccessGovernanceService governanceService;
    private final AuditService auditService;

    public DashboardController(AccessGovernanceService governanceService,
                               AuditService auditService) {
        this.governanceService = governanceService;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<RoleAssessment> assessments = governanceService.assessments();
        List<AuthorizationFinding> findings = governanceService.allFindings();
        List<UserExposure> exposures = governanceService.exposures();

        model.addAttribute("assessments", assessments);
        model.addAttribute("findings", findings);
        model.addAttribute("exposures", exposures);
        model.addAttribute("scoreChart", AccessChartRenderer.governanceBars(assessments));
        model.addAttribute("exposureChart", AccessChartRenderer.exposureMap(exposures));
        model.addAttribute("roleCount", assessments.size());
        model.addAttribute("criticalCount", governanceService.countBySeverity(Severity.CRITICAL));
        model.addAttribute("highCount", governanceService.countBySeverity(Severity.HIGH));
        model.addAttribute("exposedUserCount", governanceService.exposedUserCount());
        model.addAttribute("conflictingUserCount", governanceService.conflictingUserCount());
        model.addAttribute("averageScore", governanceService.averageScore());
        model.addAttribute("findingCount", findings.size());
        model.addAttribute("targetSystem", governanceService.targetSystemId());
        model.addAttribute("audit", auditService.recent(8));
        return "overview";
    }
}
