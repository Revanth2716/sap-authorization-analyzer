-- ---------------------------------------------------------------------------
-- SAP Authorisation & Access Governance Analyzer - database schema
--
-- The access picture a BASIS team is accountable for:
--
--   AUTH_SYSTEM      the landscape - which system is development, which is
--                    quality, which is production
--   AUTH_ROLE        the roles: single or composite, who owns them, whether
--                    they are still active
--   AUTH_OBJECT      the authorisation objects, and which of them are
--                    sensitive enough to be worth a finding on their own
--   AUTH_VALUE       the authorisation values - one row per field value of
--                    an object (F_BKPF_BUK / BUKRS / 1000)
--   AUTH_ROLE_VALUE  what a role grants, per system. The same role can hold
--                    different values in DEV and PRD - that difference is the
--                    authorisation drift this tool is about.
--   AUTH_USER_ROLE   who holds which role, per system
--   AUTH_SOD         the segregation-of-duties rules: pairs of roles that
--                    must never be held by the same person
--
-- In a live landscape the roles come from AGR_ROLES, the authorisation
-- values from AGR_AUTH, the assignments from AGR_USERS and the object
-- catalogue from the authorisation object table.
--
-- All statements are portable ANSI SQL: valid on H2 (local) and SAP HANA.
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS AUTH_USER_ROLE;
DROP TABLE IF EXISTS AUTH_ROLE_VALUE;
DROP TABLE IF EXISTS AUTH_VALUE;
DROP TABLE IF EXISTS AUTH_OBJECT;
DROP TABLE IF EXISTS AUTH_ROLE;
DROP TABLE IF EXISTS AUTH_SOD;
DROP TABLE IF EXISTS AUTH_SYSTEM;
DROP TABLE IF EXISTS PORTAL_AUDIT;
DROP TABLE IF EXISTS PORTAL_USER;
DROP TABLE IF EXISTS SAPUSER;

CREATE TABLE SAPUSER (
    USERID   VARCHAR(20) NOT NULL,
    FULLNAME VARCHAR(60) NOT NULL,
    UFLAG    VARCHAR(1)  NOT NULL,
    DEPT     VARCHAR(40) NOT NULL,
    CONSTRAINT PK_ACC_SAPUSER PRIMARY KEY (USERID)
);

-- The landscape.
CREATE TABLE AUTH_SYSTEM (
    SYSTEM_ID   VARCHAR(10) NOT NULL,
    DESCRIPTION VARCHAR(60) NOT NULL,
    SYS_ROLE    VARCHAR(20) NOT NULL,   -- DEVELOPMENT / QUALITY / PRODUCTION
    IS_TARGET   VARCHAR(1)  NOT NULL,   -- Y / N
    CONSTRAINT PK_ACC_SYSTEM PRIMARY KEY (SYSTEM_ID)
);

-- The roles.
CREATE TABLE AUTH_ROLE (
    ROLE_NAME   VARCHAR(30) NOT NULL,
    DESCRIPTION VARCHAR(80) NOT NULL,
    ROLE_TYPE   VARCHAR(16) NOT NULL,   -- SINGLE / COMPOSITE
    PARENT_ROLE VARCHAR(30) NOT NULL,   -- the composite holding it, or ''
    OWNER       VARCHAR(20) NOT NULL,
    STATUS      VARCHAR(10) NOT NULL,   -- ACTIVE / INACTIVE
    CONSTRAINT PK_ACC_ROLE PRIMARY KEY (ROLE_NAME)
);

-- The authorisation objects, and which of them are sensitive.
CREATE TABLE AUTH_OBJECT (
    OBJECT_NAME VARCHAR(30) NOT NULL,
    DESCRIPTION VARCHAR(80) NOT NULL,
    SENSITIVE   VARCHAR(1)  NOT NULL,   -- Y / N
    SENSITIVITY VARCHAR(20) NOT NULL,   -- CRITICAL / HIGH / NORMAL
    CONSTRAINT PK_ACC_OBJECT PRIMARY KEY (OBJECT_NAME)
);

