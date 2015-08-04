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
package org.sonatype.security.locators;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.sonatype.security.AbstractSecurityTestCase;
import org.sonatype.security.realms.tools.StaticSecurityResource;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import junit.framework.Assert;

public class SecurityXmlUserLocatorTest
    extends AbstractSecurityTestCase
{
  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(StaticSecurityResource.class).annotatedWith(Names.named("mock")).to(MockStaticSecurityResource.class);
  }

  public UserManager getUserManager()
      throws Exception
  {
    return (UserManager) this.lookup(UserManager.class);
  }

  public void testListUserIds()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();

    Set<String> userIds = userLocator.listUserIds();
    Assert.assertTrue(userIds.contains("test-user"));
    Assert.assertTrue(userIds.contains("anonymous"));
    Assert.assertTrue(userIds.contains("admin"));

    Assert.assertEquals(4, userIds.size());
  }

  public void testListUsers()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();

    Set<User> users = userLocator.listUsers();
    Map<String, User> userMap = this.toUserMap(users);

    Assert.assertTrue(userMap.containsKey("test-user"));
    Assert.assertTrue(userMap.containsKey("anonymous"));
    Assert.assertTrue(userMap.containsKey("admin"));

    Assert.assertEquals(4, users.size());
  }

  public void testGetUser()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();
    User testUser = userLocator.getUser("test-user");

    Assert.assertEquals("Test User", testUser.getName());
    Assert.assertEquals("test-user", testUser.getUserId());
    Assert.assertEquals("changeme1@yourcompany.com", testUser.getEmailAddress());

    // test roles
    Map<String, RoleIdentifier> roleMap = this.toRoleMap(testUser.getRoles());

    Assert.assertTrue(roleMap.containsKey("role1"));
    Assert.assertTrue(roleMap.containsKey("role2"));
    Assert.assertEquals(2, roleMap.size());
  }

  public void testGetUserWithEmptyRole()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();
    User testUser = userLocator.getUser("test-user-with-empty-role");

    Assert.assertEquals("Test User With Empty Role", testUser.getName());
    Assert.assertEquals("test-user-with-empty-role", testUser.getUserId());
    Assert.assertEquals("empty-role@yourcompany.com", testUser.getEmailAddress());

    // test roles
    Map<String, RoleIdentifier> roleMap = this.toRoleMap(testUser.getRoles());

    Assert.assertTrue(roleMap.containsKey("empty-role"));
    Assert.assertTrue(roleMap.containsKey("role1"));
    Assert.assertTrue(roleMap.containsKey("role2"));
    Assert.assertEquals(3, roleMap.size());
  }

  public void testSearchUser()
      throws Exception
  {
    UserManager userLocator = this.getUserManager();

    Set<User> users = userLocator.searchUsers(new UserSearchCriteria("test"));
    Map<String, User> userMap = this.toUserMap(users);

    Assert.assertTrue(userMap.containsKey("test-user"));
    Assert.assertTrue(userMap.containsKey("test-user-with-empty-role"));

    Assert.assertEquals(2, users.size());
  }

  private Map<String, RoleIdentifier> toRoleMap(Set<RoleIdentifier> roles) {
    Map<String, RoleIdentifier> results = new HashMap<String, RoleIdentifier>();

    for (RoleIdentifier plexusRole : roles) {
      results.put(plexusRole.getRoleId(), plexusRole);
    }
    return results;
  }

  private Map<String, User> toUserMap(Set<User> users) {
    Map<String, User> results = new HashMap<String, User>();

    for (User plexusUser : users) {
      results.put(plexusUser.getUserId(), plexusUser);
    }
    return results;
  }

  @Override
  public void configure(Properties properties) {
    super.configure(properties);
    properties.put("security-xml-file", "target/test-classes/"
        + this.getClass().getPackage().getName().replaceAll("\\.", "\\/") + "/security.xml");
  }

}
