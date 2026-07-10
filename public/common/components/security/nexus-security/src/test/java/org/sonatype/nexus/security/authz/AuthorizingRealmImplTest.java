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

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.authz.AuthorizingRealmImplTest.AuthorizingRealmImplTestConfiguration;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.PreconfiguredSecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;
import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;
import org.sonatype.nexus.security.user.UserStatus;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

/**
 * Tests for {@link AuthorizingRealmImpl}.
 */
@Import(AuthorizingRealmImplTestConfiguration.class)
@TestPropertySource(properties = {"nexus.security.principal.permissions.cache.enabled=true"})
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
public class AuthorizingRealmImplTest
    extends AbstractSecurityTest
{
  protected AuthorizingRealmImpl realm;

  protected SecurityConfigurationManager configurationManager;

  protected SimplePrincipalCollection principal;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  protected static class AuthorizingRealmImplTestConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(new MemorySecurityConfiguration());
    }
  }

  @Override
  @BeforeEach
  protected void setUp() throws Exception {
    super.setUp();
    realm = (AuthorizingRealmImpl) lookup(Realm.class, AuthorizingRealmImpl.NAME);
    realm.setRolePermissionResolver(this.lookup(RolePermissionResolver.class));

    configurationManager = lookup(SecurityConfigurationManager.class);

    buildTestAuthorizationConfig();

    // Fails because the configuration requirement in nexus authorizing realm isn't initialized
    // thus NPE
    principal = new SimplePrincipalCollection("username", realm.getName());
    Subject mockSubject = mock(Subject.class);
    when(mockSubject.getPrincipals()).thenReturn(principal);

    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getSubject).thenReturn(mockSubject);
  }

  @Override
  @AfterEach
  public void tearDown() throws Exception {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
    super.tearDown();
  }

  @Test
  public void testAuthorization() {
    assertTrue(realm.hasRole(principal, "role"));

    // Verify the permission
    assertTrue(realm.isPermitted(principal, new WildcardPermission("app:config:read")));
    // Verify other method not allowed
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:config:create")));
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:config:update")));
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:config:delete")));

    // Verify other permission not allowed
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:ui:read")));
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:ui:create")));
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:ui:update")));
    assertFalse(realm.isPermitted(principal, new WildcardPermission("app:ui:delete")));
  }

  /**
   * Verifies that the principal-level cache (NEXUS-52583) prevents repeated role expansion:
   * PermissionsState is built at most once per principal, regardless of how many isPermitted
   * calls are made. The RolePermissionResolver spy lets us assert that the expensive DB
   * round-trip (resolvePermissionsInRole) runs at most once across all calls.
   */
  @Test
  public void testIsPermittedResultIsCached() throws Exception {
    RolePermissionResolver spyResolver = spy(lookup(RolePermissionResolver.class));
    realm.setRolePermissionResolver(spyResolver);

    WildcardPermission granted = new WildcardPermission("app:config:read");
    WildcardPermission denied = new WildcardPermission("app:config:delete");

    // First calls: PermissionsState is built (role expansion runs once)
    assertTrue(realm.isPermitted(principal, granted));
    assertFalse(realm.isPermitted(principal, denied));

    // Repeated calls: principal-level cache returns the same PermissionsState, so role
    // expansion must not run again regardless of how many isPermitted calls are made.
    for (int i = 0; i < 10; i++) {
      assertTrue(realm.isPermitted(principal, granted));
      assertFalse(realm.isPermitted(principal, denied));
    }

    // resolvePermissionsInRole called at most once — proves PermissionsState is built once,
    // not once per isPermitted call (NEXUS-52583 principal-level cache, not per-permission cache).
    verify(spyResolver, atMostOnce()).resolvePermissionsInRole(org.mockito.ArgumentMatchers.anyString());
  }

  /**
   * Verifies that an exact permission (no wildcard) takes the O(1) HashSet fast path
   * in PermissionsState.implies() rather than iterating the wildcard list.
   * Indirectly verified by confirming correctness — if the fast path were broken,
   * the granted exact permission would silently fall through to false.
   */
  @Test
  public void testImpliesFastPathForExactPermission() {
    WildcardPermission exact = new WildcardPermission("app:config:read");
    WildcardPermission wildcard = new WildcardPermission("app:*");

    // Exact match — should hit exactPermissions HashSet
    assertTrue(realm.isPermitted(principal, exact));
    // Wildcard request — exact fast path does not apply; wildcard loop handles it
    assertFalse(realm.isPermitted(principal, wildcard));
  }

  /**
   * Verifies that a comma-form multi-action privilege (e.g. "app:config:read,edit") correctly
   * implies each individual action (NEXUS-53512 regression for NEXUS-52583 fast-path bug).
   *
   * Before the fix, comma-form grants were placed in exactPermissions and never matched by
   * WildcardPermission.implies(), so every action in the grant was silently denied.
   */
  @Test
  public void testMultiActionPrivilegeImpliesEachAction() throws Exception {
    // Add a multi-action privilege to the existing role
    CPrivilege multiPriv = WildcardPrivilegeDescriptor.privilege("app:config:read,edit");
    configurationManager.createPrivilege(multiPriv);

    CRole role = configurationManager.newRole();
    role.setId("multi-action-role");
    role.setName("Multi-action role");
    role.setDescription("Has a comma-form multi-action privilege");
    role.addPrivilege(multiPriv.getId());
    configurationManager.createRole(role);

    CUser user = new MemoryCUser();
    user.setEmail("multiuser@foo");
    user.setFirstName("multi");
    user.setLastName("user");
    user.setStatus(UserStatus.active.toString());
    user.setId("multiuser");
    user.setPassword("password");
    Set<String> roles = new HashSet<>();
    roles.add(role.getId());
    configurationManager.createUser(user, roles);

    SimplePrincipalCollection multiPrincipal =
        new SimplePrincipalCollection("multiuser", realm.getName());
    Subject mockMultiSubject = mock(Subject.class);
    when(mockMultiSubject.getPrincipals()).thenReturn(multiPrincipal);
    securityUtilsMock.when(SecurityUtils::getSubject).thenReturn(mockMultiSubject);

    // Both actions implied by the comma-form grant must be permitted
    assertTrue(realm.isPermitted(multiPrincipal, new WildcardPermission("app:config:read")));
    assertTrue(realm.isPermitted(multiPrincipal, new WildcardPermission("app:config:edit")));
    // Actions not in the grant must be denied
    assertFalse(realm.isPermitted(multiPrincipal, new WildcardPermission("app:config:delete")));
    assertFalse(realm.isPermitted(multiPrincipal, new WildcardPermission("app:config:create")));
  }

  /**
   * Verifies that invalidating the cache (e.g. on auth config change) forces re-evaluation
   * so updated permissions take effect.
   */
  @Test
  public void testCacheInvalidatedOnAuthConfigChange() throws Exception {
    WildcardPermission granted = new WildcardPermission("app:config:read");

    // Warm up the cache
    assertTrue(realm.isPermitted(principal, granted));

    // Simulate an authorization configuration change event
    realm.on(new AuthorizationConfigurationChanged());

    // Result must still be correct after re-evaluation
    assertTrue(realm.isPermitted(principal, granted));
  }

  /**
   * Verifies that two principals are each cached independently and do not interfere.
   */
  @Test
  public void testCacheIsolatedPerPrincipal() throws Exception {
    // Reuse the existing role — only create the second user
    CUser otherUser = new MemoryCUser();
    otherUser.setEmail("other@foo");
    otherUser.setFirstName("other");
    otherUser.setLastName("user");
    otherUser.setStatus(UserStatus.active.toString());
    otherUser.setId("other_user");
    otherUser.setPassword("password");
    Set<String> roles = new HashSet<>();
    roles.add("role");
    configurationManager.createUser(otherUser, roles);

    SimplePrincipalCollection otherPrincipal =
        new SimplePrincipalCollection("other_user", realm.getName());
    Subject mockSubjectOther = mock(Subject.class);
    when(mockSubjectOther.getPrincipals()).thenReturn(otherPrincipal);

    WildcardPermission granted = new WildcardPermission("app:config:read");
    WildcardPermission denied = new WildcardPermission("app:ui:read");

    // Populate cache for original principal
    assertTrue(realm.isPermitted(principal, granted));
    assertFalse(realm.isPermitted(principal, denied));

    // Second principal must use its own cache entry and resolve correctly
    securityUtilsMock.when(SecurityUtils::getSubject).thenReturn(mockSubjectOther);
    assertTrue(realm.isPermitted(otherPrincipal, granted));
    assertFalse(realm.isPermitted(otherPrincipal, denied));
  }

  /**
   * Verifies the linearScan code path used when the principal permissions cache is disabled.
   * This is a separate inner class so it can carry its own @TestPropertySource.
   */
  @Import(AuthorizingRealmImplTestConfiguration.class)
  @TestPropertySource(properties = {"nexus.security.principal.permissions.cache.enabled=false"})
  @DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
  static class CacheDisabledTest
      extends AuthorizingRealmImplTest
  {
    @Test
    public void testAuthorizationWithCacheDisabled() {
      // Exact permission: granted
      assertTrue(realm.isPermitted(principal, new WildcardPermission("app:config:read")));
      // Exact permission: denied
      assertFalse(realm.isPermitted(principal, new WildcardPermission("app:config:create")));
      // Wildcard check: no granted permission implies app:config:* (exercises the wildcard loop)
      assertFalse(realm.isPermitted(principal, new WildcardPermission("app:config:*")));
    }
  }

  private void buildTestAuthorizationConfig() throws Exception {
    buildTestAuthorizationConfig("username");
  }

  private void buildTestAuthorizationConfig(final String userId) throws Exception {
    CPrivilege priv = WildcardPrivilegeDescriptor.privilege("app:config:read");
    configurationManager.createPrivilege(priv);

    CRole role = configurationManager.newRole();
    role.setId("role");
    role.setName("somerole");
    role.setDescription("somedescription");
    role.addPrivilege(priv.getId());

    configurationManager.createRole(role);

    CUser user = new MemoryCUser();
    user.setEmail("dummyemail@foo");
    user.setFirstName("dummyFirstName");
    user.setLastName("dummyLastName");
    user.setStatus(UserStatus.active.toString());
    user.setId(userId);
    user.setPassword("password");

    Set<String> roles = new HashSet<String>();
    roles.add(role.getId());

    configurationManager.createUser(user, roles);
  }
}
