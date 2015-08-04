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
package org.sonatype.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.authorization.AuthorizationException;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.usermanagement.DefaultUser;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserStatus;

import junit.framework.Assert;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

public class DefaultSecuritySystemTest
    extends AbstractSecurityTest
{

  public void testLogin()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.login(token);
    Assert.assertNotNull(subject);

    try {
      securitySystem.login(new UsernamePasswordToken("jcoder", "INVALID"));
      Assert.fail("expected AuthenticationException");
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  public void testLogout()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();

    // bind to a servlet request/response
    // this.setupLoginContext( "test" );

    // login
    UsernamePasswordToken token = new UsernamePasswordToken("jcoder", "jcoder");
    Subject subject = securitySystem.login(token);
    Assert.assertNotNull(subject);

    // check the logged in user
    Subject loggedinSubject = securitySystem.getSubject();
    // Assert.assertEquals( subject.getSession().getId(), loggedinSubject.getSession().getId() );
    Assert.assertTrue(subject.isAuthenticated());
    Assert.assertTrue("Subject principal: " + loggedinSubject.getPrincipal() + " is not logged in",
        loggedinSubject.isAuthenticated());
    // now logout
    securitySystem.logout(loggedinSubject);

    // the current user should be null
    subject = securitySystem.getSubject();
    Assert.assertFalse(subject.isAuthenticated());
    Assert.assertFalse(loggedinSubject.isAuthenticated());
  }

  public void testAuthorization()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();
    PrincipalCollection principal = new SimplePrincipalCollection("jcool", "ANYTHING");
    try {
      securitySystem.checkPermission(principal, "INVALID-ROLE:*");
      Assert.fail("expected: AuthorizationException");
    }
    catch (AuthorizationException e) {
      // expected
    }

    securitySystem.checkPermission(principal, "test:read");

  }

  /*
   * FIXME: BROKEN
   */
  public void BROKENtestPermissionFromRole()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();
    PrincipalCollection principal = new SimplePrincipalCollection("jcool", "ANYTHING");

    securitySystem.checkPermission(principal, "from-role2:read");

  }

  public void testGetUser()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();
    User jcoder = securitySystem.getUser("jcoder", "MockUserManagerA");

    Assert.assertNotNull(jcoder);

  }

  public void testAuthorizationManager()
      throws Exception
  {
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

  public void testAddUser()
      throws Exception
  {
    SecuritySystem securitySystem = this.getSecuritySystem();

    User user = new DefaultUser();
    user.setEmailAddress("email@foo.com");
    user.setName("testAddUser");
    user.setSource("MockUserManagerA");
    user.setStatus(UserStatus.active);
    user.setUserId("testAddUser");

    user.addRole(new RoleIdentifier("default", "test-role1"));

    Assert.assertNotNull(securitySystem.addUser(user));
  }

  @Override
  protected void tearDown()
      throws Exception
  {
    this.getSecuritySystem().stop();

    super.tearDown();
  }

}
