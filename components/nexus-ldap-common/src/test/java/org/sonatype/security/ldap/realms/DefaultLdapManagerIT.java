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
package org.sonatype.security.ldap.realms;

import java.util.Collection;
import java.util.Set;

import org.sonatype.security.ldap.LdapTestSupport;
import org.sonatype.security.ldap.dao.LdapUser;

import junit.framework.Assert;
import org.junit.Test;

public class DefaultLdapManagerIT
    extends LdapTestSupport
{

  private LdapManager getLdapManager()
      throws Exception
  {
    return (LdapManager) this.lookup(LdapManager.class);
  }

  @Test
  public void testGetAll()
      throws Exception
  {
    LdapManager ldapManager = this.getLdapManager();

    Collection<LdapUser> users = ldapManager.getAllUsers();
    Assert.assertEquals(3, users.size());

    // NOTE: implementation detail, -1 == all
    Assert.assertEquals(3, ldapManager.getUsers(-1).size());
  }

  @Test
  public void testGetLimit()
      throws Exception
  {
    LdapManager ldapManager = this.getLdapManager();

    Assert.assertEquals(2, ldapManager.getUsers(2).size());
  }

  @Test
  public void testSort()
      throws Exception
  {
    LdapManager ldapManager = this.getLdapManager();

    Set<LdapUser> users = ldapManager.getAllUsers();
    Assert.assertEquals(3, users.size());

    String[] orderedUsers = {"Fox, Brian", "cstamas", "jvanzyl"};

    int index = 0;
    for (LdapUser user : users) {
      Assert.assertEquals(orderedUsers[index++], user.getUsername());
    }

  }
}
