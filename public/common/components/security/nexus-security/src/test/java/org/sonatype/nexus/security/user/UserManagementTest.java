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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.AbstractSecurityTest.BaseSecurityConfiguration;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.user.UserManagementTest.UserManagementTestConfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({BaseSecurityConfiguration.class, UserManagementTestConfiguration.class})
class UserManagementTest
    extends AbstractSecurityTest
{
  static class UserManagementTestConfiguration
  {
    @Bean
    UserManager userManager() {
      return new MockUserManager(new NoOpEventManager());
    }
  }

  private SecuritySystem securitySystem;

  @BeforeEach
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    securitySystem = getSecuritySystem();

    RealmManager realmManager = lookup(RealmManager.class);
    realmManager.setConfiguredRealmIds(List.of("MockRealmA", "MockRealmB"));
  }

  @Test
  void testAllUsers() throws Exception {
    Set<User> users = securitySystem.listUsers();
    assertFalse(users.isEmpty());

    // put users in map for easy search
    Map<String, User> userMap = getMapFromSet(users);

    // now check all of the users
    assertTrue(userMap.containsKey("jcoder"));
    assertTrue(userMap.containsKey("cdugas"));
    assertTrue(userMap.containsKey("pperalez"));
    assertTrue(userMap.containsKey("dknudsen"));
    assertTrue(userMap.containsKey("anonymous-user"));

    assertTrue(userMap.containsKey("bburton"));
    assertTrue(userMap.containsKey("jblevins"));
    assertTrue(userMap.containsKey("ksimmons"));
    assertTrue(userMap.containsKey("fdahmen"));
    assertTrue(userMap.containsKey("jcodar"));

    // FIXME: This is a pretty fragile assertion
    assertEquals(15, users.size());

    // we just need to check to make sure there are 2 jcoders with the correct source
    verify2Jcoders(users);
  }

  @Test
  void testSearchWithCriteria() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria();

    criteria.setUserId("pperalez");
    Set<User> users = securitySystem.searchUsers(criteria);
    assertEquals(1, users.size());
    assertEquals("pperalez", users.iterator().next().getUserId());

    criteria.setUserId("ppera");
    users = securitySystem.searchUsers(criteria);
    assertEquals(1, users.size());
    assertEquals("pperalez", users.iterator().next().getUserId());

    criteria.setUserId("ppera");
    criteria.setSource("MockUserManagerB");
    users = securitySystem.searchUsers(criteria);
    assertEquals(0, users.size());

    criteria.setUserId("ksim");
    users = securitySystem.searchUsers(criteria);
    assertEquals(1, users.size());
    assertEquals("ksimmons", users.iterator().next().getUserId());

    criteria.setUserId("jcod");
    criteria.setSource(null);
    users = securitySystem.searchUsers(criteria);
    assertEquals(3, users.size());

    // put users in map for easy search
    Map<String, User> userMap = getMapFromSet(users);

    assertTrue(userMap.containsKey("jcodar"));

    // we just need to check to make sure there are 2 jcoders with the correct source (the counts are already
    // checked above)
    verify2Jcoders(users);
  }

  private static Map<String, User> getMapFromSet(final Set<User> users) {
    Map<String, User> userMap = new HashMap<String, User>();
    for (User user : users) {
      userMap.put(user.getUserId(), user);
    }
    return userMap;
  }

  private static void verify2Jcoders(final Set<User> users) {
    Map<String, User> jcoders = new HashMap<String, User>();
    for (User user : users) {
      if (user.getUserId().equals("jcoder")) {
        jcoders.put(user.getSource(), user);
      }
    }
    assertEquals(2, jcoders.size());
    assertTrue(jcoders.containsKey("MockUserManagerA"));
    assertTrue(jcoders.containsKey("MockUserManagerB"));
  }
}
