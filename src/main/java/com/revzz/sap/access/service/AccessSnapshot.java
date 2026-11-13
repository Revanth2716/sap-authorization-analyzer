package com.revzz.sap.access.service;

import com.revzz.sap.access.domain.AccessSystem;
import com.revzz.sap.access.domain.AuthObject;
import com.revzz.sap.access.domain.AuthValue;
import com.revzz.sap.access.domain.RoleGrant;
import com.revzz.sap.access.domain.RoleProfile;
import com.revzz.sap.access.domain.SodRule;
import com.revzz.sap.access.domain.UserRoleAssignment;
import com.revzz.sap.access.repo.AuthorizationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A consistent view of the whole access landscape, loaded once.
 *
 * <p>Role expansion needs the whole role graph, and the drift rule needs
 * every system's copy of the same role. Loading that per role would be an
 * N+1 query per finding, so the landscape is loaded once and every
 * assessment reads from it.</p>
 */
public class AccessSnapshot {

    private final Map<String, RoleProfile> roles;
    private final Map<String, AuthObject> objects;
    private final Map<String, AuthValue> values;

    /** role -> system -> the value ids the role grants there. */
    private final Map<String, Map<String, List<String>>> grantsByRole;

    private final List<UserRoleAssignment> assignments;
    private final List<SodRule> sodRules;
    private final List<AccessSystem> systems;

    private AccessSnapshot(Map<String, RoleProfile> roles,
                           Map<String, AuthObject> objects,
                           Map<String, AuthValue> values,
                           Map<String, Map<String, List<String>>> grantsByRole,
                           List<UserRoleAssignment> assignments,
                           List<SodRule> sodRules,
                           List<AccessSystem> systems) {
        this.roles = roles;
        this.objects = objects;
        this.values = values;
        this.grantsByRole = grantsByRole;
        this.assignments = assignments;
        this.sodRules = sodRules;
        this.systems = systems;
    }

    public static AccessSnapshot load(AuthorizationRepository repository) {
        Map<String, RoleProfile> roles = new LinkedHashMap<String, RoleProfile>();
        for (RoleProfile role : repository.findRoles()) {
            roles.put(role.getRoleName(), role);
        }

        Map<String, AuthObject> objects = new HashMap<String, AuthObject>();
        for (AuthObject object : repository.findObjects()) {
            objects.put(object.getObjectName(), object);
        }

        Map<String, AuthValue> values = new HashMap<String, AuthValue>();
        for (AuthValue value : repository.findValues()) {
            values.put(value.getValueId(), value);
        }

        Map<String, Map<String, List<String>>> grantsByRole =
                new HashMap<String, Map<String, List<String>>>();
        for (RoleGrant grant : repository.findGrants()) {
            Map<String, List<String>> bySystem = grantsByRole.get(grant.getRoleName());
            if (bySystem == null) {
                bySystem = new LinkedHashMap<String, List<String>>();
                grantsByRole.put(grant.getRoleName(), bySystem);
            }
            List<String> list = bySystem.get(grant.getSystemId());
            if (list == null) {
                list = new ArrayList<String>();
                bySystem.put(grant.getSystemId(), list);
            }
            if (!list.contains(grant.getValueId())) {
                list.add(grant.getValueId());
            }
        }

        return new AccessSnapshot(roles, objects, values, grantsByRole,
                repository.findAssignments(), repository.findSodRules(), repository.findSystems());
    }

    public Map<String, RoleProfile> roles() {
        return roles;
    }

    public RoleProfile role(String name) {
        return roles.get(name);
    }

    public Map<String, AuthObject> objects() {
        return objects;
    }

    public AuthObject object(String name) {
        return objects.get(name);
    }

    public AuthValue value(String valueId) {
        return values.get(valueId);
    }

    public Map<String, List<String>> grantsOf(String roleName) {
        Map<String, List<String>> bySystem = grantsByRole.get(roleName);
        return bySystem == null ? Collections.<String, List<String>>emptyMap() : bySystem;
    }

    public List<UserRoleAssignment> assignments() {
        return assignments;
    }

    public List<SodRule> sodRules() {
        return sodRules;
    }

    public List<AccessSystem> systems() {
        return systems;
    }

    /** The production system - the one everybody else is compared against. */
    public AccessSystem targetSystem() {
        for (AccessSystem system : systems) {
            if (system.isTarget()) {
                return system;
            }
        }
        return systems.isEmpty() ? null : systems.get(0);
    }

