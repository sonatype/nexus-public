/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.security.role;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.security.Roles;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Determines whether the current subject is permitted to assign a given role or privilege.
 *
 * <p>
 * A non-admin role is assignable if the caller already holds all of the permissions it contains.
 * The {@code nx-admin} role is special-cased: only subjects that already have it may assign it.
 *
 * <p>
 * A privilege is assignable if the caller already holds the permission that privilege confers. Resolution fails closed:
 * a privilege whose authority cannot be determined is never assignable.
 */
@Component
public class RoleAssignabilityChecker
{
  private static final Logger log = LoggerFactory.getLogger(RoleAssignabilityChecker.class);

  /**
   * Denial message shared by the REST and ExtDirect paths so the two cannot drift apart.
   */
  public static final String GRANT_FORBIDDEN =
      "The current user is not permitted to grant the following to a role: %s.";

  private final RolePermissionResolver rolePermissionResolver;

  private final SecurityHelper securityHelper;

  private final SecurityConfigurationManager configurationManager;

  private final List<PrivilegeDescriptor> privilegeDescriptors;

  @Autowired
  public RoleAssignabilityChecker(
      final RolePermissionResolver rolePermissionResolver,
      final SecurityHelper securityHelper,
      final SecurityConfigurationManager configurationManager,
      final List<PrivilegeDescriptor> privilegeDescriptors)
  {
    this.rolePermissionResolver = requireNonNull(rolePermissionResolver);
    this.securityHelper = requireNonNull(securityHelper);
    this.configurationManager = requireNonNull(configurationManager);
    this.privilegeDescriptors = requireNonNull(privilegeDescriptors);
  }

  public boolean isRoleAssignable(final String roleId) {
    Subject subject = securityHelper.subject();
    if (Roles.ADMIN_ROLE_ID.equals(roleId)) {
      return subject.hasRole(Roles.ADMIN_ROLE_ID);
    }
    // A nonexistent role confers no authority: resolvePermissionsInRole catches NoSuchRoleException and returns an
    // empty set, so allMatch is vacuously true and the role is reported grantable. This mirrors the
    // nonexistent-privilege case in isPrivilegeAssignable and is safe for the same reason -- the downstream
    // create/update validation (validateContainedRolesAndPrivileges / validateAndCleanOrphanedPrivileges) calls
    // role.getRoles().forEach(this::readRole), which throws rather than strips, so a dangling child-role id is
    // rejected there.
    return rolePermissionResolver.resolvePermissionsInRole(roleId).stream().allMatch(subject::isPermitted);
  }

  /**
   * Determines whether the current subject already holds the permission conferred by the given privilege, and may
   * therefore grant it to a role.
   */
  public boolean isPrivilegeAssignable(final String privilegeId) {
    Subject subject = securityHelper.subject();

    // Administrators hold nexus:* and may grant any privilege. Short-circuit before descriptor resolution so an
    // absent descriptor (a feature-flagged or unloaded plugin, which RolePermissionResolverImpl already warns about)
    // cannot hand an administrator a hard denial. Mirrors the admin special-case in isRoleAssignable.
    if (subject.hasRole(Roles.ADMIN_ROLE_ID)) {
      return true;
    }

    CPrivilege privilege;
    try {
      privilege = configurationManager.readPrivilege(privilegeId);
    }
    catch (NoSuchPrivilegeException e) {
      // A privilege that does not exist confers no authority, so there is nothing to authorize here. The
      // nonexistent-privilege case is owned by the config store, which sees every create and update regardless of
      // entry point: SecurityConfigurationManagerImpl.createRole rejects a dangling id outright, and updateRole
      // strips it. Deciding otherwise here would turn a stale id into a hard failure for callers that tolerate it
      // today, without preventing any grant.
      log.debug("Privilege {} does not exist; it confers no authority to grant", privilegeId, e);
      return true;
    }

    return subjectHoldsPermissionOf(subject, privilege);
  }

