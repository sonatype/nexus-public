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

import java.util.List;

import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;
import org.sonatype.nexus.security.authz.BatchRolePermissionResolver;
import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.security.authz.PrincipalPermissionsCacheFactory;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultRoleRealmTest
{
  private DefaultRoleRealm underTest;

  @Before
  public void setup() {
    underTest = new DefaultRoleRealm(new PrincipalPermissionsCacheFactory(false, -1, () -> Time.minutes(2), false, 1));
  }

  @Test
  public void testDoGetAuthorizationInfo_notConfigured() {
    underTest.setRole(null);

    AuthorizationInfo authorizationInfo = underTest.doGetAuthorizationInfo(principals("test"));
    assertThat(authorizationInfo, nullValue());
  }

  @Test
  public void testDoGetAuthorizationInfo_authenticatedUser() {
    underTest.setRole("default-role");

    AuthorizationInfo authorizationInfo = underTest.doGetAuthorizationInfo(principals("test"));
    assertThat(authorizationInfo, notNullValue());
    assertThat(authorizationInfo.getRoles(), is(singleton("default-role")));
  }

  @Test
  public void testDoGetAuthorizationInfo_anonymousUser() {
    underTest.setRole("default-role");

    AuthorizationInfo authorizationInfo = underTest.doGetAuthorizationInfo(principals("anonymous"));
    assertThat(authorizationInfo, nullValue());
  }

  @Test
  public void testIsPermitted_cachedThroughPermissionCache() {
    // Wiring check: with an ENABLED permission cache, the realm authorizes the default role through it and re-expands
    // once.
    BatchRolePermissionResolver resolver = mock(BatchRolePermissionResolver.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(new WildcardPermission("nexus:foo:read")));
    DefaultRoleRealm caching =
        new DefaultRoleRealm(new PrincipalPermissionsCacheFactory(true, 250, () -> Time.minutes(60), false, 16));
    caching.setRole("default-role");
    caching.setRolePermissionResolver(resolver);
    PrincipalCollection user = principals("user");

    assertThat(caching.isPermitted(user, new WildcardPermission("nexus:foo:read")), is(true));
    for (int i = 0; i < 5; i++) {
      // Assert on every hit: a regression that served stale-empty entries on the 2nd..N-th call must fail here, not
      // slip through on the times(1) check alone (matches the sibling OAuth2/ServiceAccount realm tests).
      assertThat(caching.isPermitted(user, new WildcardPermission("nexus:foo:read")), is(true));
    }
    assertThat(caching.isPermitted(user, new WildcardPermission("nexus:bar:read")), is(false));
    verify(resolver, times(1)).resolvePermissionsForRoles(any());
  }

  @Test
  public void testCacheInvalidation_afterRoleChange_permissionsUpdateImmediately() {
    // Regression guard for NEXUS-53867: the Shiro authz cache must be cleared when the DefaultRole
    // capability's configured role is updated, so the new role takes effect on the very next request
    // with no TTL wait.
    //
    // This test drives the DefaultRoleRealm directly (no capability, no event bus) to verify the
    // cache contract: after invalidatePrincipalPermissions() is called (the method
    // RealmManagerImpl.clearAuthzRealmCaches() invokes on each realm), a stale cached permission
    // is evicted and the next isPermitted() re-evaluates against the updated role.
    BatchRolePermissionResolver resolver = mock(BatchRolePermissionResolver.class);
    when(resolver.resolvePermissionsForRoles(any()))
        .thenReturn(List.of(new WildcardPermission("nexus:*:*"))); // nx-admin: all permissions
    DefaultRoleRealm realm =
        new DefaultRoleRealm(new PrincipalPermissionsCacheFactory(true, 250, () -> Time.minutes(60), false, 16));
    realm.setRole("nx-admin");
    realm.setRolePermissionResolver(resolver);

    PrincipalCollection user = principals("testuser");
    WildcardPermission adminPerm = new WildcardPermission("nexus:users:read");

    // Warm the cache: resolver called exactly once, permission granted.
    assertThat(realm.isPermitted(user, adminPerm), is(true));
    verify(resolver, times(1)).resolvePermissionsForRoles(any());

    // Role changes to nx-anonymous (no permissions), but cache has NOT been invalidated yet.
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of());
    realm.setRole("nx-anonymous");

    // Without invalidation the stale cache entry still grants the old nx-admin permission.
    // This is exactly the pre-fix behavior that NEXUS-53867 reported.
    assertThat(realm.isPermitted(user, adminPerm), is(true)); // stale - bug scenario
    verify(resolver, times(1)).resolvePermissionsForRoles(any()); // no re-evaluation yet

    // Invalidate: this is what DefaultRoleCapability.onActivate / onPassivate trigger through
    // eventManager.post(new AuthorizationConfigurationChanged()), which causes
    // RealmManagerImpl.clearAuthzRealmCaches() to call invalidatePrincipalPermissions() here.
    realm.invalidatePrincipalPermissions();

    // After invalidation the cache is cleared: the next check re-evaluates with the updated role
    // and correctly denies the permission - role change takes effect immediately, no TTL wait.
    assertThat(realm.isPermitted(user, adminPerm), is(false)); // fix confirmed
    verify(resolver, times(2)).resolvePermissionsForRoles(any()); // re-expanded exactly once
  }

  @Test
  public void testGetName_isStableRealmName() {
    // The realm name must be the stable NAME constant (Shiro realm identity / principal realm-name matching, not a
    // cache key), not Shiro's default (class name + instance counter).
    assertThat(underTest.getName(), is(DefaultRoleRealm.NAME));
  }

  @Test
  public void testGetRole_returnsConfiguredRole() {
    underTest.setRole("default-role");
    assertThat(underTest.getRole(), is("default-role"));
  }

  @Test
  public void testDoGetAuthenticationInfo_notSupported() {
    // This realm only authorizes; authentication must be rejected.
    assertThrows(UnsupportedOperationException.class,
        () -> underTest.doGetAuthenticationInfo(new UsernamePasswordToken("user", "pass")));
  }

  private static PrincipalCollection principals(final String userId) {
    if ("anonymous".equals(userId)) {
      return new AnonymousPrincipalCollection(userId, "realm");
    }
    return new SimplePrincipalCollection(userId, "realm");
  }
}
