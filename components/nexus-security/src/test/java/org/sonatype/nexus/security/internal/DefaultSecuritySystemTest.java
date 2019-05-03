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

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.UserPrincipalsExpired;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.MockAuthorizationManagerB;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserStatus;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultSecuritySystem}.
 */
public class DefaultSecuritySystemTest
    extends AbstractSecurityTest
{
  @Mock
  EventManager eventManager;

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(AuthorizationManager.class)
            .annotatedWith(Names.named("sourceB"))
            .to(MockAuthorizationManagerB.class)
            .in(Singleton.class);
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    this.getSecuritySystem().stop();

    super.tearDown();
  }

  @Override
  public EventManager getEventManager() {
    return eventManager;
  }

  @Test
  public void testLogin() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.getSubject();
    Assert.assertNotNull(subject);
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
  public void testLogout() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // bind to a servlet request/response
    // this.setupLoginContext( "test" );

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.getSubject();
    Assert.assertNotNull(subject);
    subject.login(token);

    // check the logged in user
    Subject loggedinSubject = securitySystem.getSubject();
    // Assert.assertEquals( subject.getSession().getId(), loggedinSubject.getSession().getId() );
    Assert.assertTrue(subject.isAuthenticated());
    Assert.assertTrue("Subject principal: " + loggedinSubject.getPrincipal() + " is not logged in",
        loggedinSubject.isAuthenticated());
    loggedinSubject.logout();

    // the current user should be null
    subject = securitySystem.getSubject();
    Assert.assertFalse(subject.isAuthenticated());
    Assert.assertFalse(loggedinSubject.isAuthenticated());
  }

  @Test
  public void testAuthorization() throws Exception {
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
  public void BROKENtestPermissionFromRole() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();
    PrincipalCollection principal = new SimplePrincipalCollection("jcool", "ANYTHING");

    securitySystem.checkPermission(principal, "from-role2:read");
  }

  @Test
  public void testGetUser() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();
    User jcoder = securitySystem.getUser("jcoder", "MockUserManagerA");

    Assert.assertNotNull(jcoder);
  }

  @Test
  public void testAuthorizationManager() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    Set<Role> roles = securitySystem.listRoles("sourceB");
    Assert.assertEquals(2, roles.size());

    Map<String, Role> roleMap = new HashMap<String, Role>();
    for (Role role : roles) {
      roleMap.put(role.getRoleId(), role);
    }

    Assert.assertTrue(roleMap.containsKey("test-role1"));
    Assert.assertTrue(roleMap.containsKey("test-role2"));

    Role role1 = roleMap.get("test-role1");
    Assert.assertEquals("Role 1", role1.getName());

    Assert.assertTrue(role1.getPrivileges().contains("from-role1:read"));
    Assert.assertTrue(role1.getPrivileges().contains("from-role1:delete"));
  }

  @Test
  public void testAddUser() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    User user = new User();
    user.setEmailAddress("email@foo.com");
    user.setName("testAddUser");
    user.setSource("MockUserManagerA");
    user.setStatus(UserStatus.active);
    user.setUserId("testAddUser");

    user.addRole(new RoleIdentifier("default", "test-role1"));

    Assert.assertNotNull(securitySystem.addUser(user, "test123"));
  }

  @Test
  public void testUpdateUser_changePasswordStatus() throws Exception {
    SecuritySystem securitySystem = this.getSecuritySystem();

    securitySystem.addUser(createUser("testUpdateUser", UserStatus.changepassword), "test123");

    securitySystem.updateUser(createUser("testUpdateUser", UserStatus.disabled));

    boolean foundExpiredEvent = false;
    ArgumentCaptor<Object> eventArgument = ArgumentCaptor.forClass(Object.class);
    verify(eventManager, times(2)).post(eventArgument.capture());
    for (Object argValue : eventArgument.getAllValues()) {
      if (argValue instanceof UserPrincipalsExpired) {
        UserPrincipalsExpired expired = (UserPrincipalsExpired) argValue;
        assertThat(expired.getUserId(), is("testUpdateUser"));
        foundExpiredEvent = true;
      }
    }

    if (!foundExpiredEvent) {
      fail("UserPrincipalsExpired event was not fired");
    }
  }

  private User createUser(String name, UserStatus status) {
    User user = new User();
    user.setEmailAddress("email@foo.com");
    user.setName(name);
    user.setSource("MockUserManagerA");
    user.setStatus(status);
    user.setUserId(name);

    user.addRole(new RoleIdentifier("default", "test-role1"));

    return user;
  }
}
