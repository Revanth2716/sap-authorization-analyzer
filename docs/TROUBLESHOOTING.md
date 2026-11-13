# Troubleshooting

The problems that actually happen, and what they mean.

---

## 1. The application

### `Port 8087 was already in use`

```bash
# Windows
netstat -ano | findstr :8087
tasklist /FI "PID eq 12345"

# Linux
lsof -i :8087
```

Or start on a different port:

```bash
java -jar target/sap-authorization-analyzer.jar --server.port=9090
```

> If the application is still running from the IDE, it holds the **JAR file
> open**, and `mvn clean` then fails with *"the process cannot access the
> file because it is being used by another process"*. Stop it before cleaning.

### `503 Service Unavailable` — "the application is still starting up"

This is the **start-up gate** doing its job. Spring Boot opens the web server
before the `CommandLineRunner` beans have run, and the application refuses to
serve a half-built landscape. It opens as soon as the seeders have finished —
a fraction of a second.

If it persists, the seeder failed. Check the log for a constraint violation or
a missing table.

### `Table "AUTH_ROLE" not found`

The schema was not created. Check the log for `Copying 8 resources`, and that
`spring.sql.init.mode` is `always`.

### The application starts but every page is empty

The seeding did not run, or the data is already there:

```yaml
access:
  seed-demo-data: true
```

The seeder is **idempotent** — it does nothing when `AUTH_ROLE` already has
rows. To get the demo landscape back, delete the database file:

```bash
rm -rf data/          # Linux / macOS
# Windows: delete the data folder
```

---

## 2. A finding looks wrong

### "Nobody holds this role" — but somebody clearly does

Check whether it is held **through a composite**. `ZFI_TABLE_ADMIN` is held
by everybody who holds `ZABAP_ADMIN`, and nobody holds it directly. The
analyzer expands the composites, so it should not report it as an orphan —
if it does, the expansion is not reaching the role, which usually means the
role's `PARENT_ROLE` is missing in the data.

### "The role is not the same in every system" — but it looks the same

The drift is measured on the **authorisation values**, not on the object
names. A role that grants company codes 1000, 2000 and 3000 in production
and 1000, 2000, 3000 **and 4000** in development has drifted, even though
the object is the same. Look at the **Authorisation drift** table on the role
screen — it names the exact value that is missing.

### "A composite bundles a segregation conflict" — the rule names roles nobody holds

The rule names two roles that are both **inside** the composite. That is
exactly the problem: everybody who holds the composite has both, including
the people who have never heard of the rule.

### "A less severe conflict does not move the score"

It does — but less than a critical one. A `HIGH` rule costs 25 points and a
`CRITICAL` one costs 35. The severity of the **finding** is the worst rule
involved, and the penalty is the matching one.

### The severity and the score disagree

The severity is the **worst finding**, not the score band. `ZMM_PURCHASER`
scores 65 — "high" on a band — and is high. `ZFI_TABLE_ADMIN` scores 30 and
is **critical**, because granting table maintenance is not the same kind of
problem as a stale one.

### Two people show the same sensitive object but only one has a conflict

The exposure map lists every sensitive object a person reaches; the red
marker counts the **segregation rules** they break. Reaching a sensitive
object is a finding on the *role*; breaking a rule is a finding on the
*person*. A person can have one without the other.

---

## 3. RFC and the live SAP system

### `RFC function module not found` / `not remote enabled`

**The single most common RFC problem.** The function module was created in
`SE37` but **Remote Enabled** was not ticked.

1. `SE37` → open `Z_AUTH_EXPOSURE` → *Attributes* → tick **Remote Enabled**
2. Save and activate

> The error surfaces as an `SM58` entry on the **calling** system, not as a
> dump on the callee.

### The error only appears in `SM58`

`SM58` on the **calling** system shows the RFC errors. The long text names
the reason.

### `jco.properties not found`

The gateway reads `jco.properties` from the **working directory** of the
process. Copy it there and start the application from that directory.

### `Communication link failure` / `RFC destination not found`

The destination name in the code must match the name in `SM59` **exactly**,
including the case. The default is `SAPPRD`:

```java
public RfcAccessGateway() {
    this("SAPPRD");
}
```

### The call works but the role list is empty

`AGR_AUTH` holds the grants, and a role with **no** grants does not appear
in it at all. A role that exists but grants nothing is invisible to the
function module. `AGR_ROLES` has the master list — a live implementation
reads both and unions them, so an empty role is still visible.

### The assignments are empty

`AGR_USERS` holds the assignments per **client**. The RFC function module
reads the current client's data; a role assignment made in another client is
not visible from here. That is a per-client view, not a bug.

---

## 4. The build

### `Could not resolve dependencies for project ... com.sap.jco:sapjco3`

The SAP Java Connector is not on Maven Central. Either add SAP's repository to
`~/.m2/settings.xml` or build **without** the profile:

```bash
mvn clean package          # the default build, no SAP repository needed
```

### `maven-compiler-plugin` fails on a Java 8 feature

The project is compiled with `source/target = 1.8` on purpose, so it stays
deployable on the NetWeaver 7.5 Java stack. Do not raise it.

### The build fails with "the process cannot access the file because it is being used"

A running application holds the JAR open. Stop it before `mvn clean`.

---

## 5. Tests

### `PageRenderTest` fails with 500

A template expression does not resolve. The first line of the stack trace
names the template and the line.

| Symptom | Cause |
|---|---|
| `EL1008E: Property or field 'xxx' cannot be found` | The model attribute is not in the controller |
| `EL1007E: Property or field 'xxx' cannot be found on null` | A `null` object — usually a role that does not exist |
| `EL1028E: Expected to find attribute 'xxx'` | A `th:each` over a value that is not in the model |

### The landscape test fails with "expected 5 but was 0"

The seeder did not run, or the schema was rebuilt under it. The seeders run
in order: `DemoUserSeeder` (`@Order(1)`), `AccessSeeder` (`@Order(2)`), then
`StartupGate` (`@Order(100)`) which opens the application.

### A test fails only when the whole suite runs

Almost always the H2 database being shared between test classes. The test
profile uses `jdbc:h2:mem:accesstest;DB_CLOSE_DELAY=-1`, so the database
survives between classes and the schema is only created once.

---

## 6. Where to look first

| Symptom | Look at |
|---|---|
| A finding is wrong | `GET /api/v1/roles/{name}` — it returns the score, the findings, the expansion and the holders |
| The blast radius is wrong | `GET /api/v1/exposures` — it returns every user, their effective roles, the sensitive objects and the conflicts |
| Something was changed | The **Recent activity** table on the dashboard — revoking an assignment is recorded there |
| The application misbehaved | The log — every sign in, revoked assignment and exported report is written to the audit trail |