    /**
     * The atomic roles a role resolves to.
     *
     * <p>A single role is its own atomic role. A composite role expands into
     * the atomic roles it holds, recursively. A cycle in the role graph —
     * which a landscape really can have — stops the expansion rather than
     * hanging it.</p>
     */
    public List<String> atomicRoles(String roleName) {
        List<String> result = new ArrayList<String>();
        expand(roleName, result, new HashSet<String>());
        Collections.sort(result);
        return result;
    }

    private void expand(String roleName, List<String> result, Set<String> visiting) {
        if (roleName == null || !visiting.add(roleName)) {
            return;
        }
        RoleProfile role = roles.get(roleName);
        if (role == null) {
            return;
        }
        if (role.isComposite()) {
            for (RoleProfile member : roles.values()) {
                if (roleName.equals(member.getParentRole())) {
                    expand(member.getRoleName(), result, visiting);
                }
            }
        } else if (!result.contains(roleName)) {
            result.add(roleName);
        }
        visiting.remove(roleName);
    }

    /**
     * The roles a person effectively holds.
     *
     * <p>Different from {@link #atomicRoles(String)} on purpose. The atomic
     * roles are the leaves — what a role really grants. The <em>effective</em>
     * roles are the whole chain: a person holding a composite also holds
     * that composite, and every role underneath it.
     *
     * <p>That distinction matters for segregation of duties. A rule that
     * names a composite can only be broken by a person who holds the
     * composite, and the leaf roles alone would never match it.</p>
     */
    public List<String> effectiveRoles(String roleName) {
        List<String> result = new ArrayList<String>();
        collect(roleName, result, new HashSet<String>());
        return result;
    }

    private void collect(String roleName, List<String> result, Set<String> visiting) {
        if (roleName == null || !visiting.add(roleName)) {
            return;
        }
        if (!result.contains(roleName)) {
            result.add(roleName);
        }
        RoleProfile role = roles.get(roleName);
        if (role != null) {
            for (RoleProfile member : roles.values()) {
                if (roleName.equals(member.getParentRole())) {
                    collect(member.getRoleName(), result, visiting);
                }
            }
        }
        visiting.remove(roleName);
    }

    /** Everyone who holds a role, in any system - directly or through a composite. */
    public List<String> holdersOf(String roleName) {
        List<String> holders = new ArrayList<String>();
        for (UserRoleAssignment assignment : assignments) {
            if (!effectiveRoles(assignment.getRoleName()).contains(roleName)) {
                continue;
            }
            if (!holders.contains(assignment.getUserId())) {
                holders.add(assignment.getUserId());
            }
        }
        Collections.sort(holders);
        return holders;
    }

    /**
     * The users who hold both sides of a segregation-of-duties rule.
     *
     * <p>The check is per <em>person</em>, not per assignment. A person
     * holding two conflicting roles normally holds them in two separate
     * assignments, so checking one assignment at a time would find nothing
     * at all.</p>
     *
     * @return the conflicting users, and the systems it was found in
     */
    public Map<String, List<String>> conflictingUsers(SodRule rule) {
        Map<String, Map<String, Set<String>>> byUser =
                new LinkedHashMap<String, Map<String, Set<String>>>();
        for (UserRoleAssignment assignment : assignments) {
            Map<String, Set<String>> bySystem = byUser.get(assignment.getUserId());
            if (bySystem == null) {
                bySystem = new LinkedHashMap<String, Set<String>>();
                byUser.put(assignment.getUserId(), bySystem);
            }
            Set<String> effective = bySystem.get(assignment.getSystemId());
            if (effective == null) {
                effective = new HashSet<String>();
                bySystem.put(assignment.getSystemId(), effective);
            }
            effective.addAll(effectiveRoles(assignment.getRoleName()));
        }

        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : byUser.entrySet()) {
            for (Map.Entry<String, Set<String>> systemEntry : entry.getValue().entrySet()) {
                Set<String> effective = systemEntry.getValue();
                if (!effective.contains(rule.getRoleA())
                        || !effective.contains(rule.getRoleB())) {
                    continue;
                }
                List<String> systems = result.get(entry.getKey());
                if (systems == null) {
                    systems = new ArrayList<String>();
                    result.put(entry.getKey(), systems);
                }
                if (!systems.contains(systemEntry.getKey())) {
                    systems.add(systemEntry.getKey());
                }
            }
        }
        return result;
    }
}
