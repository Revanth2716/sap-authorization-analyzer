# Interview questions and answers

The questions this project invites, and the answers that are actually true.

---

## 1. The domain

### "What is a composite role and why is it dangerous?"

> "A composite role holds other roles instead of holding authorisations
> directly. It is convenient — one assignment gives a person everything the
> group needs.
>
> "It is dangerous for two reasons. First, **nobody can review it**: a
> composite that expands to six roles grants the union of six roles, and
> reviewing that means reading six roles. Second, and worse, **it can bundle
> a segregation-of-duties conflict**. If the composite holds the two sides of
> a conflict rule, then everybody who holds the composite has the conflict —
> including the people who have never heard of the rule, and including the
> person who designed the composite and thought it was a sensible grouping.
>
> "That is the finding this project exists for: not 'these two roles
> conflict' but '*this* composite hands the conflict to everybody who holds
> it'."

### "How do you detect a segregation-of-duties conflict?"

> "Three steps, and getting the order wrong is what makes it fail.
>
> "First, expand the role. A person holding a composite holds every role
> underneath it, so the check runs against the **effective** roles — the
> whole chain, composites included. Using only the leaf roles misses every
> conflict that goes through a composite, which is most of them.
>
> "Second, check **per person, not per assignment**. A person holding two
> conflicting roles normally holds them in two separate assignments. The
> check has to be on the union of what one person effectively holds. My first
> version checked one assignment at a time and found nothing at all — which
> is the most natural way to write it and completely wrong.
>
> "Third, decide which rules exist. The rules are the customer's own policy.
> The database does not hold them."

### "How do you check a role really works before it goes to production?"

> "You compare the **authorisation values**, not the object names. A role can
> grant `F_BKPF_BUK` in both systems and still be different — because in
> development it grants company codes 1000, 2000, 3000 **and 4000**, and in
> production only the first three.
>
> "That difference is authorisation drift, and it is invisible to anything
> that compares role definitions. It is also a real operational problem,
> because it means the same code behaves differently in the two systems for
> no reason anybody chose."

### "What is authorisation drift and how would you find it?"

> "The same role holding a different set of authorisation values in two
> systems. You find it by taking the set of values per role per system and
> comparing the sets — not the roles, the values.
>
> "Two things make it easy to get wrong. A role that exists in production and
> not in development is **not** drift — it is a role that was never created
> in development, and comparing it produces a false positive. And false
> positives are how a governance tool gets ignored, so the rule is: drift
> means the same role holds different values, which means **both copies have
> to exist**."

### "How do you prove somebody does not have more access than they need?"

> "You cannot prove it from the role list, because the role list is not what
> the person holds. What you can do is compute the **blast radius**: for
> each person in production, expand everything they hold and list the
> sensitive authorisation objects they reach.
>
> "In the demo landscape that produces one sentence: three people in
> production can reach a sensitive object, and two of them are in a
> segregation-of-duties conflict. That is the sentence a security review opens
> with, and it takes one screen to arrive at."

### "What is a firefighter role, and what should you do with it?"

> "Emergency access — a role that deliberately grants sensitive authorisations
> for a short window, usually with logging. It is genuinely necessary: when
> production is broken at 02:00, somebody has to be able to fix it.
>
> "The governance requirement is that it is **separate, time-boxed and
> logged** — the access comes from a different role, it has an expiry date,
> and using it writes a log entry somebody else reads. A firefighter role
> that quietly becomes a permanent role is how a least-privilege model
> disappears.
>
> "This tool does not model it yet. It is on the roadmap, and it is the
> obvious next rule, because it is the same shape as everything else here:
> a person, an expansion and a system."

### "How do you read SU53, and what does it tell you?"

> "SU53 shows the last failed authorisation check for the current user —
> which object, which field, which value, and the required authorisation.
>
> "It tells you what a user *tried* to do, not what they *can* do. That is
> the useful bit and also the limit: it is a sample of one failure, not a
> statement about the whole access picture. Which is why the tool works from
> the roles and the assignments instead — a failure tells you where somebody
> got stuck, not where they are standing."

---

## 2. The design

### "Why is the severity not derived from the score?"

> "Because that is how problems hide. `ZMM_PURCHASER` scores 65 — 'high' on
> a score band — and it is high. But `ZFI_TABLE_ADMIN` also scores 30 and it
> is **critical**, because a role that grants table maintenance is not the
> same kind of problem as an unused one.
>
> "If the severity came from the score, the finding that a developer can
> maintain any table in production would be filed next to an unused role, and
> both would be equally loud."

### "Why are there two different role expansions?"

> "Because they answer different questions. The **atomic** roles are the
> leaves — what a role really grants. The **effective** roles are the whole
> chain — who holds what, for a conflict.
>
> "Using the leaves for both gets the grants right and the conflicts wrong.
> Using the chain for both gets the grants wrong: a composite would appear to
> grant the sensitive objects its members grant, when it only grants them to
> whoever holds the member."

### "How does the tool know which authorisations are sensitive?"

