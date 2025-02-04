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
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.role.RoleIdentifier;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

// FIXME: resolve with other UserManagerTest

/**
 * Tests for {@link UserManager}.
 */
public class UserManager2Test
    extends AbstractSecurityTest
{
  private UserManager underTest;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    underTest = getUserManager();
  }

  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return UserManager2TestSecurity.securityModel();
  }

  @Test
  public void testListUserIds() throws Exception {
    Set<String> userIds = underTest.listUserIds();
    assertThat(userIds, hasItem("test-user"));
    assertThat(userIds, hasItem("anonymous"));
    assertThat(userIds, hasItem("admin"));

    assertThat(userIds, hasSize(4));
  }

  @Test
  public void testListUsers() throws Exception {
    Set<User> users = underTest.listUsers();
    Map<String, User> userMap = toUserMap(users);

    assertThat(userMap, hasKey("test-user"));
    assertThat(userMap, hasKey("anonymous"));
    assertThat(userMap, hasKey("admin"));

    Assert.assertEquals(4, users.size());
  }

  @Test
  public void testGetUser() throws Exception {
    User testUser = underTest.getUser("test-user");

    Assert.assertEquals("Test User", testUser.getName());
    Assert.assertEquals("test-user", testUser.getUserId());
    Assert.assertEquals("test-user@example.org", testUser.getEmailAddress());

    // test roles
    Map<String, RoleIdentifier> roleMap = this.toRoleMap(testUser.getRoles());

    assertThat(roleMap, hasKey("role1"));
    assertThat(roleMap, hasKey("role2"));
    Assert.assertEquals(2, roleMap.size());
  }

  @Test
  public void testGetUserWithEmptyRole() throws Exception {
    User testUser = underTest.getUser("test-user-with-empty-role");

    Assert.assertEquals("Test User With Empty Role", testUser.getName());
    Assert.assertEquals("test-user-with-empty-role", testUser.getUserId());
    Assert.assertEquals("test-user-with-empty-role@example.org", testUser.getEmailAddress());

    // test roles
    Map<String, RoleIdentifier> roleMap = this.toRoleMap(testUser.getRoles());

    assertThat(roleMap, hasKey("empty-role"));
    assertThat(roleMap, hasKey("role1"));
    assertThat(roleMap, hasKey("role2"));
    Assert.assertEquals(3, roleMap.size());
  }

  @Test
  public void testSearchUser() throws Exception {
    Set<User> users = underTest.searchUsers(new UserSearchCriteria("test"));
    Map<String, User> userMap = toUserMap(users);

    assertThat(userMap, hasKey("test-user"));
    assertThat(userMap, hasKey("test-user-with-empty-role"));

    Assert.assertEquals(2, users.size());
  }

  private Map<String, RoleIdentifier> toRoleMap(final Set<RoleIdentifier> roles) {
    Map<String, RoleIdentifier> results = new HashMap<String, RoleIdentifier>();

    for (RoleIdentifier plexusRole : roles) {
      results.put(plexusRole.getRoleId(), plexusRole);
    }
    return results;
  }

  private static Map<String, User> toUserMap(final Set<User> users) {
    Map<String, User> results = new HashMap<String, User>();

    for (User plexusUser : users) {
      results.put(plexusUser.getUserId(), plexusUser);
    }
    return results;
  }
}