-- One authorisation value - a field value of an object.
CREATE TABLE AUTH_VALUE (
    VALUE_ID    VARCHAR(20) NOT NULL,
    OBJECT_NAME VARCHAR(30) NOT NULL,
    AUTH_NAME   VARCHAR(40) NOT NULL,   -- F_BKPF_BUK
    FIELD_NAME  VARCHAR(20) NOT NULL,   -- ACTVT / BUKRS
    FIELD_VALUE VARCHAR(40) NOT NULL,   -- '*' / '1000'
    CONSTRAINT PK_ACC_VALUE PRIMARY KEY (VALUE_ID),
    CONSTRAINT FK_ACC_VALUE_OBJECT FOREIGN KEY (OBJECT_NAME) REFERENCES AUTH_OBJECT (OBJECT_NAME)
);

-- What a role grants, per system.
CREATE TABLE AUTH_ROLE_VALUE (
    SYSTEM_ID VARCHAR(10) NOT NULL,
    ROLE_NAME VARCHAR(30) NOT NULL,
    VALUE_ID  VARCHAR(20) NOT NULL,
    CONSTRAINT PK_ACC_ROLE_VALUE PRIMARY KEY (SYSTEM_ID, ROLE_NAME, VALUE_ID),
    CONSTRAINT FK_ACC_RV_ROLE FOREIGN KEY (ROLE_NAME) REFERENCES AUTH_ROLE (ROLE_NAME),
    CONSTRAINT FK_ACC_RV_VALUE FOREIGN KEY (VALUE_ID) REFERENCES AUTH_VALUE (VALUE_ID)
);

-- Who holds which role, per system.
CREATE TABLE AUTH_USER_ROLE (
    SYSTEM_ID VARCHAR(10) NOT NULL,
    USERID    VARCHAR(20) NOT NULL,
    ROLE_NAME VARCHAR(30) NOT NULL,
    CONSTRAINT PK_ACC_USER_ROLE PRIMARY KEY (SYSTEM_ID, USERID, ROLE_NAME),
    CONSTRAINT FK_ACC_UR_ROLE FOREIGN KEY (ROLE_NAME) REFERENCES AUTH_ROLE (ROLE_NAME)
);

-- The segregation-of-duties rules.
CREATE TABLE AUTH_SOD (
    RULE_ID     VARCHAR(12) NOT NULL,
    ROLE_A      VARCHAR(30) NOT NULL,
    ROLE_B      VARCHAR(30) NOT NULL,
    SEVERITY    VARCHAR(10) NOT NULL,   -- CRITICAL / HIGH
    DESCRIPTION VARCHAR(120) NOT NULL,
    CONSTRAINT PK_ACC_SOD PRIMARY KEY (RULE_ID)
);

-- Portal accounts and the audit trail.
CREATE TABLE PORTAL_USER (
    USERNAME      VARCHAR(40) NOT NULL,
    PASSWORD_HASH VARCHAR(120) NOT NULL,
    DISPLAY_NAME  VARCHAR(60) NOT NULL,
    PORTAL_ROLE   VARCHAR(20) NOT NULL,
    ENABLED       VARCHAR(1)  NOT NULL,
    CONSTRAINT PK_ACC_PORTAL_USER PRIMARY KEY (USERNAME)
);

CREATE TABLE PORTAL_AUDIT (
    AUD_ID     VARCHAR(20) NOT NULL,
    USERNAME   VARCHAR(40) NOT NULL,
    ACTION     VARCHAR(40) NOT NULL,
    DETAIL_TXT VARCHAR(400) NOT NULL,
    AT_TS      TIMESTAMP   NOT NULL,
    CONSTRAINT PK_ACC_PORTAL_AUDIT PRIMARY KEY (AUD_ID)
);
