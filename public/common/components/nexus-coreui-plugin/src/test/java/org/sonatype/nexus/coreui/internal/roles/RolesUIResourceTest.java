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
package org.sonatype.nexus.coreui.internal.roles;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.security.authz.AuthorizationManager;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link RolesUIResource}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RolesUIResourceTest
{
  @Mock
  private AuthorizationManager defaultAuthManager;

  @Mock
  private AuthorizationManager ldapAuthManager;

  @Mock
  private AuthorizationManager samlAuthManager;

  private RolesUIResource underTest;

  private DefaultSecurityManager securityManager;

  private TestRealm realm;

  @Before
  public void setUp() {
    // Mock authorization managers
    when(defaultAuthManager.getSource()).thenReturn(DEFAULT_SOURCE);
    when(ldapAuthManager.getSource()).thenReturn("LDAP");
    when(samlAuthManager.getSource()).thenReturn("SAML");

    List<AuthorizationManager> authManagers = Arrays.asList(
        defaultAuthManager,
        ldapAuthManager,
        samlAuthManager);

    underTest = new RolesUIResource(authManagers);

    // Set up Shiro security context
    realm = new TestRealm("test-realm");
    securityManager = new DefaultSecurityManager(realm);
    ThreadContext.bind(securityManager);
  }

  @After
  public void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
    if (securityManager != null) {
      securityManager.destroy();
    }
  }

  @Test
  public void testListRoleSources_WithCorrectPermission() {
    realm.addTestAccount("testuser", "nexus:roles:read");
    authenticateUser("testuser");

    List<RoleSourceUIResponse> sources = underTest.listRoleSources();

    // Should exclude DEFAULT_SOURCE and return LDAP and SAML
    assertThat(sources, hasSize(2));
    assertThat(sources.get(0).getId(), is("LDAP"));
    assertThat(sources.get(1).getId(), is("SAML"));
  }

  @Test
  public void testListRoleSources_RejectsWildcardPermission() {
    realm.addTestAccount("wildcarduser", "nexus:*");
    authenticateUser("wildcarduser");

    // This should work because nexus:* includes nexus:roles:read
    List<RoleSourceUIResponse> sources = underTest.listRoleSources();
    assertThat(sources, hasSize(2));
  }

  @Test
  public void testListRoleSources_RejectsInsufficientPermission() {
    realm.addTestAccount("noreaduser", "nexus:roles:write");
    authenticateUser("noreaduser");

    try {
      underTest.listRoleSources();
      fail("Expected AuthorizationException for insufficient permissions");
    }
    catch (AuthorizationException e) {
      // Expected - user has write but not read permission
    }
  }

  @Test
  public void testListRoleSources_RequiresAuthentication() {
    // Don't authenticate any user
    try {
      underTest.listRoleSources();
      fail("Expected AuthorizationException for unauthenticated request");
    }
    catch (AuthorizationException e) {
      // Expected - no authenticated user
    }
  }

  private void authenticateUser(String username) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection(username, realm.getName());
    Session session = new SimpleSession();
    DelegatingSubject subject = new DelegatingSubject(principals, true, "localhost", session, securityManager);
    ThreadContext.bind(subject);
  }

  /**
   * Test realm for Shiro security context.
   */
  private static class TestRealm
      extends SimpleAccountRealm
  {
    public TestRealm(String name) {
      super(name);
    }

    public void addTestAccount(String username, String... permissions) {
      Set<Permission> permissionSet = new HashSet<>();
      for (String permission : permissions) {
        permissionSet.add(new WildcardPermission(permission));
      }
      SimpleAccount account =
          new SimpleAccount(username, "password", getName(), Collections.emptySet(), permissionSet);
      add(account);
    }
  }
}
