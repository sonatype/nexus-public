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
package org.sonatype.nexus.security.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class AdditionalRoleSecuritySystemTest
    extends AbstractSecurityTest
{
  private SecuritySystem securitySystem;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    securitySystem = getSecuritySystem();
  }

  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return AdditionalRoleSecuritySystemTestSecurity.securityModel();
  }

  private Set<String> getRoles() throws Exception {
    AuthorizationManager authzManager = lookup(AuthorizationManager.class);

    Set<String> roles = new HashSet<String>();
    for (Role role : authzManager.listRoles()) {
      roles.add(role.getRoleId());
    }

    return roles;
  }

  @Test
  public void testListUsers() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria(null, null, "MockUserManagerA");
    Set<User> users = securitySystem.searchUsers(criteria);

    Map<String, User> userMap = toUserMap(users);

    User user = userMap.get("jcoder");
    Assert.assertNotNull(user);

    // A,B,C,1
    Set<String> roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue("roles: " + toRoleIdSet(user.getRoles()), roleIds.contains("Role1"));

    Assert.assertEquals("roles: " + toRoleIdSet(user.getRoles()), 4, user.getRoles().size());

    user = userMap.get("dknudsen");
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());

    // Role2
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    user = userMap.get("cdugas");
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));

    user = userMap.get("pperalez");
    Assert.assertNotNull(user);
    Assert.assertEquals(0, user.getRoles().size());
  }

  @Ignore("TESTING, issue here with more usermanager bound than test requires")
  public void testSearchEffectiveTrue() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.setOneOfRoleIds(getRoles());

    criteria.setUserId("pperalez");
    User user = searchForSingleUser(criteria, "pperalez", null);
    Assert.assertNull(user);

    criteria.setUserId("jcoder");
    user = searchForSingleUser(criteria, "jcoder", null);
    Assert.assertNotNull(user);
    Assert.assertEquals("Roles: " + toRoleIdSet(user.getRoles()), 4, user.getRoles().size());

    // A,B,C,1
    Set<String> roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue(roleIds.contains("Role1"));

    criteria.setUserId("dknudsen");
    user = searchForSingleUser(criteria, "dknudsen", null);
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());

    // Role2
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    criteria.setUserId("cdugas");
    user = searchForSingleUser(criteria, "cdugas", null);
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));
  }

  @Test
  public void testSearchEffectiveFalse() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria();

    criteria.setUserId("pperalez");
    User user = searchForSingleUser(criteria, "pperalez", "MockUserManagerA");
    Assert.assertNotNull(user);

    criteria.setUserId("jcoder");
    user = searchForSingleUser(criteria, "jcoder", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(4, user.getRoles().size());

    // A,B,C,1
    Set<String> roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("RoleC"));
    Assert.assertTrue(roleIds.contains("Role1"));

    criteria.setUserId("dknudsen");
    user = searchForSingleUser(criteria, "dknudsen", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(1, user.getRoles().size());

    // Role2
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("Role2"));

    criteria.setUserId("cdugas");
    user = searchForSingleUser(criteria, "cdugas", "MockUserManagerA");
    Assert.assertNotNull(user);
    Assert.assertEquals(3, user.getRoles().size());

    // A,B,1
    roleIds = toRoleIdSet(user.getRoles());
    Assert.assertTrue(roleIds.contains("RoleA"));
    Assert.assertTrue(roleIds.contains("RoleB"));
    Assert.assertTrue(roleIds.contains("Role1"));
  }

  @Ignore("TESTING, issue here with more usermanager bound than test requires")
  public void testNestedRoles() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria();
    criteria.getOneOfRoleIds().add("Role1");

    Set<User> result = securitySystem.searchUsers(criteria);

    Map<String, User> userMap = toUserMap(result);
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("admin"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("test-user"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("jcoder"));
    Assert.assertTrue("User not found in: " + userMap, userMap.containsKey("cdugas"));
    // Assert.assertTrue( "User not found in: " + userMap, userMap.containsKey( "other-user" ) );
    // other user is only defined in the mapping, simulates a user that was deleted

    Assert.assertEquals(4, result.size());
  }

  private User searchForSingleUser(UserSearchCriteria criteria, String userId, String source) throws Exception {
    criteria.setSource(source);
    Set<User> users = securitySystem.searchUsers(criteria);

    System.out.println("Found users:");
    for (User user : users) {
      System.out.format("%s, source=%s%n", user, user.getSource());
    }

    Map<String, User> userMap = toUserMap(users);
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
