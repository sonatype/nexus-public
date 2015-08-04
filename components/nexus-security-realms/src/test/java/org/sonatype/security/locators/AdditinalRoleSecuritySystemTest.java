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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.sonatype.security.AbstractSecurityTestCase;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.realms.tools.StaticSecurityResource;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import junit.framework.Assert;

public class AdditinalRoleSecuritySystemTest
    extends AbstractSecurityTestCase
{
  public static final String PLEXUS_SECURITY_XML_FILE = "security-xml-file";

  private final String SECURITY_CONFIG_FILE_PATH = getBasedir() + "/target/test-classes/"
      + AdditinalRoleSecuritySystemTest.class.getPackage().getName().replaceAll("\\.", "\\/")
      + "/additinalRoleTest-security.xml";

  @Override
  public void configure(Properties properties) {
    super.configure(properties);
    properties.put(PLEXUS_SECURITY_XML_FILE, SECURITY_CONFIG_FILE_PATH);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(StaticSecurityResource.class).annotatedWith(Names.named("mock")).to(MockStaticSecurityResource.class);
  }

  private Set<String> getXMLRoles()
      throws Exception
  {
    AuthorizationManager authzManager = this.lookup(AuthorizationManager.class);

    Set<String> roles = new HashSet<String>();
    for (Role role : authzManager.listRoles()) {
      roles.add(role.getRoleId());
    }

    return roles;
  }

  private SecuritySystem getSecuritySystem()
      throws Exception
  {
    return this.lookup(SecuritySystem.class);
  }

  public void testListUsers()
      throws Exception
  {
    SecuritySystem userManager = this.getSecuritySystem();
    UserSearchCriteria criteria = new UserSearchCriteria(null, null, "MockUserManagerA");
    Set<User> users = userManager.searchUsers(criteria);

    Map<String, User> userMap = this.toUserMap(users);

    User user = userMap.get("jcoder");
    Assert.assertNotNull(user);

    // A,B,C,1
    Set<String> roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue("roles: " + this.toRoleIdSet(user.getRoles()), roleIds.contains("Role1"));

    Assert.assertEquals("roles: " + this.toRoleIdSet(user.getRoles()), 4, user.getRoles().size());

    user = userMap.get("dknudsen");
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());

    // Role2
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    user = userMap.get("cdugas");
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));

    user = userMap.get("pperalez");
    Assert.assertNotNull(user);
    Assert.assertEquals(0, user.getRoles().size());

  }

  public void testSearchEffectiveTrue()
      throws Exception
  {
    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.setOneOfRoleIds(this.getXMLRoles());

    criteria.setUserId("pperalez");
    User user = searchForSingleUser(criteria, "pperalez", null);
    Assert.assertNull(user);

    criteria.setUserId("jcoder");
    user = searchForSingleUser(criteria, "jcoder", null);
    Assert.assertNotNull(user);
    Assert.assertEquals("Roles: " + this.toRoleIdSet(user.getRoles()), 4, user.getRoles().size());
    // A,B,C,1
    Set<String> roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue(roleIds.contains("Role1"));

    criteria.setUserId("dknudsen");
    user = searchForSingleUser(criteria, "dknudsen", null);
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());
    // Role2
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    criteria.setUserId("cdugas");
    user = searchForSingleUser(criteria, "cdugas", null);
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));

  }

  public void testSearchEffectiveFalse()
      throws Exception
  {
    UserSearchCriteria criteria = new UserSearchCriteria();

    criteria.setUserId("pperalez");
    User user = searchForSingleUser(criteria, "pperalez", "MockUserManagerA");
    Assert.assertNotNull(user);

    criteria.setUserId("jcoder");
    user = searchForSingleUser(criteria, "jcoder", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(4, user.getRoles().size());
    // A,B,C,1
    Set<String> roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue(roleIds.contains("Role1"));

    criteria.setUserId("dknudsen");
    user = searchForSingleUser(criteria, "dknudsen", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());
    // Role2
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    criteria.setUserId("cdugas");
    user = searchForSingleUser(criteria, "cdugas", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = this.toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));

  }

  public void testNestedRoles()
      throws Exception
  {
    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.getOneOfRoleIds().add("Role1");

    Set<User> result = this.getSecuritySystem().searchUsers(criteria);

    Map<String, User> userMap = this.toUserMap(result);

    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("admin"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("test-user"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("jcoder"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("cdugas"));
    // Assert.assertTrue( "User not found in: " + userMap, userMap.containsKey( "other-user" ) );
    // other user is only defined in the mapping, simulates a user that was deleted

    Assert.assertEquals(4, result.size());

  }

  private User searchForSingleUser(UserSearchCriteria criteria, String userId, String source)
      throws Exception
  {
    SecuritySystem userManager = this.getSecuritySystem();

    criteria.setSource(source);
    Set<User> users = userManager.searchUsers(criteria);

    Map<String, User> userMap = this.toUserMap(users);

    Assert.assertTrue("More then 1 User was returned: " + userMap.keySet(), users.size() <= 1);

    return userMap.get(userId);
  }

  private Map<String, User> toUserMap(Set<User> users) {
    HashMap<String, User> map = new HashMap<String, User>();
    for (User plexusUser : users) {
      map.put(plexusUser.getUserId(), plexusUser);
    }
    return map;
  }

  private Set<String> toRoleIdSet(Set<RoleIdentifier> roles) {
    Set<String> roleIds = new HashSet<String>();
    for (RoleIdentifier role : roles) {
      roleIds.add(role.getRoleId());
    }
    return roleIds;
  }

}
