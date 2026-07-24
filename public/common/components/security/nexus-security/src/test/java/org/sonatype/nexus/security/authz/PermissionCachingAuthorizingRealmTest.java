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

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.internal.RolePermissionResolverImpl;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link PermissionCachingAuthorizingRealm} base: permission expansion and delegation to this
 * realm's own {@link PrincipalPermissionsCache}. (Shiro authorization-cache invalidation is centralized in
 * {@code RealmManagerImpl}; expanded-permission cache invalidation is covered by
 * {@link PrincipalPermissionsCacheTest}.)
 */
class PermissionCachingAuthorizingRealmTest
{
  private static final Permission READ = new WildcardPermission("app:config:read");

  private static final Permission DELETE = new WildcardPermission("app:config:delete");

  /** Minimal concrete realm so the abstract base can be exercised directly; authz info is configurable per test. */
  private static final class TestRealm
      extends PermissionCachingAuthorizingRealm
  {
    private AuthorizationInfo authInfo;

    // Optional per-principal grants (keyed by primary principal) so a test can give distinct users distinct access.
    private final Map<Object, AuthorizationInfo> authInfoByPrincipal = new HashMap<>();

    TestRealm(final PrincipalPermissionsCache cache) {
      super("TestRealm", stubFactory(cache));
    }

    // Wraps a pre-built cache in a factory so the base ctor's factory.create(realmName) returns exactly that instance.
    private static PrincipalPermissionsCacheFactory stubFactory(final PrincipalPermissionsCache cache) {
      PrincipalPermissionsCacheFactory factory = mock(PrincipalPermissionsCacheFactory.class);
      when(factory.create(anyString())).thenReturn(cache);
      return factory;
    }

    // Lets a test drive doGetAuthorizationInfo(...) to throw, so failure propagation can be asserted.
    private RuntimeException authInfoFailure;

    void failAuthInfoWith(final RuntimeException failure) {
      this.authInfoFailure = failure;
    }

    void setAuthInfo(final AuthorizationInfo authInfo) {
      this.authInfo = authInfo;
    }

    void setAuthInfoFor(final Object primaryPrincipal, final AuthorizationInfo info) {
      authInfoByPrincipal.put(primaryPrincipal, info);
    }

    // When enabled, collapse the permission-cache key to the primary principal only (as ServiceAccount does with its
    // per-token principals), so principals sharing a primary principal share one cache entry.
    private boolean collapseByPrimaryPrincipal;

    void collapseByPrimaryPrincipal() {
      this.collapseByPrimaryPrincipal = true;
    }

    @Override
    protected PrincipalCollection permissionCacheKey(
        final PrincipalCollection principals,
        final AuthorizationInfo info)
    {
      return collapseByPrimaryPrincipal
          ? new SimplePrincipalCollection(principals.getPrimaryPrincipal(), getName())
          : super.permissionCacheKey(principals, info);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) {
      return null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(final PrincipalCollection principals) {
      if (authInfoFailure != null) {
        throw authInfoFailure;
      }
      AuthorizationInfo perPrincipal = authInfoByPrincipal.get(principals.getPrimaryPrincipal());
      return perPrincipal != null ? perPrincipal : authInfo;
    }
  }

  private static AuthorizationInfo authInfoWithStringPerm(final String permission) {
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    info.setStringPermissions(Set.of(permission));
    return info;
  }

  private static PrincipalPermissionsCache enabledCache() {
    return new PrincipalPermissionsCache(true, 250, Duration.ofMinutes(60), false, 16);
  }

  private static TestRealm realm() {
    return new TestRealm(enabledCache());
  }

  private static PrincipalPermissionsCache disabledCache() {
    return new PrincipalPermissionsCache(false, -1, Duration.ZERO, false, 1);
  }

  // ---- getPermissions ----

  @Test
  void getPermissionsReturnsEmptyForNullInfo() {
    assertTrue(realm().getPermissions(null).isEmpty());
  }

  @Test
  void getPermissionsReturnsEmptyForEmptyInfo() {
    // No string/object permissions and null roles -> every null-guard branch, empty result.
    assertTrue(realm().getPermissions(new SimpleAuthorizationInfo()).isEmpty());
  }

  @Test
  void getPermissionsIgnoresEmptyRoleSet() {
    // Non-null but empty role set exercises the !roleIds.isEmpty() == false branch.
    assertTrue(realm().getPermissions(new SimpleAuthorizationInfo(emptySet())).isEmpty());
  }

  @Test
  void getPermissionsResolvesStringPermissionsSkippingNullAndBlank() {
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    Set<String> stringPerms = new LinkedHashSet<>();
    stringPerms.add("app:config:read");
    stringPerms.add(""); // skipped (empty)
    stringPerms.add(null); // skipped (null)
    info.setStringPermissions(stringPerms);

    Collection<Permission> permissions = realm().getPermissions(info);

    assertTrue(permissions.contains(new WildcardPermission("app:config:read")));
    assertTrue(permissions.stream().allMatch(p -> p.implies(READ) || !p.implies(DELETE)));
  }

  @Test
  void getPermissionsIncludesObjectPermissions() {
    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    Permission objectPerm = new WildcardPermission("app:ui:read");
    info.setObjectPermissions(Set.of(objectPerm));

    assertTrue(realm().getPermissions(info).contains(objectPerm));
  }

  @Test
  void getPermissionsUsesBatchResolverForRolePermissionResolverImpl() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    realm.setRolePermissionResolver(resolver);

    Collection<Permission> permissions = realm.getPermissions(new SimpleAuthorizationInfo(Set.of("role")));

    assertTrue(permissions.contains(READ));
    verify(resolver).resolvePermissionsForRoles(any());
  }

