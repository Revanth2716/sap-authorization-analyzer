package com.revzz.sap.access.seed;

import com.revzz.sap.access.config.AppProperties;
import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.AuthObject;
import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.RoleGrant;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.revzz.sap.access.repo.AuthorizationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds an access landscape with a story.
 *
 * <p>Nine roles, each with the problem a real customer has:</p>
 *
 * <ul>
 *   <li><b>ZFI_TABLE_ADMIN</b> - grants direct table maintenance, and
 *       everybody who holds the ABAP administration role holds it too.</li>
 *   <li><b>ZABAP_ADMIN</b> - a composite that <em>bundles</em> a
 *       segregation-of-duties conflict, so every holder has it whether they
 *       know it or not.</li>
 *   <li><b>ZALL_INTEGRATION</b> - a composite that expands to seven atomic
 *       roles and bundles three conflicts.</li>
 *   <li><b>ZHR_PAYROLL</b> - inactive, still assigned to somebody, and in
 *       a conflict with the HR administration role.</li>
 *   <li><b>ZFI_GL_POSTER</b> - the same role with a different set of
 *       company codes in development and production. The drift.</li>
 *   <li><b>ZMM_PURCHASER</b> - nobody holds it.</li>
 *   <li><b>ZFI_READONLY</b> - clean. The control.</li>
 * </ul>
 *
 * <p>Random noise would prove nothing. Every value here exists because it
 * demonstrates one specific finding.</p>
 */
