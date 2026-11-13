# Learning log

The decisions behind the code, and the mistakes behind some of them. Written
for the person reading the repository in six months — including me.

---

## 1. The order the work happened in

1. The schema, the domain classes and the repository.
2. The rules, written as a plain method with no framework in sight.
3. The seed — and only then, the screens.

The rules are the product; the screens are the report of the rules. Writing
the screens first produces a dashboard that shows what is easy to show.

---

## 2. The decision that shaped the whole project

**A role name is not an authorisation.**

Everything follows from that sentence. The authorisation is the whole
expansion, held by a person, in a system — so the tool has to work on the
expansion, not on the role list.

That reframing is what produced the rules. If the tool were about roles, the
rules would be "is this role used?" and "does this role have an owner?". If
the tool is about *access*, the rules are about conflicts, drift, sensitivity
and who is holding what.

> The general lesson: a metric that is easy to read is often not the one that
> matters. Ask what the metric is *for* before you build a screen around it.

---

## 3. Two expansions, not one

The single biggest thing I got wrong, and the fix is a pair of methods.

`atomicRoles(role)` returns the **leaves** — the single roles a role really
resolves to. `effectiveRoles(role)` returns the **whole chain**, composites
included.

The first version used one expansion for everything, and it was wrong in two
different directions depending on which question it was answering:

- Using the leaves to decide **what a role grants** is correct — a composite
  grants nothing itself.
- Using the leaves to decide **who holds a role** misses every conflict that
  goes through a composite, because the rule names the composite and the
  leaves are not it.
- Using the chain to decide **what a role grants** is wrong — a composite
  would appear to grant the sensitive objects its members grant, when it only
  grants them to whoever holds the member.

Two methods, two purposes, and a test for each.

> The general lesson: when one function answers two different questions, it
> is probably two functions.

---

## 4. The bug the tests caught that the data could not

The segregation check was **per assignment**:

```java
for (UserRoleAssignment assignment : assignments) {
    if (!atomic.contains(rule.getRoleA()) || !atomic.contains(rule.getRoleB())) {
        continue;
    }
    ...
}
```

A person holding two conflicting roles normally holds them in **two separate
assignments** — one row per role. The check looked at one row at a time and
therefore never found anybody holding both.

It was found by a test, not by looking at the output, because the seeded
landscape also had an assignment that looked like it should have matched, and
the message was wrong in a way that was hard to read. The fix is to group the
assignments **by person** and check the union.

> The general lesson: "a person holds X and Y" is a statement about a
> person's *whole* access, not about a row. Model the thing the sentence is
> actually about.

---

## 5. The two other bugs the tests caught

**A role held through a composite was reported as an orphan.** "Nobody holds
this role" was checked against the direct assignments only, so
`ZFI_TABLE_ADMIN` — held by everybody who holds `ZABAP_ADMIN` — looked
unused. The fix is to count the effective holders.

**A high-severity conflict cost nothing.** The penalty was set inside the
branch that handled a *critical* rule, so a high-severity rule produced a
finding with a severity label and **zero points**. The finding never moved
the score, so the role never moved up the list. A finding nobody has to act
on is not a finding.

> The general lesson: a penalty that depends on a condition you only
> half-handled is a silent bug. Write the penalty for every branch.

---

## 6. The one screenshot that made the picture

The first version of the dashboard had a table of roles and a table of
assignments, and the answer to "who can reach table maintenance?" was to read
both and hold it in your head.

It did not work. With three systems and nine roles it was already too much,
and it would be hopeless with thirty.

The exposure map — one row per person, a chip per sensitive object, a red
marker for a conflict — came out of trying to answer that question for
myself while looking at the screenshot of the first version.

> The general lesson: the picture you need is often the picture you have to
> build. If the answer takes a spreadsheet to hold, it wants a screen.

---

## 7. Decisions that were easy

* **Plain Java domain classes.** No Lombok, no records (Java 8), no
  annotations.
* **JdbcTemplate.** Decided in the first ten minutes, for portability.
* **Server-side SVG.** Decided when the first chart was drawn and the CDN
  dependency was written down.
* **The severity is the worst finding.** Carried over from the sibling
  projects, and it is right every time.
* **The start-up gate.** Carried over from the sibling projects, and it saved
  a whole class of confusing first-run behaviour.

---

## 8. Things I would do differently

* **Write the landscape test first.** I wrote the rules, then the seed, then
  the landscape test. The seed was supposed to demonstrate the rules, and it
  took several rounds of adjustment before it demonstrated all of them.
  Writing `AccessLandscapeTest` first would have made the intended story
  explicit before the data was written.
* **Model the profile as well as the role.** Everything here goes through a
  role. A user with a local profile that grants extra authorisations is
  invisible, and in a real landscape that is where the surprises live.
* **Model the firefighter role.** Emergency access is the same shape as
  everything else here — a person, an expansion, a system — and it is the
  obvious next rule.
* **Keep the sensitivity and the rules in a file of their own.** They are
  customer policy, they are the part most likely to be edited, and they are
  currently mixed into the seeder with everything else.
