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
package org.sonatype.nexus.plugins.defaultrole;

import javax.annotation.Nullable;
import org.sonatype.nexus.security.anonymous.AnonymousHelper;
import org.sonatype.nexus.security.authz.PermissionCachingAuthorizingRealm;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCacheFactory;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.sonatype.nexus.common.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Realm that adds the specified role to all authenticated users.
 *
 * <p>
 * Extends {@link PermissionCachingAuthorizingRealm}, so it uses the same cached, batch-resolved authorization as every
 * other authorizing realm (its own per-realm cache instance). Because the granted role is identical for every
 * (non-anonymous) principal, all principals resolve to the same permission set, so this realm collapses them onto a
 * single shared permission-cache entry (see {@link #permissionCacheKey}) rather than accumulating one identical entry
 * per user. The default role is mutable at runtime (via the capability), which posts
 * {@code AuthorizationConfigurationChanged} on change so both caches are invalidated immediately (NEXUS-53719).
 */
@Component
@Qualifier(DefaultRoleRealm.NAME)
@Description("Default Role Realm")
public class DefaultRoleRealm
    extends PermissionCachingAuthorizingRealm
{
  private static final Logger log = LoggerFactory.getLogger(DefaultRoleRealm.class);

  public static final String NAME = "DefaultRole";

  // Every non-anonymous principal is granted the same single default role, hence an identical expanded permission set.
  // Key this realm's permission cache off one shared, principal-independent entry so it holds a single datum instead of
  // one identical entry per user (which would thrash the LRU above nexus.authorizingrealm.permissionscache.maximumsize
  // and re-run the role expansion the cache exists to avoid). Safe because DefaultRoleCapability posts
  // AuthorizationConfigurationChanged on any role change, invalidating this entry immediately.
  private static final PrincipalCollection SHARED_PERMISSION_CACHE_KEY = new SimplePrincipalCollection("shared", NAME);

  // volatile: written by the capability activation/passivation thread (setRole) and read by request-handling threads
  // (doGetAuthorizationInfo) and the UI state thread (getRole). Without a happens-before edge a reader could observe a
  // stale value - including the initial null - and silently not apply the default role for that check.
  private volatile String role;

  @Autowired
  public DefaultRoleRealm(final PrincipalPermissionsCacheFactory principalPermissionsCacheFactory) {
    // Base ctor sets the stable Shiro realm name (used for realm identity / principal realm-name matching); this
    // realm's permission cache is its own per-realm instance, keyed by principals, not by realm name (NEXUS-53719).
    super(NAME, principalPermissionsCacheFactory);
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
    return maybeGrantRole(principals);
  }

  /**
   * All non-anonymous principals are granted the same default role and therefore the same permissions, so they share a
   * single cache entry. Reached only when {@code info != null} (the base skips caching otherwise), i.e. never for the
   * anonymous subject.
   */
  @Override
  protected PrincipalCollection permissionCacheKey(final PrincipalCollection principals, final AuthorizationInfo info) {
    return SHARED_PERMISSION_CACHE_KEY;
  }

  private AuthorizationInfo maybeGrantRole(final PrincipalCollection principals) {
    if (role != null) {
      // only attempt to apply default role if user is not anonymous
      if (!AnonymousHelper.isAnonymous(principals)) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRole(role);
        log.debug("Granting {} role to {}", role, principals);
        return info;
      }
    }

    return null;
  }

  /**
   * @throws UnsupportedOperationException Authentication is not supported
   */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) throws AuthenticationException {
    throw new UnsupportedOperationException();
  }

  @Nullable
  public String getRole() {
    return role;
  }

  public void setRole(@Nullable final String role) {
    this.role = role;
  }
}
