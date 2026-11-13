package com.revzz.sap.access.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders every screen.
 *
 * <p>A template can compile and the service layer can be fully tested while
 * one screen still returns HTTP 500, because a single expression in the
 * markup does not resolve. This test renders each page the way a browser
 * would, so that class of bug fails the build.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} renders")
    @ValueSource(strings = {
            "/",
            "/roles/ZFI_TABLE_ADMIN",
            "/roles/ZABAP_ADMIN",
            "/roles/ZHR_PAYROLL",
            "/roles/ZFI_GL_POSTER",
            "/roles/ZALL_INTEGRATION",
            "/sod",
            "/report",
    })
    @DisplayName("every screen renders for the security administrator")
    void everyScreenRenders(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Whitelabel Error Page"))))
                .andExpect(content().string(not(containsString("TemplateProcessingException"))));
    }

    @Test
    @DisplayName("the login screen renders")
    void loginRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in")));
    }

    @Test
    @DisplayName("the overview shows both charts and the findings")
    void overviewShowsTheCharts() throws Exception {
        mockMvc.perform(get("/").session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("WHO CAN REACH WHAT")))
                .andExpect(content().string(containsString("ZFI_TABLE_ADMIN")))
                .andExpect(content().string(containsString("SENSITIVE_EXPOSURE")));
    }

    @Test
    @DisplayName("the role screen shows the expansion, the drift and the holders")
    void roleScreenShowsTheExpansion() throws Exception {
        mockMvc.perform(get("/roles/ZABAP_ADMIN").session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ZABAP_DEV")))
                .andExpect(content().string(containsString("CONFLICT_BUNDLED_IN_COMPOSITE")));
    }

    @Test
    @DisplayName("the SoD board shows who breaks each rule")
    void sodBoardShowsTheConflicts() throws Exception {
        mockMvc.perform(get("/sod").session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SD01")))
                .andExpect(content().string(containsString("ANIL.HR")));
    }

    @Test
    @DisplayName("an unknown role shows the error page, not a stack trace")
    void unknownRoleShowsErrorPage() throws Exception {
        mockMvc.perform(get("/roles/ZNOPE").session(sessionFor("SECURITY_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Something went wrong")));
    }

    @ParameterizedTest(name = "the viewer cannot open {0}")
    @ValueSource(strings = {"/sod", "/report"})
    @DisplayName("the viewer role is blocked from the analysis screens")
    void viewerIsBlocked(String path) throws Exception {
        mockMvc.perform(get(path).session(sessionFor("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an anonymous visitor is sent to the login page")
    void anonymousIsSentToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String location = result.getResponse().getRedirectedUrl();
                    org.junit.jupiter.api.Assertions.assertNotNull(location);
                    org.junit.jupiter.api.Assertions.assertTrue(location.contains("/login"),
                            "expected a redirect to the login page but got " + location);
                });
    }

    @Test
    @DisplayName("the access analyst may read the board but may not revoke an assignment")
    void analystCannotRevoke() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/roles/ZFI_TABLE_ADMIN/revoke")
                .param("systemId", "PRD")
                .param("userId", "DEVADMIN")
                .session(sessionFor("ANALYST")))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession sessionFor(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.SESSION_ATTRIBUTE, role);
        session.setAttribute("DISPLAY_NAME", role);
        return session;
    }
}
