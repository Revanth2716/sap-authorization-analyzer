package com.revzz.sap.access.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The REST API.
 *
 * <p>Read-only, and authenticated - the data describes who can reach what
 * in production, so it is not served to anyone who asks.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the health check is open")
    void healthIsOpen() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")))
                .andExpect(jsonPath("$.roles").value(9))
                .andExpect(jsonPath("$.exposedUsers").value(3))
                .andExpect(jsonPath("$.conflictingUsers").value(2));
    }

    @Test
    @DisplayName("the role data needs a session")
    void rolesNeedASession() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("authentication required")));
    }

    @Test
    @DisplayName("the role data is served to a signed-in caller")
    void rolesAreServedToASignedInCaller() throws Exception {
        mockMvc.perform(get("/api/v1/roles").session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(content().string(containsString("ZFI_TABLE_ADMIN")))
                .andExpect(content().string(containsString("SENSITIVE_EXPOSURE")));
    }

    @Test
    @DisplayName("one role can be read on its own")
    void oneRoleCanBeRead() throws Exception {
        mockMvc.perform(get("/api/v1/roles/ZFI_READONLY").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"score\":100")))
                .andExpect(content().string(containsString("\"severity\":\"INFO\"")));
    }

    @Test
    @DisplayName("an unknown role is a 404 with a JSON body")
    void unknownRoleIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/roles/ZNOPE").session(sessionFor("ANALYST")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("no such role")));
    }

    @Test
    @DisplayName("the findings list is served")
    void findingsAreServed() throws Exception {
        mockMvc.perform(get("/api/v1/findings").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(content().string(containsString("recommendation")));
    }

    @Test
    @DisplayName("the exposures are served")
    void exposuresAreServed() throws Exception {
        mockMvc.perform(get("/api/v1/exposures").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DEVADMIN")))
                .andExpect(content().string(containsString("F_OB01")));
    }

    @Test
    @DisplayName("the SoD board is served")
    void sodBoardIsServed() throws Exception {
        mockMvc.perform(get("/api/v1/sod").session(sessionFor("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SD01")))
                .andExpect(content().string(containsString("ZHR_PAYROLL")));
    }

    @Test
    @DisplayName("signing in with the demo password works, and a wrong password does not")
    void signInWorks() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "security.admin")
                        .param("password", "demo1234"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/login")
                        .param("username", "security.admin")
                        .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/login?error=invalid"));
    }

    @Test
    @DisplayName("the API never returns a stack trace")
    void apiNeverLeaksAStackTrace() throws Exception {
        mockMvc.perform(get("/api/v1/roles/ZNOPE").session(sessionFor("ANALYST")))
                .andExpect(content().string(not(containsString("at com.revzz"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.SESSION_ATTRIBUTE, role);
        session.setAttribute("DISPLAY_NAME", role);
        return session;
    }
}