  @Test
  void getPermissionsFallsBackToPerRoleResolver() {
    TestRealm realm = realm();
    RolePermissionResolver resolver = mock(RolePermissionResolver.class);
    when(resolver.resolvePermissionsInRole("granting")).thenReturn(List.of(READ));
    when(resolver.resolvePermissionsInRole("empty")).thenReturn(null); // resolved == null branch
    realm.setRolePermissionResolver(resolver);

    Set<String> roles = new LinkedHashSet<>();
    roles.add("granting");
    roles.add("empty");

    Collection<Permission> permissions = realm.getPermissions(new SimpleAuthorizationInfo(roles));

    assertTrue(permissions.contains(READ));
  }

  @Test
  void getPermissionsWithoutResolverIgnoresRoles() {
    TestRealm realm = realm();
    realm.setRolePermissionResolver(null);

    assertTrue(realm.getPermissions(new SimpleAuthorizationInfo(Set.of("role"))).isEmpty());
  }

  // ---- isPermitted delegation ----

  @Test
  void isPermittedDelegatesToCacheKeyedByPassedPrincipals() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(new SimpleAuthorizationInfo(Set.of("role")));

    // Keyed by the principals passed to the authorizer, NOT SecurityUtils.getSubject().
    PrincipalCollection principals = new SimplePrincipalCollection("alice", realm.getName());

