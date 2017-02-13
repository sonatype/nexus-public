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
package org.sonatype.security.ldap.usermanagement;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.guice.SecurityModule;
import org.sonatype.security.ldap.LdapConstants;
import org.sonatype.security.ldap.LdapTestSupport;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserNotFoundException;
import org.sonatype.security.usermanagement.UserNotFoundTransientException;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import com.google.inject.Module;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LdapUserManagerIT
    extends LdapTestSupport
{

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Override
  protected Module[] getTestCustomModules() {
    return new Module[]{new SecurityModule()};
  }

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();

    copyResourceToFile("/test-conf/conf/security-users-in-both-realms.xml", getNexusSecurityConfiguration());
    copyResourceToFile("/test-conf/conf/security-configuration.xml", getSecurityConfiguration());
  }

  private SecuritySystem getSecuritySystem()
      throws Exception
  {
    return this.lookup(SecuritySystem.class);
  }

  private UserManager getUserManager()
      throws Exception
  {
    return this.lookup(UserManager.class, LdapConstants.USER_SOURCE);
  }

  @Test
  public void testGetUserFromUserManager()
      throws Exception
  {

    SecuritySystem securitySystem = this.getSecuritySystem();
    securitySystem.start();
    User user = securitySystem.getUser("cstamas");
    Assert.assertNotNull(user);
    Assert.assertEquals("cstamas", user.getUserId());
    Assert.assertEquals("cstamas@sonatype.com", user.getEmailAddress());
    Assert.assertEquals("Tamas Cservenak", user.getName());

    Set<String> roleIds = this.getUserRoleIds(user);
    Assert.assertTrue(roleIds.contains("repoconsumer")); // from LDAP
    Assert.assertTrue(roleIds.contains("developer")); // FROM LDAP and XML
    Assert.assertTrue(roleIds.contains("anonymous")); // FROM XML
    Assert.assertTrue(roleIds.contains("nx-developer"));
    Assert.assertEquals("Expected 4 roles; found " + roleIds.size() + ": " + roleIds, 4, roleIds.size());
  }

  @Test
  public void testGetUserFromLocator()
      throws Exception
  {
    Assert.assertNotNull(this.lookup(LdapConfiguration.class));

    UserManager userLocator = this.getUserManager();
    User user = userLocator.getUser("cstamas");
    Assert.assertNotNull(user);
    Assert.assertEquals("cstamas", user.getUserId());
    Assert.assertEquals("cstamas@sonatype.com", user.getEmailAddress());
    Assert.assertEquals("Tamas Cservenak", user.getName());
  }

  @Test
  public void testGetUserFailsLdapDown()
      throws Exception
  {
    UserManager userManager = this.getUserManager();
    getLdapServer().stop();
    expectedException.expect(UserNotFoundTransientException.class);
    userManager.getUser("cstamas");
  }

  @Test
  public void testGetUserFailsUserDoesNotExist()
      throws Exception
  {
    UserManager userManager = this.getUserManager();
    expectedException.expect(UserNotFoundException.class);
    userManager.getUser("not_a_user");
  }

  @Test
  public void testGetUserIds()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();
    Set<String> userIds = userLocator.listUserIds();
    Assert.assertTrue(userIds.contains("cstamas"));
    Assert.assertTrue(userIds.contains("brianf"));
    Assert.assertTrue(userIds.contains("jvanzyl"));
    Assert.assertTrue(userIds.contains("jdcasey"));
    Assert.assertEquals("Ids: " + userIds, 4, userIds.size());
  }

  @Test
  public void testSearch()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();
    Set<User> users = userLocator.searchUsers(new UserSearchCriteria("j"));

    Assert.assertNotNull(this.getById(users, "jvanzyl"));
    Assert.assertNotNull(this.getById(users, "jdcasey"));
    Assert.assertEquals("Users: " + users, 2, users.size());
  }

  @Test
  public void testEffectiveSearch()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();

    Set<String> allRoleIds = new HashSet<String>();
    for (Role role : this.getSecuritySystem().listRoles()) {
      allRoleIds.add(role.getRoleId());
    }

    UserSearchCriteria criteria = new UserSearchCriteria("j", allRoleIds, null);

    Set<User> users = userLocator.searchUsers(criteria);

    Assert.assertNotNull(this.getById(users, "jvanzyl"));
    Assert.assertEquals("Users: " + users, 1, users.size());
  }

  @Test
  public void testGetUsers()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();
    Set<User> users = userLocator.listUsers();

    User cstamas = this.getById(users, "cstamas");
    Assert.assertEquals("cstamas", cstamas.getUserId());
    Assert.assertEquals("cstamas@sonatype.com", cstamas.getEmailAddress());
    Assert.assertEquals("Tamas Cservenak", cstamas.getName());

    User brianf = this.getById(users, "brianf");
    Assert.assertEquals("brianf", brianf.getUserId());
    Assert.assertEquals("brianf@sonatype.com", brianf.getEmailAddress());
    Assert.assertEquals("Brian Fox", brianf.getName());

    User jvanzyl = this.getById(users, "jvanzyl");
    Assert.assertEquals("jvanzyl", jvanzyl.getUserId());
    Assert.assertEquals("jvanzyl@sonatype.com", jvanzyl.getEmailAddress());
    Assert.assertEquals("Jason Van Zyl", jvanzyl.getName());

    User jdcasey = this.getById(users, "jdcasey");
    Assert.assertEquals("jdcasey", jdcasey.getUserId());
    Assert.assertEquals("jdcasey@sonatype.com", jdcasey.getEmailAddress());
    Assert.assertEquals("John Casey", jdcasey.getName());

    Assert.assertEquals("Ids: " + users, 4, users.size());
  }

  private User getById(Set<User> users, String userId) {
    for (User User : users) {
      if (User.getUserId().equals(userId)) {
        return User;
      }
    }
    Assert.fail("Failed to find user: " + userId + " in list.");
    return null;
  }

  private Set<String> getUserRoleIds(User user) {
    Set<String> roleIds = new HashSet<String>();
    for (RoleIdentifier role : user.getRoles()) {
      roleIds.add(role.getRoleId());
    }
    return roleIds;
  }

  @Test
  public void testOrderOfUserSearch()
      throws Exception
  {
    IOUtils.copy(getClass().getResourceAsStream("/test-conf/conf/security-users-in-both-realms.xml"),
        new FileOutputStream(getNexusSecurityConfiguration()));

    SecuritySystem securitySystem = this.getSecuritySystem();
    securitySystem.start();

    List<String> realms = new ArrayList<String>();
    realms.add("XmlAuthenticatingRealm");
    realms.add(LdapConstants.REALM_NAME);

    securitySystem.setRealms(realms);

    // the user developer is in both realms, we need to make sure the order is honored
    User user = securitySystem.getUser("brianf");
    Assert.assertEquals("default", user.getSource());

    realms.clear();
    realms.add(LdapConstants.REALM_NAME);
    realms.add("XmlAuthenticatingRealm");
    securitySystem.setRealms(realms);

    // now the user should belong to the LDAP realm

    user = securitySystem.getUser("brianf");
    Assert.assertEquals("LDAP", user.getSource());

  }
}
