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
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmManager;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.junit.Assert;
import org.junit.Test;

public class UserManagementTest
    extends AbstractSecurityTest
{
  private SecuritySystem securitySystem;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    securitySystem = getSecuritySystem();

    RealmManager realmManager = lookup(RealmManager.class);
    RealmConfiguration model = new RealmConfiguration();
    model.setRealmNames(ImmutableList.of("MockRealmA", "MockRealmB"));
    realmManager.setConfiguration(model);
  }

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(UserManager.class)
            .annotatedWith(Names.named("Mock"))
            .to(MockUserManager.class)
            .in(Singleton.class);
      }
    });
  }

  @Test
  public void testAllUsers() throws Exception {
    Set<User> users = securitySystem.listUsers();
    Assert.assertFalse(users.isEmpty());

    // put users in map for easy search
    Map<String, User> userMap = this.getMapFromSet(users);

    // now check all of the users
    Assert.assertTrue(userMap.containsKey("jcoder"));
    Assert.assertTrue(userMap.containsKey("cdugas"));
    Assert.assertTrue(userMap.containsKey("pperalez"));
    Assert.assertTrue(userMap.containsKey("dknudsen"));
    Assert.assertTrue(userMap.containsKey("anonymous-user"));

    Assert.assertTrue(userMap.containsKey("bburton"));
    Assert.assertTrue(userMap.containsKey("jblevins"));
    Assert.assertTrue(userMap.containsKey("ksimmons"));
    Assert.assertTrue(userMap.containsKey("fdahmen"));
    Assert.assertTrue(userMap.containsKey("jcodar"));

    // FIXME: This is a pretty fragile assertion
    Assert.assertEquals(15, users.size());

    // we just need to check to make sure there are 2 jcoders with the correct source
    this.verify2Jcoders(users);
  }

  @Test
  public void testSearchWithCriteria() throws Exception {
    UserSearchCriteria criteria = new UserSearchCriteria();

    criteria.setUserId("pperalez");
    Set<User> users = securitySystem.searchUsers(criteria);
    Assert.assertEquals(1, users.size());
    Assert.assertEquals("pperalez", users.iterator().next().getUserId());

    criteria.setUserId("ppera");
    users = securitySystem.searchUsers(criteria);
    Assert.assertEquals(1, users.size());
    Assert.assertEquals("pperalez", users.iterator().next().getUserId());

    criteria.setUserId("ppera");
    criteria.setSource("MockUserManagerB");
    users = securitySystem.searchUsers(criteria);
    Assert.assertEquals(0, users.size());

    criteria.setUserId("ksim");
    users = securitySystem.searchUsers(criteria);
    Assert.assertEquals(1, users.size());
    Assert.assertEquals("ksimmons", users.iterator().next().getUserId());

    criteria.setUserId("jcod");
    criteria.setSource(null);
    users = securitySystem.searchUsers(criteria);
    Assert.assertEquals(3, users.size());

    // put users in map for easy search
    Map<String, User> userMap = this.getMapFromSet(users);

    Assert.assertTrue(userMap.containsKey("jcodar"));

    // we just need to check to make sure there are 2 jcoders with the correct source (the counts are already
    // checked above)
    this.verify2Jcoders(users);

  }

  private Map<String, User> getMapFromSet(Set<User> users) {
    Map<String, User> userMap = new HashMap<String, User>();
    for (User user : users) {
      userMap.put(user.getUserId(), user);
    }
    return userMap;
  }

  private void verify2Jcoders(Set<User> users) {
    Map<String, User> jcoders = new HashMap<String, User>();
    for (User user : users) {
      if (user.getUserId().equals("jcoder")) {
        jcoders.put(user.getSource(), user);
      }
    }
    Assert.assertEquals(2, jcoders.size());
    Assert.assertTrue(jcoders.containsKey("MockUserManagerA"));
    Assert.assertTrue(jcoders.containsKey("MockUserManagerB"));
  }
}
