# Resume content

Copy-paste blocks. Pick the length that fits the CV and delete the rest.

> **Do not paste the project title as a client engagement.** This is
> self-initiated portfolio work. Say *"a self-initiated project I built to
> practise..."* — an interviewer who asks "who was the client?" and gets a
> vague answer will stop asking the good questions.

---

## 1. The one-line version

> **SAP Authorisation & Access Governance Analyzer** — access governance for
> SAP landscapes: role expansion, segregation-of-duties conflicts, authorisation
> drift between systems, sensitive exposure and a per-role governance score.

---

## 2. The three-bullet version

> **SAP Authorisation & Access Governance Analyzer** — *Java 8 · Spring Boot 2.7 · JdbcTemplate · SAP HANA · Thymeleaf · ABAP RFC · SAP Java Connector*
>
> - Built an **access governance analyzer** for SAP landscapes that evaluates **segregation-of-duties conflicts across the whole composite expansion** — including the composite that bundles a conflict and hands it to every holder — detects **authorisation drift** where the same role holds different values in development and production, and computes the **blast radius** of each person in production.
> - Modelled the distinction a role list cannot express: the **atomic** roles a role really grants versus the **effective** roles a person ends up holding, and made the segregation check **per person rather than per assignment** — so a conflict held through two separate role assignments is actually found rather than silently missed.
> - Shipped the SAP-side artifacts (**RFC-enabled ABAP function module** reading `AGR_ROLES`/`AGR_AUTH`/`AGR_USERS`, **SAP Java Connector client** behind an optional Maven profile) on **portable ANSI SQL** that runs unchanged on local H2 and SAP HANA, with **58 automated tests**.

---

## 3. The two-bullet version

> **SAP Authorisation & Access Governance Analyzer** — access governance for SAP landscapes
> *Java 8 · Spring Boot 2.7 · JdbcTemplate · SAP HANA · ABAP RFC*
>
> - Built an **access governance analyzer** that finds **segregation-of-duties conflicts across composite role expansion**, detects **authorisation drift** between development and production, and reports **who in production can reach a sensitive authorisation object** — with a per-role governance score and an actionable finding behind it.
> - Modelled the **atomic versus effective role expansion** and made the conflict check **per person rather than per assignment**, so a person holding two conflicting roles is found even when the roles are assigned separately — plus an **RFC-enabled ABAP function module** and **SAP Java Connector client** behind an optional Maven profile, on **portable ANSI SQL** for H2 and SAP HANA, with **58 tests**.

---

## 4. The one-bullet version

> Built an **access governance analyzer for SAP landscapes** (Java 8, Spring Boot, JdbcTemplate, SAP HANA, ABAP RFC) that evaluates segregation-of-duties conflicts across the full composite role expansion, detects authorisation drift between development and production, and computes the blast radius of each person — with an RFC-enabled ABAP function module, a SAP Java Connector client and 58 automated tests.

---

## 5. Where it fits in the CV

This is the **ninth** BASIS portfolio project. The set covers nine distinct
workstreams with no overlap, which is the point — it shows range, not nine
versions of the same CRUD app.

| # | Project | Workstream |
|---|---|---|
| 1 | `sap-basis-ops-console` | Monitoring and governance analytics (Python CLI) |
| 2 | `sap-basis-operations-portal` | Operations: leave self-service and governance |
| 3 | `sap-custom-code-analyzer` | Repository analysis: where-used and custom code quality |
| 4 | `sap-interface-cockpit` | Integration: RFC, IDoc and BAPI operations |
| 5 | `sap-performance-analyzer` | Performance: ST12 and ST03 trace analysis |
| 6 | `sap-backup-readiness` | Continuity and DR: backup, restore and capacity |
| 7 | `sap-transport-analyzer` | Change and transport: object conflicts, release trains |
| 8 | `sap-upgrade-readiness` | Upgrades: obsolete blockers, patch drift, compatibility |
| 9 | **`sap-authorization-analyzer`** | **Authorisations: SoD conflicts, drift, blast radius** |

Recommended CV section:

