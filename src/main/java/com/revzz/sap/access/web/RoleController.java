package com.revzz.sap.access.web;

import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.repo.AuthorizationRepository;
import com.revzz.sap.access.service.AccessGovernanceService;
import com.revzz.sap.access.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One role, in detail.
 *
 * <p>The detail screen is where a security team actually works: the
 * authorisation values the role holds in each system, the atomic roles it
 * expands to, who holds it, and the drift between the systems. It is also
 * the only place a role assignment can be revoked - taking access away is
 * the write path of this application.</p>
 */
@Controller
public class RoleController {

    private final AccessGovernanceService governanceService;
    private final AuthorizationRepository repository;
    private final AuditService auditService;

    public RoleController(AccessGovernanceService governanceService,
                          AuthorizationRepository repository,
                          AuditService auditService) {
        this.governanceService = governanceService;
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping("/roles/{name}")
    public String role(@PathVariable String name, Model model) {
        RoleAssessment assessment = governanceService.assess(name);
        if (assessment == null) {
            model.addAttribute("error", "There is no role called " + name + ".");
            return "error";
        }

        List<String> systemIds = new ArrayList<String>();
        for (String systemId : assessment.getValuesBySystem().keySet()) {
            systemIds.add(systemId);
        }
        Collections.sort(systemIds);

        model.addAttribute("assessment", assessment);
        model.addAttribute("role", assessment.getRole());
        model.addAttribute("systemIds", systemIds);
        model.addAttribute("targetSystem", governanceService.targetSystemId());
        return "role";
    }

    /**
     * Revokes a role assignment.
     *
     * <p>Taking access away is the action that matters, and the one a
     * security review will ask about afterwards. It is written to the audit
     * trail.</p>
     */
    @PostMapping("/roles/{name}/revoke")
    @RequireRole("SECURITY_ADMIN")
    public String revoke(@PathVariable String name,
                         @RequestParam String systemId,
                         @RequestParam String userId,
                         Model model,
                         HttpServletRequest request) {
        if (repository.findRole(name) == null) {
            model.addAttribute("error", "There is no role called " + name + ".");
            return "error";
        }

        repository.deleteAssignment(systemId, userId, name);
        auditService.record(CurrentUser.username(request.getSession(false)),
                "ROLE_REVOKED",
                name + " revoked from " + userId + " in " + systemId);
        return "redirect:/roles/" + name + "?revoked=1";
    }
}
