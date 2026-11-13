# Honest status

What in this repository is **verified working**, what is **written but not yet
executed**, and what is **modelled rather than measured**. Read this before
describing the project in an interview.

---

## 1. Verified working today

| Item | Evidence |
|---|---|
| The application builds | `mvn clean package` → `BUILD SUCCESS`, executable JAR produced |
| The whole test suite passes | `mvn test` → **58 tests, 0 failures, 0 errors** |
| The application starts and serves | `java -jar target/sap-authorization-analyzer.jar` → HTTP 200 on `/api/v1/health` |
| Every screen renders | Seven screens captured from the running application into `docs/screenshots/` |
| The role expansion works | `AccessGovernanceServiceTest` — a composite expands to its atomic roles, and a person holding a composite reaches the sensitive objects underneath it |
| The segregation check works | The same test — a person holding both sides of a rule is found, **including when the two roles are assigned separately** |
| The bundled-conflict rule works | A composite that holds both sides of a rule is a critical finding of its own |
| The drift rule works | The same role with different values in two systems is a finding; the same role with the same values is not; a role that exists in only one system is not drift |
| The severity rule holds | `severityIsTheWorstFindingNotTheScoreBand` — a critical exposure stays critical at a score of 65 |
| The penalty rule holds | `aHighSeverityRuleStillCostsSomething` — a less severe rule still costs points |
| The landscape tells the story | `AccessLandscapeTest` — nine roles, six critical, the payroll role at 5, the read-only role at 100, the average 48 |
| The blast radius is right | Three people in production reach a sensitive object, two of them are in a conflict, and `DEVADMIN` reaches `F_OB01` through a composite |
| The API works | `ApiControllerTest` — health, roles, one role, findings, exposures, the SoD board |
| The API is authenticated | Every endpoint except `/api/v1/health` returns 401 for an anonymous caller |
| Role gating works | `PageRenderTest` — a viewer is blocked from the SoD board and the report, an analyst cannot revoke an assignment |
| The start-up gate works | `StartupGateTest` — the application is closed until the seeders have run |
| The log is clean | No exception or error in the application log during a full pass |

**Reproduce all of it:**

```bash
mvn clean package
java -jar target/sap-authorization-analyzer.jar
```

---

## 2. Written but not yet executed on a live SAP system

| Item | Status | Why |
|---|---|---|
| `abap/Z_AUTH_EXPOSURE.abap` | **Not yet compiled on NetWeaver** | No SAP system was available in the build environment. The code follows NetWeaver 7.5 / ABAP 7.40+ syntax and the Remote-Enabled flag is documented in the header, but it has not been through the ABAP syntax check. |
| `abap/Z_AUT_TYPES.abap` | **Not yet created in SE11** | These are the DDIC structures the function module imports and exports. |
| `src/extra/java/.../RfcAccessGateway.java` | **Not yet compiled** | The SAP Java Connector (`com.sap.jco:sapjco3`) is published on SAP's own Maven repository, not Maven Central, and that host was unreachable from the build environment. The `with-jco` profile declares the exact coordinates needed. |
| The HANA JDBC path | **Configured, not exercised** | The `hana` profile and the datasource configuration are in place; no HANA instance was available. |
| The WAR deployment on NetWeaver 7.5 | **Designed, not executed** | `DEPLOYMENT.md` describes it; the project is built as a JAR, and the `pom.xml` change to `war` is documented but not committed. |
| The `AGR_*` field names | **Unverified against a live release** | The classic PFCG tables are stable, but the column set differs between releases, and the join between `AGR_AUTH` and `AGR_ROLES` is worth checking on a real system. |

**What this means in an interview:** say *"the SAP-side artifacts are written
against NetWeaver 7.5 semantics and are ready to compile, but I could not run
them because I did not have a system available."*

**The plan to close the gap** is in [`DEPLOYMENT.md`](DEPLOYMENT.md): create
the DDIC structures in `SE11`, paste the function module into `SE37` with
**Remote-Enabled** ticked, create the RFC destination in `SM59`, and build the
JCo adapter against SAP's Maven repository.

---

## 3. The two gaps that are not build problems

**The sensitivity of an authorisation object is a judgement, not data.**
Neither the database nor SAP holds the fact that table maintenance is critical
and display is not. That is something a security team decides and signs off.

**The segregation-of-duties rules are a customer policy.** The database does
not hold "payroll administration and payroll processing must not be combined".
SAP ships a default SoD catalogue, but it is a starting point that every
customer edits.

So in this project:

| Data | Where it comes from |
|---|---|
| The roles, the grants, the assignments | The database — read by the RFC function module |
| **The object sensitivity** | **Maintained on the portal side** |
| **The segregation rules** | **Maintained on the portal side** |

A production implementation loads the last two from the customer's own rule
catalogue, so the verdict comes from the customer's rules rather than from the
tool.

---

## 4. Modelled rather than measured

| Area | What is modelled | What production needs |
|---|---|---|
| The roles | Nine single and composite roles | Every role of the landscape, per client |
| The authorisation values | Thirteen values across seven objects | The real `AGR_AUTH` of every system |
| The assignments | Six users across three systems | Every assignment of every system |
| The sensitivity marking | A hand-written list of seven objects | The customer's own object catalogue |
| The SoD rules | Three rules | The customer's SoD catalogue, including the SAP default as a starting point |
| The systems | Three — development, quality and production | Every system, and the clients inside them |

---

## 5. Known limitations — the ones an interviewer would find

1. **A role with no grants is invisible to the RFC path.** `AGR_AUTH` holds
   the grants, so a role that grants nothing does not appear in it. A live
   implementation reads `AGR_ROLES` as well and unions the two.
2. **The assignments are per client.** `AGR_USERS` holds the assignments of
   the current client. A role assigned in another client is not visible from
   here — that is a per-client view, and the tool does not aggregate across
   clients.
3. **A direct profile assignment is not modelled.** Everything here goes
   through a role. A user with a local profile that grants extra
   authorisations would not be detected.
4. **No emergency access.** The firefighter role — separate, time-boxed and
   logged — is the obvious next rule and is not implemented.
5. **No history.** The tool answers "who can reach what now", not "who could
   reach it in March". Drift over time is the question a real review asks
   next.
6. **The schema is rebuilt on every start.** `schema.sql` drops and recreates
   every table and the seeders reload the landscape. That is predictable for a
   demo and wrong for production — migrations replace it, and
   `spring.sql.init.mode` becomes `never`.
7. **No production-readiness check.** The tool does not ask whether a role
   has been through a review, and it does not know when it was last
   reviewed.

---

## 6. What the project is not

* It is **not** connected to a live SAP system out of the box — it runs
  against the seeded dataset, and the RFC path needs a system that was not
  available.
* It is **not** a role maintenance tool. It never creates a role, never
  changes an authorisation and never revokes anything in SAP. It records
  that a revocation happened and recalculates the exposure.
* It is **not** a substitute for PFCG's own authorisation checks or for
  `SU53`. It reads the model; it does not enforce it.
* It is **not** a SoD catalogue. It evaluates the rules it is given against
  the roles it can see.

---

## 7. Why the gaps were left open rather than faked

A portfolio project that pretends to have run against a production system is
easy to expose in an interview: the interviewer asks which system, which
client, which role, and the story collapses. The gaps above are specific,
explained, and each one comes with the concrete step that closes it.

The same rule applies to the data. The seeded landscape is explicitly
synthetic, and the way it is seeded — one deliberate failure mode per role —
makes it a **test fixture**, not a pretence of a customer.