```markdown
### SAP Basis Analyst — Portfolio Projects

**SAP Authorisation & Access Governance Analyzer** · Java 8 · Spring Boot · JdbcTemplate · SAP HANA · ABAP RFC
Built an access governance analyzer that evaluates segregation-of-duties conflicts across the
full composite role expansion, detects authorisation drift between development and
production, and computes the blast radius of each person in production. 58 automated tests.
→ github.com/Revanth2716/sap-authorization-analyzer

**SAP Upgrade & Support Package Readiness** · ...
**SAP Transport & Change Management Analyzer** · ...
```

---

## 6. Keywords

`SAP BASIS` · `SAP NetWeaver 7.5` · `Authorisations` · `PFCG` · `Role
administration` · `Segregation of Duties` · `SoD conflict matrix` · `Composite
roles` · `Role expansion` · `Authorisation drift` · `Access review` ·
`Least privilege` · `Sensitive authorisations` · `Emergency access` /
`firefighter` · `SoD rules` · `PFCG_AUTH` · `SU01` · `SU53` · `AGR_USERS` ·
`AGR_ROLES` · `AGR_AUTH` · `ABAP` · `RFC` · `SAP Java Connector (JCo)` ·
`SM59` · `SE37` · `SE11` · `Java 8` · `Spring Boot` · `JdbcTemplate` ·
`Thymeleaf` · `SAP HANA` · `Git`

---

## 7. How to talk about it in an interview

### The thirty-second version

> "I built an access governance analyzer for SAP landscapes. The insight is
> that a role list is the wrong unit of analysis — a role name is not an
> authorisation. The authorisation is the whole expansion, held by a person, in
> a system. So the tool expands every composite, finds the people who end up
> holding both sides of a segregation rule, compares the same role across
> development and production, and shows you the blast radius: three people in
> production can reach a sensitive object, and two of them are in a conflict."

### The design decision worth defending

**Why is the segregation check per person rather than per assignment?**

> "Because a person holding two conflicting roles normally holds them in two
> separate assignments. My first version checked one assignment at a time and
> found nothing at all — which is the most natural way to write the check and
> completely wrong. The check has to be on the union of what a person
> effectively holds, and 'effectively' has to include the intermediate
> composites, because a rule that names a composite can only be broken by
> somebody who holds the composite."

### The thing to be honest about

> "The sensitivity of an authorisation object is a judgement, not data.
> Neither the database nor SAP holds the fact that table maintenance is
> critical and display is not — that is something a security team decides.
> The tool keeps that list, and the documentation says so. The Java side is
> fully tested — 58 tests, and every screenshot in the repository was
> captured from the running application."

---

## 8. What to say if you are asked "what was the hardest part?"

> "The expansion. There are two different expansions and they are not the
> same thing, and the first version used one of them for everything. The
> **atomic** roles are the leaves — what a role really grants. The
> **effective** roles are the whole chain, composites included.
>
> "Use the leaves to decide what a role grants, and you get the grants right.
> Use the leaves to decide who holds a role, and you miss every conflict that
> goes through a composite — which is most of them. Using the chain for the
> grants gets them wrong in the other direction: a composite appears to grant
> the sensitive objects its members grant, when it only grants them to whoever
> holds the member."

---

## 9. Things not to claim

| Do not say | Say instead |
|---|---|
| "I built this for a customer" | "I built this as a self-initiated portfolio project" |
| "It is deployed in a production landscape" | "It runs locally against a seeded database; the live SAP path is written but not executed" |
| "It finds every segregation conflict" | "It finds the conflicts defined by the rules it is given — the rules are a customer policy, and the database does not hold them" |
| "It knows which authorisations are critical" | "The object sensitivity is a judgement the security team makes; the portal holds that list" |
| "It revokes access in SAP" | "It records the revocation and recalculates the exposure — the revocation itself is done in PFCG" |

---

## 10. Interview questions this project invites

Have an answer ready for these — they are the good ones:

* "How do you check a role really works before it goes to production?"
* "What is a composite role, and why is it dangerous?"
* "How do you detect a segregation-of-duties conflict?"
* "What is authorisation drift, and how would you find it?"
* "How do you prove somebody does not have more access than they need?"
* "What is a firefighter role, and what should you do with it?"
* "How do you read SU53, and what does it tell you?"
* "What would you do when the business wants emergency access?"

The full answers are in [`INTERVIEW-QA.md`](INTERVIEW-QA.md).
