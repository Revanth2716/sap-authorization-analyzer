# Architecture

Why the SAP Authorisation & Access Governance Analyzer is built the way it
is.

---

## 1. The shape of the problem

A role list is the wrong unit of analysis. It looks complete and it is not,
because three things hide behind it:

| What hides | Why the role list cannot show it |
|---|---|
| **The composite** | `ZABAP_ADMIN` grants nothing itself. It grants whatever the roles underneath it grant |
| **The person** | A conflict is not in a role, it is in the **pair of roles one person holds** |
| **The other system** | The same role with different values in development and production is two different roles with the same name |

The value of this application is that it works on the **expansion**: what a
role really grants, who ends up holding it, and whether the systems agree.

---

## 2. The layers

```
web/          Controllers, session auth, role gating, REST API
service/      The rules: governance analysis, audit
repo/         JdbcTemplate over portable ANSI SQL
domain/       Plain Java objects, no framework annotations
support/      Server-side SVG rendering
integration/  The boundary to a live SAP system
seed/         The demo landscape and the demo accounts
```

The dependency rule is one-directional and never bent: `web → service → repo
→ domain`.

### Why `JdbcTemplate` and not JPA

1. **Portability.** The SQL has to run unchanged on H2 (the demo) and on SAP
   HANA (production).
2. **No `LIMIT`.** `LIMIT` is not valid on SAP HANA. Every "top N" query here
   fetches the rows and cuts the list **in Java**
   (`PlatformRepository.findRecentAudit`).
3. **The schema is eight tables.** An ORM would add more configuration than the
   schema itself.

---

## 3. The domain model

```
AccessSystem         one system: development, quality or production
   │
RoleProfile          one role: single or composite, its owner, its status
   │
   ├── RoleGrant        one authorisation value granted by a role, in a system
   │
   ├── UserRoleAssignment  one user holding one role, in one system
   │
   └── RoleAssessment   the assessment: score, severity, findings
            │
            └── AuthorizationFinding  severity + category + message + action

AuthObject          one authorisation object, and how sensitive it is
AuthValue           one field value of an object: F_BKPF_BUK / ACTVT = 1000
SodRule             two roles that must never be held by the same person
UserExposure        what one person can actually reach, in one system
```

### Why the authorisation *value* is the unit

The role does not grant `F_BKPF_BUK`. It grants `F_BKPF_BUK / ACTVT = 1000`
and `F_BKPF_BUK / ACTVT = 2000`, and not `= 3000`. The set of values is what
the role can actually do, and it is what differs between the systems.

That is also why drift is measured on values and not on objects.

---

## 4. The two expansions

This is the idea the whole project rests on, and it is the thing a role list
cannot express.

| Expansion | What it is | What it is for |
|---|---|---|
| **Atomic roles** | The leaves — the single roles a role really resolves to | What the role **grants** |
| **Effective roles** | The whole chain — the role itself plus every role underneath, composites included | Who **holds** the role, for a conflict |

A person holding `ZALL_INTEGRATION` effectively holds `ZABAP_ADMIN`, which
effectively holds `ZABAP_DEV` and `ZFI_TABLE_ADMIN`. The atomic roles are the
leaves; the effective roles are everything on the way down.

Both are needed, and conflating them produces two different wrong answers:

- Using the atomic roles to match a rule that names a composite **misses the
  conflict entirely**.
- Using the effective roles to decide what a role grants **overstates what it
  grants** — a composite would appear to grant the sensitive objects its
  members grant, when in fact it grants them only to whoever holds the
  member.

`AccessSnapshot.atomicRoles` and `AccessSnapshot.effectiveRoles` are
therefore two different methods, and both are used.

---

## 5. The rules

`AccessGovernanceService.assess()` runs eight checks per role.

| # | Condition | Finding | Severity | Penalty |
|---|---|---|---|---|
| 1 | The role grants a sensitive authorisation value | `SENSITIVE_EXPOSURE` | The object's (CRITICAL / HIGH) | 35 / 20 |
| 2 | A user holds this role together with the conflicting role | `SO_D_CONFLICT` | The rule's (CRITICAL / HIGH) | 35 / 25 |
| 3 | A composite holds both sides of a rule | `CONFLICT_BUNDLED_IN_COMPOSITE` | CRITICAL | 35 |
| 4 | The role holds different values in different systems | `AUTHORIZATION_DRIFT` | HIGH | 30 |
| 5 | An inactive role is still assigned | `INACTIVE_ROLE_ASSIGNED` | HIGH | 25 |
| 6 | A composite expands beyond the policy limit | `COMPOSITE_EXPANSION` | MEDIUM | 20 |
| 7 | An active role that nobody holds | `ORPHAN_ROLE` | MEDIUM | 15 |
| 8 | A role with no owner | `NO_OWNER` | LOW | 10 |