> "It does not — and that is the honest answer. The sensitivity of an
> authorisation object is a **judgement a security team makes and signs
> off**. Table maintenance is critical, display is not, payroll is critical.
> Neither the database nor SAP holds that.
>
> "So the portal keeps the list, and the documentation says where it comes
> from. Putting the judgement in the tool would be convenient; putting it in
> the customer's rule catalogue is correct, because the moment the judgement
> moves, the tool must move with it."

### "Why do you draw the charts on the server?"

> "Two reasons. Corporate networks block outbound internet, and a chart
> library from a CDN renders as an empty box in exactly the environment where
> this has to work. And a security review has to be able to save the page and
> still see the evidence.
>
> "The exposure map is plain SVG — one row per person, a chip per sensitive
> object, a red marker for a conflict."

---

## 3. The implementation

### "Walk me through the request path for a role detail screen."

> "The interceptor checks the start-up gate, the session and the role. The
> controller asks the analysis service for the assessment of that role. The
> service loads the whole landscape once into a snapshot — the roles, the
> grants, the assignments, the rules — because the conflict check needs every
> assignment of every user, and the drift rule needs every system's copy of
> the role. Loading that per role would be an N+1 query per finding.
>
> "Then the eight rules run, the findings are produced, and the view renders
> them."

### "How do you test a governance rule?"

> "One scenario per test, with the repository mocked so that the test
> contains exactly the situation it is about — an obsolete... sorry, a critical
> object and nothing else, a conflict held through two separate assignments
> and nothing else. When a test fails it names the rule that broke.
>
> "On top of that there is a test against the whole seeded landscape, because
> the seed has to tell a specific story: a developer who reaches table
> maintenance in production through a composite, a composite that bundles a
> conflict, a role that drifted, and one clean role as the control."

### "What is the `PageRenderTest` for, and why is it in every project?"

> "A template can compile, the service layer can be fully green, and one
> screen can still return HTTP 500 because a single expression in the markup
> does not resolve. Nothing else in the suite would catch that."

---

## 4. The SAP side

### "How does the live path work?"

> "Two artifacts. The ABAP function module `Z_AUTH_EXPOSURE` reads the roles
> from `AGR_ROLES`, the authorisation values from `AGR_AUTH` and the
> assignments from `AGR_USERS`. The JCo adapter calls it over RFC through a
> destination maintained in `SM59`.
>
> "The important design point is the interface. `AccessGateway` is the
> boundary; the local implementation reads the database, the RFC
> implementation calls the function module."

### "What goes wrong first with RFC?"

> "The **Remote Enabled** flag. If the function module is created in `SE37`
> without it, the call fails with 'function module not found / not remote
> enabled'. And the error appears as an `SM58` entry on the **calling**
> system, not as a dump on the callee — so the system you are looking at is
> the one that looks fine."

### "What should the RFC user be allowed to do?"

> "Read the function module and nothing else. And the thing to be careful
> about is that `AGR_AUTH` *is* the authorisation model of the system — a
> user who can read it can read everything every role can do. Which is
> rather the point of the system."

---

## 5. The honest questions

### "Has this run against a real SAP system?"

> "No. The Java side runs and is tested — 58 tests, and every screenshot in
> the repository was captured from the running application. The ABAP, the DDIC
> types and the JCo adapter are written against NetWeaver 7.5 semantics and
> are ready to compile, but there was no NetWeaver system and no access to
> SAP's Maven repository in the build environment, so they have not been
> executed. The repository says so in `HONEST-STATUS.md`, and I would rather
> say it here than have it come out in an interview."

### "Where does the data come from then?"

> "It is a seeded synthetic landscape, and the seed is deliberately a *story*
> rather than noise. Nine roles, each with one specific failure mode: a
> composite that bundles a conflict, a role held only through a composite, a
> role that grants payroll and is inactive but still assigned, a role that
> drifted between the systems, and a read-only role that is clean."

### "What would you change first with a real customer?"

> "The object sensitivity list and the segregation rules. In this project both
> are maintained in the portal, which is honest but wrong — they belong to the
> customer's own rule catalogue. The first real integration loads them, so the
> verdict comes from the customer's rules rather than from the tool.
>
> "Second, the **firefighter** case — emergency access with an expiry and a
> log. It is the same shape as everything else here: a person, an expansion
> and a system."

### "What would you not build?"

> "I would not build the revocation into this tool. The analyzer records that
> access was taken away and recalculates the exposure, but the revocation
> itself belongs in PFCG, where the audit trail and the profile update are
> already correct. Two systems of record for one piece of access is how
> access control drifts."

---

## 6. Questions to ask them back

* "How do you currently detect a segregation-of-duties conflict?"
* "Who owns the segregation-of-duties rules — the security team or the
  business?"
* "How do you review the roles nobody holds any more?"
* "How do you know the role in production is the role you reviewed in
  development?"
* "What happens when somebody needs emergency access at 02:00?"
* "How do you handle a role that has to exist in production but not in
  development?"
* "Who signs off that a role is right?"
