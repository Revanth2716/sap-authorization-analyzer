package com.revzz.sap.access.web;

import com.revzz.sap.access.domain.AuthorizationFinding;
import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserExposure;
import com.revzz.sap.access.service.AccessGovernanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for reporting and integrations.
 *
 * <p>Read-only on purpose: the API exposes the same analysis the screens
 * show, so a monitoring script or a ticket system can consume it without
 * scraping HTML.</p>
 *
 * <p>Every endpoint except the health check needs a signed-in session. The
 * data describes who can reach what in production, so it is not something to
 * serve to anyone who asks.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final AccessGovernanceService governanceService;

    public ApiController(AccessGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    /** The health check stays open - a load balancer has to be able to reach it. */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<String, Object>();
        health.put("status", "UP");
        health.put("roles", governanceService.roles().size());
        health.put("exposedUsers", governanceService.exposedUserCount());
        health.put("conflictingUsers", governanceService.conflictingUserCount());
        health.put("averageScore", governanceService.averageScore());
        return health;
    }

    @GetMapping(value = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> roles(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(governanceService.assessments());
    }

    @GetMapping(value = "/roles/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> role(@PathVariable String name, HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        RoleAssessment assessment = governanceService.assess(name);
        if (assessment == null) {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("error", "no such role");
            body.put("role", name);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
        return ResponseEntity.ok(assessment);
    }

    @GetMapping(value = "/findings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findings(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(governanceService.allFindings());
    }

    @GetMapping(value = "/exposures", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> exposures(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(governanceService.exposures());
    }

    @GetMapping(value = "/sod", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> sod(HttpSession session) {
        ResponseEntity<Object> denied = requireSession(session);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("targetSystem", governanceService.targetSystemId());
        Map<SodRule, Map<String, List<String>>> board = governanceService.conflictBoard();
        body.put("rules", board.keySet());
        body.put("conflicts", board);
        return ResponseEntity.ok(body);
    }

    /**
     * Rejects an anonymous caller.
     *
     * @return null when the caller is signed in, otherwise a 401 with a JSON body
     */
    private ResponseEntity<Object> requireSession(HttpSession session) {
        if (CurrentUser.isSignedIn(session)) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", "authentication required");
        body.put("hint", "sign in at /login and send the session cookie");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
