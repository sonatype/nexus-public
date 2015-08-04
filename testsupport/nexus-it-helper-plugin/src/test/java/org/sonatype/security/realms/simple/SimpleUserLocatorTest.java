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
package org.sonatype.security.realms.simple;

import java.util.Set;

import org.sonatype.security.realms.AbstractRealmTest;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserManager;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import junit.framework.Assert;
import org.junit.Test;

public class SimpleUserLocatorTest
    extends AbstractRealmTest
{

  @Test
  public void testLocatorLookup()
      throws Exception
  {
    // a bit of Plexus background, this is how you can look up a component from a test class
    lookup(UserManager.class, "Simple");
  }

  @Test
  public void testSearch()
      throws Exception
  {
    UserManager userLocator = lookup(UserManager.class, "Simple");

    Set<User> result = userLocator.searchUsers(new UserSearchCriteria("adm"));
    Assert.assertEquals(1, result.size());
    // your test could be a bit more robust
    Assert.assertEquals(result.iterator().next().getUserId(), "admin-simple");
  }

  @Test
  public void testIdList()
      throws Exception
  {
    UserManager userLocator = lookup(UserManager.class, "Simple");

    Set<String> ids = userLocator.listUserIds();

    Assert.assertTrue(ids.contains("admin-simple"));
    Assert.assertTrue(ids.contains("deployment-simple"));
    Assert.assertTrue(ids.contains("anonymous-simple"));

    Assert.assertEquals(3, ids.size());
  }

  @Test
  public void testUserList()
      throws Exception
  {
    UserManager userLocator = this.lookup(UserManager.class, "Simple");

    Set<User> users = userLocator.listUsers();
    // your test could be a bit more robust
    Assert.assertEquals(3, users.size());
  }

}
