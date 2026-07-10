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
package org.sonatype.nexus.security.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.Description;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.distributed.event.service.api.common.AuthorizationChangedDistributedEvent;
import org.sonatype.nexus.security.authz.AuthorizationConfigurationChanged;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.RoleMappingUserManager;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.crypto.hash.Sha1Hash;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_EXPIRE_AFTER_ACCESS_MINUTES_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_EXPIRE_AFTER_WRITE_MINUTES_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS_NAMED_VALUE;

/**
 * Default {@link AuthorizingRealm}.
 *
 * This realm ONLY handles authorization.
 */
@Component
@Qualifier(AuthorizingRealmImpl.NAME)
@Description("Local Authorizing Realm")
public class AuthorizingRealmImpl
    extends AuthorizingRealm
    implements Realm, EventAware
{
  private static final Logger logger = LoggerFactory.getLogger(AuthorizingRealmImpl.class);

  public static final String NAME = "NexusAuthorizingRealm";

  private final RealmSecurityManager realmSecurityManager;

  private final UserManager userManager;

  private final Map<String, UserManager> userManagerMap;

  /**
   * Bundles the expanded permission collection for a principal with indexed lookups.
   * Separates exact permissions (no wildcards) for O(1) HashSet lookup from wildcard
   * permissions that need iterative matching (NEXUS-52583 optimization).
   */
  private static final class PermissionsState
  {
    // Exact permission strings (lowercase, no wildcards) for O(1) lookup
    final Set<String> exactPermissions;

    // Permissions containing wildcards that need iterative matching
    final List<Permission> wildcardPermissions;

    // Result cache for computed checks: (Permission -> Boolean)
    final ConcurrentHashMap<Permission, Boolean> results = new ConcurrentHashMap<>();

    PermissionsState(final Collection<Permission> permissions) {
      this.exactPermissions = new HashSet<>();
      this.wildcardPermissions = new ArrayList<>();

      for (Permission perm : permissions) {
        if (perm instanceof WildcardPermission) {
          String permStr = perm.toString().toLowerCase();
          if (permStr.contains("*") || permStr.contains(",")) {
            // Wildcard or comma-separated multi-action permission — needs iterative matching.
            // Comma-form grants like "nexus:settings:read,update" must go through
            // WildcardPermission.implies() so individual actions are correctly evaluated.
            wildcardPermissions.add(perm);
          }
          else {
            // Exact single-action permission — O(1) HashSet lookup is safe.
            exactPermissions.add(permStr);
          }
        }
        else {
          // Non-WildcardPermission - treat as wildcard for safety
          wildcardPermissions.add(perm);
        }
      }
    }

    /**
     * Check if this state implies the given permission using optimized lookup.
     */
    boolean implies(final Permission permission) {
      // Fast path: exact match for non-wildcard permissions
      if (permission instanceof WildcardPermission) {
        String permStr = permission.toString().toLowerCase();
        if (!permStr.contains("*") && exactPermissions.contains(permStr)) {
          return true;
        }
      }

      // Slow path: check wildcard permissions
      for (Permission wp : wildcardPermissions) {
        if (wp.implies(permission)) {
          return true;
        }
      }
      return false;
    }
  }

  private final Cache<PrincipalCollection, PermissionsState> principalPermissionsCache;

  private final boolean principalPermissionsCacheEnabled;

  @Autowired
  public AuthorizingRealmImpl(
      final RealmSecurityManager realmSecurityManager,
      final UserManager userManager,
      final List<UserManager> userManagerList,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_ENABLED_NAMED_VALUE) final boolean principalPermissionsCacheEnabled,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_MAXIMUM_SIZE_NAMED_VALUE) final int cacheMaximumSize,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_EXPIRE_AFTER_WRITE_MINUTES_NAMED_VALUE) final int cacheExpireAfterWriteMinutes,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_EXPIRE_AFTER_ACCESS_MINUTES_NAMED_VALUE) final int cacheExpireAfterAccessMinutes,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_RECORD_STATS_NAMED_VALUE) final boolean cacheRecordStats,
      @Value(PRINCIPAL_PERMISSIONS_CACHE_CONCURRENCY_LEVEL_NAMED_VALUE) final int cacheConcurrencyLevel)
  {
    this.realmSecurityManager = realmSecurityManager;
    this.userManager = userManager;
    this.userManagerMap = QualifierUtil.buildQualifierBeanMap(userManagerList);
    this.principalPermissionsCacheEnabled = principalPermissionsCacheEnabled;

    // Build cache with configurable parameters
    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
        .concurrencyLevel(cacheConcurrencyLevel)
        .expireAfterWrite(cacheExpireAfterWriteMinutes, TimeUnit.MINUTES)
        .expireAfterAccess(cacheExpireAfterAccessMinutes, TimeUnit.MINUTES)
        .softValues();

    // Apply maximum size if configured (use -1 to disable size limit)
    if (cacheMaximumSize > 0) {
      cacheBuilder.maximumSize(cacheMaximumSize);
    }

    if (cacheRecordStats) {
      cacheBuilder.recordStats();
    }

    this.principalPermissionsCache = cacheBuilder.<PrincipalCollection, PermissionsState>build();

    HashedCredentialsMatcher credentialsMatcher = new HashedCredentialsMatcher();
    credentialsMatcher.setHashAlgorithmName(Sha1Hash.ALGORITHM_NAME);
    setCredentialsMatcher(credentialsMatcher);
    setName(NAME);
    setAuthenticationCachingEnabled(false); // we authz only, no authc done by this realm
    setAuthorizationCachingEnabled(true);
  }

  @Override
  public boolean supports(final AuthenticationToken token) {
    return false;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) {
    return null;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    if (principals == null) {
      throw new AuthorizationException("Cannot authorize with no principals.");
    }

    String username = principals.getPrimaryPrincipal().toString();
    Set<String> roles = new HashSet<String>();

    Set<String> realmNames = new HashSet<String>(principals.getRealmNames());

    // if the user belongs to this realm, we are most likely using this realm stand alone, or for testing
    if (!realmNames.contains(this.getName())) {
      // make sure the realm is enabled
      Collection<Realm> configureadRealms = realmSecurityManager.getRealms();
      boolean foundRealm = false;
      for (Realm realm : configureadRealms) {
        if (realmNames.contains(realm.getName())) {
          foundRealm = true;
          break;
        }
      }
      if (!foundRealm) {
        // user is from a realm that is NOT enabled
        throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
            + " belongs to a disabled realm(s): " + principals.getRealmNames() + ".");
      }
    }

    // clean up the realm names for processing (replace the Nexus*Realm with default)
    cleanUpRealmList(realmNames);

    if (RoleMappingUserManager.class.isInstance(userManager)) {
      for (String realmName : realmNames) {
        try {
          for (RoleIdentifier roleIdentifier : ((RoleMappingUserManager) userManager).getUsersRoles(username,
              realmName)) {
            roles.add(roleIdentifier.getRoleId());
          }
        }
        catch (UserNotFoundException e) {
          logger.trace("Failed to find role mappings for user: {} realm: {}", username, realmName);
        }
      }
    }
    else if (realmNames.contains("default")) {
      try {
        for (RoleIdentifier roleIdentifier : userManager.getUser(username).getRoles()) {
          roles.add(roleIdentifier.getRoleId());
        }
      }
      catch (UserNotFoundException e) {
        throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
            + " could not be found.", e);
      }

    }
    else
    // user not managed by this Realm
    {
      throw new AuthorizationException("User for principals: " + principals.getPrimaryPrincipal()
          + " not manged by Nexus realm.");
    }

    return new SimpleAuthorizationInfo(roles);
  }

  private void cleanUpRealmList(final Set<String> realmNames) {
    for (UserManager userManager : this.userManagerMap.values()) {
      String authRealmName = userManager.getAuthenticationRealmName();
      if (authRealmName != null && realmNames.contains(authRealmName)) {
        realmNames.remove(authRealmName);
        realmNames.add(userManager.getSource());
      }
    }

    if (realmNames.contains(getName())) {
      realmNames.remove(getName());
      realmNames.add("default");
    }
  }

  @Override
  protected boolean isPermitted(final Permission permission, final AuthorizationInfo info) {
    if (!principalPermissionsCacheEnabled) {
      return linearScan(permission, this.getPermissions(info));
    }

    PrincipalCollection principals = SecurityUtils.getSubject().getPrincipals();
    // cache.get(key, loader) is atomic: at most one thread builds the PermissionsState per
    // principal under concurrent login bursts, preventing double DB queries (NEXUS-52583).
    final PermissionsState state;
    try {
      state = principalPermissionsCache.get(principals, () -> new PermissionsState(this.getPermissions(info)));
    }
    catch (ExecutionException e) {
      // Loader threw an unexpected checked exception; evaluate without caching.
      return linearScan(permission, this.getPermissions(info));
    }
    // computeIfAbsent is atomic: the permission check runs at most once per (principal, permission) pair.
    // Subsequent isPermitted calls for the same pair are O(1) map lookups.
    // The PermissionsState.implies() method uses an optimized lookup with HashSet for exact matches
    // and only iterates wildcard permissions when needed (NEXUS-52583 optimization).
    return state.results.computeIfAbsent(permission, state::implies);
  }

  /**
   * Overrides Shiro's permission expansion to use a single batch DB query per BFS level
   * instead of one query per role, reducing cold-login cost from O(roles) DB round-trips
   * to O(depth) batch calls (NEXUS-52583).
   *
   * Shiro's {@code resolveRolePermissions} is private so we override the parent
   * {@code getPermissions(AuthorizationInfo)} which owns the role-expansion loop.
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
      if (getRolePermissionResolver() instanceof RolePermissionResolverImpl) {
        permissions.addAll(
            ((RolePermissionResolverImpl) getRolePermissionResolver()).resolvePermissionsForRoles(roleIds));
      }
      else {
        for (String roleId : roleIds) {
          Collection<Permission> resolved = getRolePermissionResolver().resolvePermissionsInRole(roleId);
          if (resolved != null) {
            permissions.addAll(resolved);
          }
        }
      }
    }

    return permissions.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(permissions);
  }

  // Only reached when principalPermissionsCacheEnabled=false (opt-out via config).
  // With the cache enabled (the default) this method is unreachable from isPermitted().
  private static boolean linearScan(final Permission permission, final Collection<Permission> userPermissions) {
    if (userPermissions == null || userPermissions.isEmpty()) {
      return false;
    }
    for (Permission perm : userPermissions) {
      if (perm.implies(permission)) {
        return true;
      }
    }
    return false;
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AuthorizationConfigurationChanged event) {
    invalidatePermissionsCache();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final SecurityContributionChangedEvent event) {
    invalidatePermissionsCache();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void on(final AuthorizationChangedDistributedEvent event) {
    if (EventHelper.isReplicating()) {
      invalidatePermissionsCache();
    }
  }

  private void invalidatePermissionsCache() {
    principalPermissionsCache.invalidateAll();
    logger.debug("Principal permissions cache invalidated");
  }
}