@Component
@Order(2)
public class AccessSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final AuthorizationRepository repository;
    private final AppProperties properties;

    public AccessSeeder(JdbcTemplate jdbc, AuthorizationRepository repository,
                        AppProperties properties) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.isSeedDemoData() || repository.countRoles() > 0) {
            return;
        }
        seedUsers();

        // --- the landscape ----------------------------------------------------
        system("DEV", "Development system", AccessSystem.ROLE_DEVELOPMENT, false);
        system("QA", "Quality system", AccessSystem.ROLE_QUALITY, false);
        system("PRD", "Production ERP", AccessSystem.ROLE_PRODUCTION, true);

        // --- the authorisation objects -----------------------------------------
        object("F_BKPF_BUK", "Posting to financial documents", false,
                AuthObject.SENSITIVITY_NORMAL);
        object("F_BKPF_BUKR", "Company codes in postings", false,
                AuthObject.SENSITIVITY_NORMAL);
        object("F_OB01", "Direct table maintenance", true,
                AuthObject.SENSITIVITY_CRITICAL);
        object("S_TCODE", "Start transactions", true, AuthObject.SENSITIVITY_HIGH);
        object("S_DEVELOP", "ABAP development objects", true, AuthObject.SENSITIVITY_HIGH);
        object("F_CEP", "Payroll processing", true, AuthObject.SENSITIVITY_CRITICAL);
        object("F_LFA_CREATE", "Create vendor master", true, AuthObject.SENSITIVITY_HIGH);

        // --- the authorisation values ------------------------------------------
        value("V01", "F_BKPF_BUK", "F_BKPF_BUK", "ACTVT", "*");
        value("V02", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "1000");
        value("V03", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "2000");
        value("V04", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "3000");
        value("V05", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "4000");
        value("V06", "F_OB01", "F_OB01", "ACTVT", "*");
        value("V07", "S_DEVELOP", "S_DEVELOP", "ACTVT", "*");
        value("V08", "S_TCODE", "S_TCODE", "TCD", "SA38");
        value("V09", "F_CEP", "F_CEP", "ACTVT", "*");
        value("V10", "F_LFA_CREATE", "F_LFA_CREATE", "ACTVT", "01");
        value("V11", "F_BKPF_BUK", "F_BKPF_BUK", "ACTVT", "03");
        value("V12", "F_BKPF_BUKR", "F_BKPF_BUKR", "ACTVT", "03");
        value("V15", "F_CEP", "F_CEP", "ACTVT", "03");

        // --- the roles -----------------------------------------------------------
        role("ZFI_GL_POSTER", "Financial document posting", RoleProfile.TYPE_SINGLE,
                "", "BASIS.LEAD", RoleProfile.STATUS_ACTIVE);
        role("ZFI_TABLE_ADMIN", "Direct table maintenance", RoleProfile.TYPE_SINGLE,
                "", "BASIS.LEAD", RoleProfile.STATUS_ACTIVE);
        role("ZABAP_DEV", "ABAP development", RoleProfile.TYPE_SINGLE,
                "", "DEV.LEAD", RoleProfile.STATUS_ACTIVE);
        role("ZABAP_ADMIN", "ABAP administration", RoleProfile.TYPE_COMPOSITE,
                "", "BASIS.LEAD", RoleProfile.STATUS_ACTIVE);
        role("ZHR_ADMIN", "HR administration", RoleProfile.TYPE_SINGLE,
                "", "HR.OWNER", RoleProfile.STATUS_ACTIVE);
        role("ZHR_PAYROLL", "Payroll processing", RoleProfile.TYPE_SINGLE,
                "", "HR.OWNER", RoleProfile.STATUS_INACTIVE);
        role("ZMM_PURCHASER", "Purchasing", RoleProfile.TYPE_SINGLE,
                "", "PROC.OWNER", RoleProfile.STATUS_ACTIVE);
        role("ZFI_READONLY", "Financial display only", RoleProfile.TYPE_SINGLE,
                "", "BASIS.LEAD", RoleProfile.STATUS_ACTIVE);
        role("ZALL_INTEGRATION", "All integration roles", RoleProfile.TYPE_COMPOSITE,
                "", "DEV.LEAD", RoleProfile.STATUS_ACTIVE);

        // The members of the composites.
        member("ZABAP_DEV", "ZABAP_ADMIN");
        member("ZFI_TABLE_ADMIN", "ZABAP_ADMIN");
        member("ZABAP_ADMIN", "ZALL_INTEGRATION");
        member("ZFI_GL_POSTER", "ZALL_INTEGRATION");
        member("ZHR_ADMIN", "ZALL_INTEGRATION");
        member("ZHR_PAYROLL", "ZALL_INTEGRATION");
        member("ZFI_READONLY", "ZALL_INTEGRATION");

        // --- what each role grants ----------------------------------------------
        grantAll("ZFI_GL_POSTER", "V01", "V02", "V03", "V04");
        grant("ZFI_GL_POSTER", "DEV", "V05");
        grantAll("ZFI_TABLE_ADMIN", "V06");
        grantAll("ZABAP_DEV", "V07", "V08");
        grantAll("ZHR_ADMIN", "V15");
        grantAll("ZHR_PAYROLL", "V09");
        grantAll("ZMM_PURCHASER", "V10");
        grantAll("ZFI_READONLY", "V11", "V12");

        // --- who holds what -------------------------------------------------------
        assign("PRD", "DEVADMIN", "ZABAP_ADMIN");
        assign("PRD", "BASIS.LEAD", "ZFI_GL_POSTER");
        assign("PRD", "BASIS.LEAD", "ZABAP_DEV");
        assign("PRD", "ANIL.HR", "ZHR_ADMIN");
        assign("PRD", "ANIL.HR", "ZHR_PAYROLL");
        assign("PRD", "HANA.SVC", "ZFI_READONLY");

        assign("DEV", "DEVADMIN", "ZABAP_ADMIN");
        assign("DEV", "DEV.TWO", "ZALL_INTEGRATION");
        assign("DEV", "ANIL.HR", "ZHR_ADMIN");

        assign("QA", "QA.TESTER", "ZFI_GL_POSTER");
        assign("QA", "QA.TESTER", "ZFI_READONLY");

        // --- the segregation-of-duties rules ---------------------------------------
        sod("SD01", "ZHR_ADMIN", "ZHR_PAYROLL", "CRITICAL",
                "Payroll administration and payroll processing must not be combined");
        sod("SD02", "ZABAP_DEV", "ZFI_TABLE_ADMIN", "CRITICAL",
                "ABAP development together with direct table maintenance can change anything "
                        + "the application validates");
        sod("SD03", "ZABAP_ADMIN", "ZHR_ADMIN", "HIGH",
                "A development administrator should not also administer HR master data");
    }

    private void seedUsers() {
        Object[][] users = {
                {"DEVADMIN", "Development Administrator", "A", "IT-Basis"},
                {"BASIS.LEAD", "Basis Lead", "A", "IT-Basis"},
                {"ANIL.HR", "Anil HR", "A", "HR"},
                {"HANA.SVC", "HANA Service Account", "A", "IT-Basis"},
                {"DEV.TWO", "Developer Two", "A", "IT-Basis"},
                {"QA.TESTER", "QA Tester", "A", "IT-Basis"},
        };
        for (Object[] user : users) {
            jdbc.update("INSERT INTO SAPUSER (USERID, FULLNAME, UFLAG, DEPT) VALUES (?,?,?,?)",
                    user[0], user[1], user[2], user[3]);
        }
    }

    private void system(String id, String description, String role, boolean target) {
        AccessSystem system = new AccessSystem();
        system.setSystemId(id);
        system.setDescription(description);
        system.setSystemRole(role);
        system.setTarget(target);
        repository.insertSystem(system);
    }

    private void object(String name, String description, boolean sensitive, String sensitivity) {
        AuthObject object = new AuthObject();
        object.setObjectName(name);
        object.setDescription(description);
        object.setSensitive(sensitive);
        object.setSensitivity(sensitivity);
        repository.insertObject(object);
    }

    private void value(String id, String objectName, String authName, String fieldName,
                       String fieldValue) {
        AuthValue value = new AuthValue();
        value.setValueId(id);
        value.setObjectName(objectName);
        value.setAuthName(authName);
        value.setFieldName(fieldName);
        value.setFieldValue(fieldValue);
        repository.insertValue(value);
    }

    private void role(String name, String description, String type, String parent,
                      String owner, String status) {
        RoleProfile role = new RoleProfile();
        role.setRoleName(name);
        role.setDescription(description);
        role.setRoleType(type);
        role.setParentRole(parent);
        role.setOwner(owner);
        role.setStatus(status);
        repository.insertRole(role);
    }

    /** Puts a role into a composite. */
    private void member(String roleName, String composite) {
        jdbc.update("UPDATE AUTH_ROLE SET PARENT_ROLE = ? WHERE ROLE_NAME = ?",
                composite, roleName);
    }

    private void grantAll(String roleName, String... valueIds) {
        for (String systemId : new String[]{"DEV", "QA", "PRD"}) {
            grant(roleName, systemId, valueIds);
        }
    }

    private void grant(String roleName, String systemId, String... valueIds) {
        for (String valueId : valueIds) {
            RoleGrant grant = new RoleGrant();
            grant.setSystemId(systemId);
            grant.setRoleName(roleName);
            grant.setValueId(valueId);
            repository.insertGrant(grant);
        }
    }

    private void assign(String systemId, String userId, String roleName) {
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setSystemId(systemId);
        assignment.setUserId(userId);
        assignment.setRoleName(roleName);
        repository.insertAssignment(assignment);
    }

    private void sod(String ruleId, String roleA, String roleB, String severity,
                     String description) {
        SodRule rule = new SodRule();
        rule.setRuleId(ruleId);
        rule.setRoleA(roleA);
        rule.setRoleB(roleB);
        rule.setSeverity(severity);
        rule.setDescription(description);
        repository.insertSodRule(rule);
    }
}