  /**
   * Returns the ids of the child roles and privileges that {@code desired} adds relative to {@code current} but that
   * the caller is not itself authorized to grant. An empty result means the change confers no authority the caller does
   * not already hold.
   *
   * <p>
   * Only additions are examined: removing authority from a role is not an escalation. {@code current} is the immutable
   * pre-change state of the role, and must be {@code null} when the role is being created.
   *
   * <p>
   * A non-empty result is logged at {@code WARN} here rather than at the call sites, so every entry point that enforces
   * the check shares one greppable log line that cannot drift.
   *
   * @param desired the role as it would be persisted
   * @param current the role as it exists now, or {@code null} when creating
   */
  public Set<String> findUngrantableAdditions(final Role desired, @Nullable final Role current) {
    requireNonNull(desired);

    Set<String> addedRoles = added(desired.getRoles(), current == null ? null : current.getRoles());
    Set<String> addedPrivileges = added(desired.getPrivileges(), current == null ? null : current.getPrivileges());

    Set<String> ungrantable = new LinkedHashSet<>();
    addedRoles.stream().filter(roleId -> !isRoleAssignable(roleId)).forEach(ungrantable::add);
    ungrantable.addAll(findUngrantablePrivileges(addedPrivileges));

    if (!ungrantable.isEmpty()) {
      log.warn("Rejected attempt to grant authority not held by the caller on role {}: {}", desired.getRoleId(),
          ungrantable);
    }
    return ungrantable;
  }

  /**
   * Returns the ids of the given added privileges that the caller may not grant. The privileges are read in a single
   * batch to avoid the N+1 read pattern that {@code readPrivileges(Set)} was introduced to eliminate (NEXUS-52583).
   */
  private Set<String> findUngrantablePrivileges(final Set<String> addedPrivileges) {
    if (addedPrivileges.isEmpty()) {
      return Collections.emptySet();
    }

    Subject subject = securityHelper.subject();
    // Administrators hold nexus:* and may grant any privilege; skip resolution entirely. This also avoids denying an
    // admin when a privilege's descriptor is absent, matching the short-circuit in isPrivilegeAssignable.
    if (subject.hasRole(Roles.ADMIN_ROLE_ID)) {
      return Collections.emptySet();
    }

    Set<String> ungrantable = new LinkedHashSet<>();
    // readPrivileges silently drops ids with no privilege, so a nonexistent id is treated as grantable -- the same
    // outcome as the NoSuchPrivilegeException branch in isPrivilegeAssignable, and safe for the same reason.
    for (CPrivilege privilege : configurationManager.readPrivileges(addedPrivileges)) {
      if (!subjectHoldsPermissionOf(subject, privilege)) {
        ungrantable.add(privilege.getId());
      }
    }
    return ungrantable;
  }

  /**
   * Whether {@code subject} already holds the permission conferred by {@code privilege}. Fails closed: a privilege
   * whose permission cannot be resolved (no matching descriptor) is never held.
   */
  private boolean subjectHoldsPermissionOf(final Subject subject, final CPrivilege privilege) {
    Permission permission = permissionOf(privilege);
    if (permission == null) {
      log.warn("Treating privilege {} of type {} as not assignable: its permission could not be resolved",
          privilege.getId(), privilege.getType());
      return false;
    }
    return subject.isPermitted(permission);
  }

  /**
   * Formats the denial message naming the ids the caller may not grant.
   */
  public static String grantForbiddenMessage(final Set<String> ungrantable) {
    return String.format(GRANT_FORBIDDEN, String.join(", ", ungrantable));
  }

  private static Set<String> added(@Nullable final Set<String> desired, @Nullable final Set<String> current) {
    if (desired == null || desired.isEmpty()) {
      return Collections.emptySet();
    }
    if (current == null || current.isEmpty()) {
      return new LinkedHashSet<>(desired);
    }
    Set<String> added = new LinkedHashSet<>(desired);
    added.removeAll(current);
    return added;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Permission permissionOf(final CPrivilege privilege) {
    for (PrivilegeDescriptor descriptor : privilegeDescriptors) {
      if (privilege.getType().equals(descriptor.getType())) {
        return descriptor.createPermission(privilege);
      }
    }
    return null;
  }
}
