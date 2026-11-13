package com.revzz.sap.access.web;

import com.revzz.sap.access.service.AccessGovernanceService;
import com.revzz.sap.access.service.AuditService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * The access governance report.
 *
 * <p>The deliverable: a document that can be attached to an access review,
 * pasted into a risk register or read out in a change board.</p>
 */
@Controller
@RequireRole({"ANALYST", "SECURITY_ADMIN"})
public class ReportController {

    private final AccessGovernanceService governanceService;
    private final AuditService auditService;

    public ReportController(AccessGovernanceService governanceService,
                            AuditService auditService) {
        this.governanceService = governanceService;
        this.auditService = auditService;
    }

    @GetMapping("/report")
    public String report(Model model, HttpServletRequest request) {
        String markdown = governanceService.renderReport();
        model.addAttribute("markdown", markdown);
        model.addAttribute("lineCount", markdown.split("\n").length);

        auditService.record(CurrentUser.username(request.getSession(false)), "REPORT_VIEWED",
                "Access governance report opened");
        return "report";
    }

    /** The same report as a downloadable Markdown document. */
    @GetMapping("/report.md")
    public ResponseEntity<String> download(HttpServletRequest request) {
        String markdown = governanceService.renderReport();

        auditService.record(CurrentUser.username(request.getSession(false)), "REPORT_EXPORTED",
                "Access governance report exported as Markdown");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "markdown", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "access-governance-report.md");
        return new ResponseEntity<String>(markdown, headers, HttpStatus.OK);
    }
}