### The four rules that are easy to get wrong

**The segregation check is per person, not per assignment.** A person holding
two conflicting roles normally holds them in **two separate assignments**.
Checking one assignment at a time finds nothing at all — and that was the
first version, and it found nothing.

**A composite that bundles a conflict is its own finding.** Everybody who
holds the composite has the conflict, including the people who have never
heard of it. Reporting it only on the two atomic roles puts the finding on
roles nobody holds.

**Drift means the same role with different values, so both copies must
exist.** A role that is in production and not in development is not drift, it
is a role that was never created in development. Comparing a role with a role
that is not there is a false positive, and false positives are how a
governance tool gets ignored.

**"Nobody holds it" means nobody *effectively* holds it.** A role held only
through a composite is not an orphan — it is held by everybody who holds the
composite.

### One finding, one cost

A **less severe** rule still costs something. The first version made the
penalty depend on the rule's severity, and a high-severity conflict produced
a finding that cost **zero points** — which means it never moved the score
and therefore never moved the role up the list. A finding nobody has to act
on is not a finding.

---

## 6. The score, and why the severity is not the score

```
score    = max(0, 100 - sum of the penalties)
severity = the worst severity among the findings
```

`ZMM_PURCHASER` scores 65 — "high" on a score band — and is **high**. But
`ZFI_TABLE_ADMIN` scores 30 and is **critical**, because a role that grants
table maintenance is not the same kind of problem as a stale one.

If the severity came from the score, the finding that a developer can
maintain any table in production would be filed next to an unused role, and
both would be equally loud.

`AccessGovernanceServiceTest.severityIsTheWorstFindingNotTheScoreBand` pins
this down, and the test name says why it exists.

---

## 7. The blast radius

`AccessGovernanceService.exposures()` answers the question the role list
cannot: **what can each person actually reach?**

For every user in every system it takes the union of the **effective** roles
they hold, collects the sensitive authorisation objects underneath them, and
lists the segregation rules they break.

It is rendered as the **exposure map** — one row per person, a chip per
sensitive object, and a red marker for a conflict. Three people in production
reach a sensitive object; two of them are in a conflict. That is the sentence
a security review opens with, and it takes one screen to arrive at it.

---

## 8. The gateway boundary

```java
public interface AccessGateway {
    String description();
    List<AccessSystem> fetchSystems();
    List<RoleProfile> fetchRoles(String systemId);
    List<UserRoleAssignment> fetchAssignments(String systemId);
}
```

| Implementation | Where | When |
|---|---|---|
| `LocalAccessGateway` | `src/main/java` | The default — reads the local database |
| `RfcAccessGateway` | `src/extra/java` | `access.gateway: jco` — calls `Z_AUTH_EXPOSURE` over RFC |

The JCo adapter lives in `src/extra/java` and is added by the `with-jco`
profile, because the SAP Java Connector is published on SAP's own Maven
repository and not on Maven Central. The default build therefore compiles and
tests anywhere.

---

## 9. The start-up gate

Spring Boot opens the web server **before** the `CommandLineRunner` beans have
run, so for the first few hundred milliseconds the application serves a
half-built landscape — a dashboard showing three roles instead of nine.

`StartupGate` is a `CommandLineRunner` with the highest order, so it is
always the last one, and the interceptor answers **503** with *"the
application is still starting up"* until it runs. The same gate is in every
project in this portfolio, because the same bug was found in two different
projects before it was fixed once and ported.

---

## 10. Security

| Concern | Decision |
|---|---|
| Password storage | BCrypt, hashed at runtime — no reusable hash is committed |
| Sessions | Server-side `HttpSession`, 30-minute timeout, `HttpOnly` and `SameSite=Lax` |
| Authorisation | Three roles, one interceptor, one `@RequireRole` |
| The API | Authenticated like the screens; only `/api/v1/health` is open |
| Write operations | Revoking a role assignment is `SECURITY_ADMIN` only, and is audited |

Spring Security is used for the BCrypt encoder only — the filter chain is
deliberately not used, so the application stays deployable as a plain WAR.

Every finding message and every recommendation is written by the service
layer, never by the view.

---

## 11. Testing strategy

| Layer | What is tested | How |
|---|---|---|
| Rules | Each rule on its own | `AccessGovernanceServiceTest` — a mocked repository per scenario |
| Data | The story the seed has to tell | `AccessLandscapeTest` — the whole Spring context |
| Contract | The API shape and the authentication | `ApiControllerTest` — MockMvc |
| Rendering | Every screen renders | `PageRenderTest` — MockMvc against every URL |
| Start-up | The application opens after the seed | `StartupGateTest` |

`PageRenderTest` is not optional. A template can compile, the service layer
can be fully green, and one screen can still return HTTP 500 because a
single expression in the markup does not resolve.
