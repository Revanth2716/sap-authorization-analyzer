# Deployment

From a laptop to a SAP landscape. Every step is written down, including the
parts that do not work yet.

---

## 1. The three deployment shapes

| Shape | Where it runs | How |
|---|---|---|
| **Local demo** | Your machine | `java -jar` against a seeded H2 file |
| **WAR on NetWeaver 7.5** | The NetWeaver Java stack | `mvn package` → deploy `sap-authorization-analyzer.war` |
| **Standalone against SAP HANA** | Any Java 8+ host | `java -jar` with a HANA datasource |

The code is Java 8 and uses `javax.servlet` throughout, so the WAR deploys on
the NetWeaver 7.5 Java stack without changes.

---

## 2. Local demo

### Requirements

* Java 8 or newer
* Maven 3.6+

Nothing else. No SAP system, no SAP licence, no database server.

```bash
git clone https://github.com/Revanth2716/sap-authorization-analyzer.git
cd sap-authorization-analyzer
mvn clean package
java -jar target/sap-authorization-analyzer.jar
```

Open <http://localhost:8087>.

| User | Role |
|---|---|
| `security.admin` | SECURITY_ADMIN |
| `audit.analyst` | ANALYST |
| `viewer` | VIEWER |

Password: `demo1234`.

> **The application is closed until the demo data is loaded.** For the first
> few hundred milliseconds after start-up it answers HTTP 503 with *"the
> application is still starting up"* — Spring Boot opens the port before the
> seeders have run, and a half-built landscape is worse than no landscape.

### On Windows

```cmd
run.cmd            build, test and start
run.cmd test       the test suite only
```

### On Linux / macOS

```bash
./run.sh
./run.sh test
```

### Turning the demo data off

```yaml
access:
  seed-demo-users: false   # no demo accounts
  seed-demo-data: false    # no seeded landscape
```

---

## 3. Deploying as a WAR on NetWeaver 7.5

### 3.1 Package the WAR

The `pom.xml` declares `<packaging>jar</packaging>`. Change it to `war`:

```xml
<packaging>war</packaging>
```

and add the servlet container dependency that is provided by the container:

```xml
<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>javax.servlet-api</artifactId>
  <version>3.1.0</version>
  <scope>provided</scope>
</dependency>
```

### 3.2 Deploy

1. `mvn clean package` → `target/sap-authorization-analyzer.war`
2. Copy it into the NetWeaver deploy directory
   (`$SAP_HOME/instance/deploy` on a standalone instance, or use the
   *Deploy Applications* transaction in the NetWeaver Administrator).
3. The context path comes from the WAR name, so the application is available
   at `http://<host>:8000/sap-authorization-analyzer/`.

### 3.3 Configuration on NetWeaver

```yaml
# application-prod.yml  (outside the WAR)
spring:
  datasource:
    url: jdbc:sap://hana-host:39015/?databaseName=PRD&currentSchema=SAPCONT
    driver-class-name: com.sap.db.jdbc.HanaDriver
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

Start with `--spring.profiles.active=prod` and put `application-prod.yml` on
the classpath, or set `SPRING_CONFIG_ADDITIONAL_LOCATION`.

> **The schema is rebuilt on every start.** `schema.sql` drops and recreates
> every table and the seeders reload the landscape. That is right for a demo
> and **wrong for production**. Run the schema once as a migration and switch
> `spring.sql.init.mode` to `never`.

---

## 4. Pointing it at SAP HANA

```bash
mvn -Phana clean package
```

```yaml
spring:
  datasource:
    url: jdbc:sap://hana-host:39015/?databaseName=PRD&currentSchema=SAPCONT
    driver-class-name: com.sap.db.jdbc.HanaDriver
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  sql:
    init:
      mode: never
```

> **HANA and H2 are not identical.** The schema and the queries are ANSI, but
> a HANA schema uses upper-case identifiers and checks `VARCHAR` lengths
> strictly. Run the test suite against HANA before trusting it with a real
> landscape.

---

## 5. The live SAP path (RFC over JCo)

### 5.1 Build with the SAP Java Connector

The SAP Java Connector is published on SAP's own Maven repository. Add it to
`~/.m2/settings.xml`:

```xml
<profile>
  <id>sap</id>
  <repositories>
    <repository>
      <id>sap-maven</id>
      <url>https://artifacts.sap.com/artifactory/</url>
    </repository>
  </repositories>
