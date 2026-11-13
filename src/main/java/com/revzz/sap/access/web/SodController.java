package com.revzz.sap.access.web;

import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserExposure;
import com.revzz.sap.access.service.AccessGovernanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * The segregation-of-duties board.
 *
 * <p>Every rule, and who breaks it. A rule that nobody breaks is a rule
 * that works; a rule with three people on it is a rule that is not.</p>
 */
@Controller
@RequireRole({"ANALYST", "SECURITY_ADMIN"})
public class SodController {

    private final AccessGovernanceService governanceService;

    public SodController(AccessGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/sod")
    public String sod(Model model) {
        Map<SodRule, Map<String, List<String>>> board = governanceService.conflictBoard();
        List<UserExposure> exposures = governanceService.exposures();

        int broken = 0;
        int users = 0;
        for (Map.Entry<SodRule, Map<String, List<String>>> entry : board.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                broken++;
                users += entry.getValue().size();
            }
        }

        model.addAttribute("board", board);
        model.addAttribute("exposures", exposures);
        model.addAttribute("ruleCount", board.size());
        model.addAttribute("brokenCount", broken);
        model.addAttribute("conflictUserCount", users);
        model.addAttribute("targetSystem", governanceService.targetSystemId());
        return "sod";
    }
}
