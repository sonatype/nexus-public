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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.distributed.event.service.api.common.UserPasswordChangedDistributedEvent;
import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authc.UserPasswordChanged;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.MockAuthorizationManagerB;
import org.sonatype.nexus.security.internal.DefaultSecuritySystemTest.DefaultSecuritySystemTestConfiguration;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.session.SessionInvalidator;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultSecuritySystem}.
 */
@Import(DefaultSecuritySystemTestConfiguration.class)
public class DefaultSecuritySystemTest
    extends AbstractSecurityTest
{
  @MockitoBean
  protected SessionInvalidator sessionInvalidator;

  public static class DefaultSecuritySystemTestConfiguration
      extends BaseSecurityConfiguration
  {
    @Bean
    AuthorizationManager authorizationManager() {
      return new MockAuthorizationManagerB();
    }
  }

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    reset(lookup(EventManager.class));
  }

  @Override
  @AfterEach
  protected void tearDown() throws Exception {
    this.getSecuritySystem().stop();

    super.tearDown();
  }

  @Test
  void testLogin() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.getSubject();
    assertNotNull(subject);
    subject.login(token);

    try {
      subject.login(new UsernamePasswordToken("jcoder", "INVALID"));
      fail("expected AuthenticationException");
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  @Test
  void testLogout() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // bind to a servlet request/response
    // this.setupLoginContext( "test" );

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.getSubject();
    assertNotNull(subject);
    subject.login(token);

    // check the logged in user
    Subject loggedinSubject = securitySystem.getSubject();
    // assertEquals( subject.getSession().getId(), loggedinSubject.getSession().getId() );
    assertTrue(subject.isAuthenticated());
    assertTrue(loggedinSubject.isAuthenticated(),
        "Subject principal: " + loggedinSubject.getPrincipal() + " is not logged in");
    loggedinSubject.logout();

    // the current user should be null
    subject = securitySystem.getSubject();
    assertFalse(subject.isAuthenticated());
    assertFalse(loggedinSubject.isAuthenticated());
  }

  @Test
  void testAuthorization() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();
    PrincipalCollection principal = new SimplePrincipalCollection("jcool", "ANYTHING");
    try {
      securitySystem.checkPermission(principal, "INVALID-ROLE:*");
      fail("expected: AuthorizationException");
    }
    catch (AuthorizationException e) {
      // expected
    }

    securitySystem.checkPermission(principal, "test:read");
  }

  /*
   * FIXME: BROKEN
   */
  void BROKENtestPermissionFromRole() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();
    PrincipalCollection principal = new SimplePrincipalCollection("jcool", "ANYTHING");

    securitySystem.checkPermission(principal, "from-role2:read");
  }

  @Test
  void testGetUser() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();
    User jcoder = securitySystem.getUser("jcoder", "MockUserManagerA");

    assertNotNull(jcoder);
  }

  @Test
  void testAuthorizationManager() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    Set<Role> roles = securitySystem.listRoles("sourceB");
    assertEquals(2, roles.size());

    Map<String, Role> roleMap = new HashMap<String, Role>();
    for (Role role : roles) {
      roleMap.put(role.getRoleId(), role);
    }

    assertTrue(roleMap.containsKey("test-role1"));
    assertTrue(roleMap.containsKey("test-role2"));

    Role role1 = roleMap.get("test-role1");
    assertEquals("Role 1", role1.getName());

    assertTrue(role1.getPrivileges().contains("from-role1:read"));
    assertTrue(role1.getPrivileges().contains("from-role1:delete"));
  }

  @Test
  void testSearchRoles() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    Set<Role> roles = securitySystem.searchRoles("sourceB", "query");
    // Search is equal to listRoles for not LDAP sources
    assertEquals(securitySystem.listRoles(), roles);
  }

  @Test
  void testAddUser() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    User user = new User();
    user.setEmailAddress("email@foo.com");
    user.setName("testAddUser");
    user.setSource("MockUserManagerA");
    user.setStatus(UserStatus.active);
    user.setUserId("testAddUser");

    user.addRole(new RoleIdentifier("default", "test-role1"));

    assertNotNull(securitySystem.addUser(user, "test1234"));
  }

  @Test
  void testUpdateUser_changePasswordStatus() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    securitySystem.addUser(createUser("testUpdateUser", UserStatus.changepassword), "test1234");

    securitySystem.updateUser(createUser("testUpdateUser", UserStatus.disabled));

    verifyEventPosted(1, UserPrincipalsExpired.class, event -> {
      assertThat(event.getUserId(), is("testUpdateUser"));
    });
  }

  @Test
  void testChangePassword_AfterUserLogin() throws UserNotFoundException, NoSuchUserManagerException {
    SecuritySystem securitySystem = this.getSecuritySystem();
    Subject subject = securitySystem.getSubject();
    subject.login(new UsernamePasswordToken("jcoder", "jcoder"));

    // change my own
    securitySystem.changePassword("jcoder", "newpassword");

    verifyEventPosted(1, UserPasswordChanged.class, event -> {
      assertThat(event.isClearCache(), is(true));
      assertThat(event.getUserId(), is("jcoder"));
    });

    verifyEventPosted(1, UserPasswordChangedDistributedEvent.class, event -> {
      assertThat(event.isLocal(), is(true));
      assertThat(event.isClearCache(), is(true));
      assertThat(event.getUserId(), is("jcoder"));
    });

    // change another user's password
    assertThrows(AuthorizationException.class, () -> securitySystem.changePassword("fakeuser", "newpassword"),
        "jcoder is not permitted to change the password for fakeuser");
  }

  @Test
  void testChangePassword_selfChange_usesShiroRealmAsInvalidationSource() {
    SecuritySystem securitySystem = this.getSecuritySystem();
    Subject subject = securitySystem.getSubject();
    subject.login(new UsernamePasswordToken("jcoder", "jcoder"));

    assertDoesNotThrow(() -> securitySystem.changePassword("jcoder", "newpassword2"));

    // The invalidation source should be the Shiro realm name from the authenticated Subject,
    // not the UserManager "default" source of the internal user record.
    ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);
    verify(sessionInvalidator).invalidateSessionsForUser(eq("jcoder"), sourceCaptor.capture());
    assertThat(sourceCaptor.getValue(), not(is("default")));
    assertNotNull(sourceCaptor.getValue());
  }

  @Test
  void testDefaultSecuritySystem_SearchUsersPreservesOrder() throws Exception {
    // use a unique prefix so we can filter out pre-existing users added in test setup
    final String prefixForTest = "testDefaultSecuritySystem_SearchUsersPreservesOrder_";

    SecuritySystem securitySystem = this.getSecuritySystem();

    // Add users, the order we add them in should not get changed by code inside DefaultSecuritySystem->searchUsers
    securitySystem.addUser(createUser(prefixForTest + "delta", UserStatus.active), "password");
    securitySystem.addUser(createUser(prefixForTest + "alpha", UserStatus.active), "password");
    securitySystem.addUser(createUser(prefixForTest + "charlie", UserStatus.active), "password");
    securitySystem.addUser(createUser(prefixForTest + "bravo", UserStatus.active), "password");

    // Search for all users from this source
    final UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.setSource("MockUserManagerA");

    Set<User> result = securitySystem.searchUsers(criteria);

    assertTrue(result.size() >= 4, "Should have at least 4 users");
    List<String> ourUserIds = result.stream()
        .map(User::getUserId)
        .filter(id -> id.startsWith(prefixForTest))
        .toList();

    assertEquals(prefixForTest + "delta", ourUserIds.get(0), "First user should be user-delta");
    assertEquals(prefixForTest + "alpha", ourUserIds.get(1), "Second user should be user-alpha");
    assertEquals(prefixForTest + "charlie", ourUserIds.get(2), "Third user should be user-charlie");
    assertEquals(prefixForTest + "bravo", ourUserIds.get(3), "Fourth user should be user-bravo");
  }

  @Test
  void testSearchUsersWithSourcePreservesOrder() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // Add users in a specific order
    securitySystem.addUser(createUser("search-zebra", UserStatus.active), "password");
    securitySystem.addUser(createUser("search-apple", UserStatus.active), "password");
    securitySystem.addUser(createUser("search-mango", UserStatus.active), "password");

    // Search for users from specific source
    org.sonatype.nexus.security.user.UserSearchCriteria criteria =
        new org.sonatype.nexus.security.user.UserSearchCriteria();
    criteria.setSource("MockUserManagerA");

    Set<User> users = securitySystem.searchUsers(criteria);

    // Verify LinkedHashSet preserves insertion order from user manager
    assertNotNull(users);
    assertThat(users.getClass().getName(), is("java.util.LinkedHashSet"));
  }

  private static User createUser(final String name, final UserStatus status) {
    User user = new User();
    user.setEmailAddress("email@foo.com");
    user.setName(name);
    user.setSource("MockUserManagerA");
    user.setStatus(status);
    user.setUserId(name);

    user.addRole(new RoleIdentifier("default", "test-role1"));

    return user;
  }

  private <T> void verifyEventPosted(final int totalEvents, final Class<T> eventClass, final Consumer<T> assertions) {
    ArgumentCaptor<T> eventArgument = ArgumentCaptor.forClass(eventClass);
    verify(lookup(EventManager.class), times(totalEvents)).post(eventArgument.capture());
    assertions.accept(eventArgument.getValue());
  }
}