</profile>
<activeProfiles><activeProfile>sap</activeProfile></activeProfiles>
```

then

```bash
mvn -Pwith-jco clean package
```

### 5.2 Create the ABAP artifacts

1. **SE11** — create the structures `ZAUT_ROLE` and `ZAUT_USER` and the
   table types `ZAUT_ROLE_TAB` and `ZAUT_USER_TAB` from
   `abap/Z_AUT_TYPES.abap`.
2. **SE37** — create the function module `Z_AUTH_EXPOSURE` from
   `abap/Z_AUTH_EXPOSURE.abap`.

   > **Tick "Remote Enabled".** Without it the function module is not
   > registered for RFC, and the call fails with *"function module not found /
   > not remote enabled"*. The error surfaces as an `SM58` entry on the
   > **calling** system, not as a dump on the callee.
3. **SM59** — create the RFC destination (`SAPPRD` by default, see
   `RfcAccessGateway`). The destination name is matched **exactly**,
   including the case.

### 5.3 Configure the gateway

```bash
cp jco.properties.example jco.properties
```

```properties
sap.applicationServer=appserver.example.com
sap.systemNumber=00
sap.client=000
sap.user=RFC_READONLY
sap.password=change-me
```

`jco.properties` is in `.gitignore`. **Never commit real credentials.**

```yaml
access:
  gateway: jco
```

### 5.4 Use a read-only RFC user

The user in `jco.properties` needs to be able to call `Z_AUTH_EXPOSURE` and
nothing else — a role with `S_RFC` access to that function module, no dialog
profile, no administrative profile, no `S_TCODE` rights.

> **The role catalogue is the sensitive read.** `AGR_AUTH` shows exactly what
> each role can do. A user who can read it can read the whole authorisation
> model of the system, which is the thing the system is supposed to protect.

---

## 6. Where the data comes from

| Data | Where it lives | How the portal gets it |
|---|---|---|
| The roles | `AGR_ROLES` | The RFC function module |
| The authorisation values | `AGR_AUTH` | The RFC function module |
| The assignments | `AGR_USERS` | The RFC function module |
| **The sensitivity of an object** | **Nowhere — it is a judgement** | **Maintained on the portal side** |
| **The segregation-of-duties rules** | **Nowhere — they are a customer policy** | **Maintained on the portal side** |

The two entries in bold are the honest gaps. Neither the database nor SAP
holds them: *"table maintenance is critical and display is not"* and
*"payroll administration and payroll processing must not be combined"* are
judgements a security team makes and signs off.

The portal keeps them, and says so. A production implementation would load
them from the customer's own rule catalogue.

---

## 7. Configuration reference

Everything under `access:` in `application.yml`.

| Key | Default | Meaning |
|---|---|---|
| `application-name` | `SAP Authorisation & Access Governance Analyzer` | Shown in the top bar |
| `sap-system` | `PFCG` | The system shown in the top bar as a tag |
| `gateway` | `local` | `local` or `jco` |
| `seed-demo-users` | `true` | Create the three demo accounts |
| `seed-demo-data` | `true` | Load the seeded landscape |
| `demo-password` | `demo1234` | The password of the demo accounts |
| `seed.random-seed` | `20261113` | The seed of the random generator |
| `policy.orphan-role-days` | `90` | How long an unused role may linger |
| `policy.max-drift-systems` | `0` | How many systems a role may differ in before it counts as drift |
| `policy.max-composite-expansion` | `5` | How far a composite may expand before it is too broad to review |

### The policy numbers are the contract

They are configuration, not code, because they are the numbers a customer
agrees with its security team. When a customer says *"a composite that
expands to more than ten roles is too broad for us"*, that is
`max-composite-expansion: 10` — not a code change and a release.

---

## 8. Checklist before going live

- [ ] `access.seed-demo-users` and `access.seed-demo-data` are `false`
- [ ] `spring.sql.init.mode` is `never` and the schema is a migration
- [ ] The datasource is SAP HANA (or another real database), not H2
- [ ] The password hashes are **not** the demo ones — create real accounts
- [ ] The RFC user is read-only and limited to `Z_AUTH_EXPOSURE`
- [ ] `jco.properties` is on the server and **not** in version control
- [ ] The **object sensitivity** list has been agreed with the security team
- [ ] The **segregation-of-duties rules** have been agreed with the business
- [ ] The policy numbers have been agreed, not guessed

---

## 9. What has not been done

No NetWeaver system, no SAP licence and no access to SAP's Maven repository
were available while this was built. The ABAP, the DDIC types and the JCo
adapter are **written against NetWeaver 7.5 semantics and are ready to
compile**, but they have not been through the ABAP syntax check and they have
not been executed.

See [`HONEST-STATUS.md`](HONEST-STATUS.md) for the full list.