    assertTrue(realm.isPermitted(principals, READ));
    assertFalse(realm.isPermitted(principals, DELETE));
  }

  @Test
  void isPermittedBatchKeyedByPassedPrincipals() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(new SimpleAuthorizationInfo(Set.of("role")));
    PrincipalCollection principals = new SimplePrincipalCollection("alice", realm.getName());

    boolean[] result = realm.isPermitted(principals, List.of(READ, DELETE));

    assertTrue(result[0]);
    assertFalse(result[1]);
  }

  @Test
  void isPermittedReturnsFalseWithoutResolvingWhenRealmDoesNotOwnPrincipal() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(null); // realm grants nothing for these principals
    PrincipalCollection principals = new SimplePrincipalCollection("stranger", "OtherRealm");

    assertFalse(realm.isPermitted(principals, READ));
    assertFalse(realm.isPermitted(principals, List.of(READ, DELETE))[0]);
    // Short-circuit on null info: no role expansion, no cache entry created.
    verify(resolver, never()).resolvePermissionsForRoles(any());
  }

  @Test
  void twoRealmsWithSeparateCachesDoNotClobberSamePrincipal() {
    // Each realm owns its OWN cache. Two realms authorize the SAME principal but grant DIFFERENT permissions; their
    // independent caches must keep the entries separate (Shiro ORs across realms, so the same principal legitimately
    // has different grants per realm).
    PrincipalCollection alice = new SimplePrincipalCollection("alice", "shared");

    RolePermissionResolverImpl resolverA = mock(RolePermissionResolverImpl.class);
    when(resolverA.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    TestRealm realmA = new TestRealm(enabledCache());
    realmA.setName("RealmA");
    realmA.setRolePermissionResolver(resolverA);
    realmA.setAuthInfo(new SimpleAuthorizationInfo(Set.of("roleA")));

    RolePermissionResolverImpl resolverB = mock(RolePermissionResolverImpl.class);
    when(resolverB.resolvePermissionsForRoles(any())).thenReturn(List.of(DELETE));
    TestRealm realmB = new TestRealm(enabledCache());
    realmB.setName("RealmB");
    realmB.setRolePermissionResolver(resolverB);
    realmB.setAuthInfo(new SimpleAuthorizationInfo(Set.of("roleB")));

    // RealmA grants READ (not DELETE); RealmB grants DELETE (not READ) - for the same principal.
    assertTrue(realmA.isPermitted(alice, READ));
    assertFalse(realmA.isPermitted(alice, DELETE));
    assertTrue(realmB.isPermitted(alice, DELETE));
    assertFalse(realmB.isPermitted(alice, READ));
  }

  @Test
  void permissionCacheKeyOverrideCollapsesDistinctPrincipalsOntoOneEntry() {
    // A realm may key its cache off part of the principal; distinct principal collections that map to the same key
    // (here, the same primary principal) must share one entry and expand permissions only once (NEXUS-53719).
    TestRealm realm = realm();
    realm.collapseByPrimaryPrincipal();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(new SimpleAuthorizationInfo(Set.of("role")));

    // Same account ("acct"), two different per-token principals -> one collapsed cache key.
    PrincipalCollection tokenA = new SimplePrincipalCollection(List.of("acct", "tokenA"), realm.getName());
    PrincipalCollection tokenB = new SimplePrincipalCollection(List.of("acct", "tokenB"), realm.getName());

    assertTrue(realm.isPermitted(tokenA, READ));
    assertTrue(realm.isPermitted(tokenB, READ));
    assertFalse(realm.isPermitted(tokenB, DELETE));
    // Expanded once for the account despite two distinct token principals.
    verify(resolver, times(1)).resolvePermissionsForRoles(any());
  }

  @Test
  void checkingOnePrincipalDoesNotPoisonAnotherPrincipalsEntry() {
    // Cross-principal check (e.g. an admin querying another user's access): the cache key is the PASSED principals,
    // never the ambient subject, so resolving bob's permissions must neither create nor alter alice's entry.
    TestRealm realm = realm();
    realm.setAuthInfoFor("alice", authInfoWithStringPerm("app:config:read")); // alice may READ, not DELETE
    realm.setAuthInfoFor("bob", authInfoWithStringPerm("app:config:delete")); // bob may DELETE, not READ

    PrincipalCollection alice = new SimplePrincipalCollection("alice", realm.getName());
    PrincipalCollection bob = new SimplePrincipalCollection("bob", realm.getName());

    // Check the "other user" bob first, so any leakage would land in the cache before alice is ever checked.
    assertTrue(realm.isPermitted(bob, DELETE));
    assertFalse(realm.isPermitted(bob, READ));

    // alice's own entry is unaffected by bob's check: she has READ, not DELETE.
    assertTrue(realm.isPermitted(alice, READ));
    assertFalse(realm.isPermitted(alice, DELETE));
  }

  @Test
  void invalidatePrincipalPermissionsClearsThisRealmsCache() {
    // Central hook (RealmManagerImpl) and realm-specific events (e.g. LDAP) both flush this realm's own cache.
    PrincipalPermissionsCache cache = mock(PrincipalPermissionsCache.class);
    TestRealm realm = new TestRealm(cache);

    realm.invalidatePrincipalPermissions();

    verify(cache).invalidateAll();
  }

  // ---- Shiro batch contract, failure propagation and disabled-cache path (NEXUS-53719 review) ----

  @Test
  void isPermittedBatchWithNullOrEmptyReturnsEmptyWithoutResolving() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(new SimpleAuthorizationInfo(Set.of("role")));
    PrincipalCollection principals = new SimplePrincipalCollection("alice", realm.getName());

    // Shiro's AuthorizingRealm contract: null/empty permissions yield boolean[0] and never resolve permissions.
    assertEquals(0, realm.isPermitted(principals, (List<Permission>) null).length);
    assertEquals(0, realm.isPermitted(principals, List.of()).length);
    verify(resolver, never()).resolvePermissionsForRoles(any());
  }

  @Test
  void isPermittedPropagatesAuthorizationInfoFailure() {
    TestRealm realm = realm();
    realm.failAuthInfoWith(new IllegalStateException("boom"));
    PrincipalCollection principals = new SimplePrincipalCollection("alice", realm.getName());

    // A failure resolving authorization info must propagate (not be swallowed) from both overloads.
    assertThrows(IllegalStateException.class, () -> realm.isPermitted(principals, READ));
    assertThrows(IllegalStateException.class, () -> realm.isPermitted(principals, List.of(READ, DELETE)));
  }

  @Test
  void isPermittedWithDisabledCacheStillEvaluatesCorrectlyAndReResolves() {
    TestRealm realm = new TestRealm(disabledCache());
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(List.of(READ));
    realm.setRolePermissionResolver(resolver);
    realm.setAuthInfo(new SimpleAuthorizationInfo(Set.of("role")));
    PrincipalCollection principals = new SimplePrincipalCollection("alice", realm.getName());

    // Correct answers on the disabled path ...
    assertTrue(realm.isPermitted(principals, READ));
    assertTrue(realm.isPermitted(principals, READ));
    assertFalse(realm.isPermitted(principals, DELETE));

    // ... and no caching: every check re-resolves (nothing is memoized when the shared cache is disabled).
    verify(resolver, times(3)).resolvePermissionsForRoles(any());
  }

  @Test
  void getPermissionsToleratesBatchResolverReturningNull() {
    TestRealm realm = realm();
    RolePermissionResolverImpl resolver = mock(RolePermissionResolverImpl.class);
    // Defensive: BatchRolePermissionResolver's contract says never-null, but a broken impl must not crash the
    // authorizer - the getPermissions null-guard is what this verifies (null is NOT a valid normal return).
    when(resolver.resolvePermissionsForRoles(any())).thenReturn(null);
    realm.setRolePermissionResolver(resolver);

    assertTrue(realm.getPermissions(new SimpleAuthorizationInfo(Set.of("role"))).isEmpty());
  }
}
