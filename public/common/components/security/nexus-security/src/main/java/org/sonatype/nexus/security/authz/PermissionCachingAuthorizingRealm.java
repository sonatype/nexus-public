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
package org.sonatype.nexus.security.authz;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract {@link AuthorizingRealm} base that resolves a subject's permissions <em>once</em> per principal and caches
 * the result, instead of re-resolving every role on every permission check.
 *
 * <p>
 * Shiro's stock {@link AuthorizingRealm#isPermitted(Permission, AuthorizationInfo)} calls
 * {@link #getPermissions(AuthorizationInfo)} - which expands every role into its permission set - for <em>each</em>
 * permission checked. The UI permission load checks the entire privilege catalogue in a single batch, so for a user
 * backed by many roles/privileges this degrades to O(requested x granted) role resolutions and dominates login time
 * (NEXUS-53719, generalising the NEXUS-52583 cache that previously only lived in the local authorizing realm).
 *
 * <p>
 * Every Nexus realm that resolves roles into permissions - the local authorizing realm and the external realms
 * (SAML, LDAP, Crowd, OAuth2, ...) - should extend this class so they all share identical, cached authorization
 * behaviour. Subclasses supply the raw role/permission grants by overriding
 * {@link #doGetAuthorizationInfo(PrincipalCollection)}; this base owns the permission expansion, and each realm owns
 * its own {@link PrincipalPermissionsCache} instance (created from the shared {@link PrincipalPermissionsCacheFactory})
 * so realms never share entries or invalidation. Subclasses are responsible for their own authentication behaviour
 * ({@code supports} and {@code doGetAuthenticationInfo}).
 *
 * <p>
 * Authorization state for a principal is split across two per-realm caches, both invalidated centrally by
 * {@code RealmManagerImpl}: this realm's Shiro {@link AuthorizationInfo} (role) cache and this realm's
 * {@link PrincipalPermissionsCache}. This base therefore does not subscribe to authorization-change events itself.
 */
public abstract class PermissionCachingAuthorizingRealm
    extends AuthorizingRealm
{
  private final PrincipalPermissionsCache principalPermissionsCache;

  protected PermissionCachingAuthorizingRealm(
      final String realmName,
      final PrincipalPermissionsCacheFactory principalPermissionsCacheFactory)
  {
    super();
    // Set a stable/deterministic realm name here rather than relying on each subclass to call setName(...) after
    // super(...) (NEXUS-53719).
    setName(checkNotNull(realmName, "realmName"));
    setAuthorizationCachingEnabled(true);
    // Each realm owns its own permission cache instance (sized per realm); realms never share entries or invalidation.
    this.principalPermissionsCache = checkNotNull(principalPermissionsCacheFactory, "principalPermissionsCacheFactory")
        .create(realmName);
  }

  /**
   * Delegates to this realm's cache keyed (via {@link #permissionCacheKey}) off the <em>authorized</em> principals
   * (those passed by the authorizer), not the ambient subject - so a cross-principal check (e.g. an admin querying
   * another user's access) can neither read nor poison the acting subject's entry. Returns {@code false} without
   * caching when this realm grants nothing for the principals ({@code doGetAuthorizationInfo} returns {@code null}), so
   * realms that do not own a principal do not accumulate empty cache entries.
   */
  @Override
  public boolean isPermitted(final PrincipalCollection principals, final Permission permission) {
    AuthorizationInfo info = getAuthorizationInfo(principals);
    if (info == null) {
      return false;
    }
    return principalPermissionsCache.isPermitted(permissionCacheKey(principals, info), permission,
        () -> getPermissions(info));
  }

  /**
   * Batch variant (used by the UI's whole-catalogue permission load). Resolves the authorization info once, then
   * answers each permission from this realm's cache; the cache resolves the expanded permission set once for the batch.
   */
  @Override
  public boolean[] isPermitted(final PrincipalCollection principals, final List<Permission> permissions) {
    // Shiro contract: a null/empty list yields an empty result (also guards the permissions.size() NPE below).
    if (permissions == null || permissions.isEmpty()) {
      return new boolean[0];
    }
    AuthorizationInfo info = getAuthorizationInfo(principals);
    if (info == null) {
      return new boolean[permissions.size()]; // this realm grants nothing for these principals
    }
    return principalPermissionsCache.isPermitted(permissionCacheKey(principals, info), permissions,
        () -> getPermissions(info));
  }

  /**
   * The {@link PrincipalCollection} used as this realm's permission-cache key for {@code principals} (whose resolved
   * {@code info} is supplied so the key can reflect what actually determines the permissions); defaults to the
   * authorized principals themselves. A realm whose authorization depends on only part of the principal (e.g. an
   * account and its role, not a per-token id) may override this to collapse principals that resolve to the
   * <em>same</em> permissions onto a single cache entry. It must never merge principals with different effective
   * permissions, and the key must change whenever those permissions would: violating this silently serves one
   * principal's permissions for another until the entry's TTL expires (not merely until the next invalidation).
   */
  protected PrincipalCollection permissionCacheKey(final PrincipalCollection principals, final AuthorizationInfo info) {
    return principals;
  }

  /**
   * Invalidates this realm's permission cache. Called centrally by {@code RealmManagerImpl} on authorization-change
   * events, and directly by realms whose own special events (e.g. LDAP {@code LdapCacheInvalidatedEvent}) can change
   * this realm's resolved permissions. Because each realm owns its cache, this never disturbs any other realm.
   */
  public void invalidatePrincipalPermissions() {
    principalPermissionsCache.invalidateAll();
  }

  /**
   * Overrides Shiro's permission expansion to use a single batch DB query per BFS level
   * instead of one query per role, reducing cold-login cost from O(roles) DB round-trips
   * to O(depth) batch calls (NEXUS-52583).
   *
   * Shiro's {@code resolveRolePermissions} is private so we override the parent
   * {@code getPermissions(AuthorizationInfo)} which owns the role-expansion loop.
   *
   * Invoked at most once per principal per cache miss, via the supplier passed to
   * {@link PrincipalPermissionsCache#isPermitted} - not on every permission check - so subclasses need not optimise
   * for per-check invocation.
   */
  @Override
  protected Collection<Permission> getPermissions(final AuthorizationInfo info) {
    if (info == null) {
      return Collections.emptySet();
    }

    final Set<Permission> permissions = new LinkedHashSet<>();

    Collection<String> stringPerms = info.getStringPermissions();
    if (stringPerms != null) {
      for (String sp : stringPerms) {
        if (sp != null && !sp.isEmpty()) {
          permissions.add(getPermissionResolver().resolvePermission(sp));
        }
      }
    }

    Collection<Permission> objectPerms = info.getObjectPermissions();
    if (objectPerms != null) {
      permissions.addAll(objectPerms);
    }

    Collection<String> roleIds = info.getRoles();
    if (roleIds != null && !roleIds.isEmpty()) {
      // Resolve once so the instanceof check and the call below cannot observe two different resolver instances.
      RolePermissionResolver resolver = getRolePermissionResolver();
      if (resolver instanceof BatchRolePermissionResolver batchResolver) {
        // Batch-resolve all roles in one shot (avoids the internal-impl downcast; NEXUS-52583/NEXUS-53719).
        Collection<Permission> resolved = batchResolver.resolvePermissionsForRoles(roleIds);
        if (resolved != null) {
          permissions.addAll(resolved);
        }
      }
      else if (resolver != null) {
        for (String roleId : roleIds) {
          Collection<Permission> resolved = resolver.resolvePermissionsInRole(roleId);
          if (resolved != null) {
            permissions.addAll(resolved);
          }
        }
      }
    }

    return permissions.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(permissions);
  }
}
