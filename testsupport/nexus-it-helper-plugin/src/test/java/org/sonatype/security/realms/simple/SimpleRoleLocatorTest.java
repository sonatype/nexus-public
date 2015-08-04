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

import java.util.HashSet;
import java.util.Set;

import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.realms.AbstractRealmTest;

import junit.framework.Assert;
import org.junit.Test;

public class SimpleRoleLocatorTest
    extends AbstractRealmTest
{

  @Test
  public void testListRoleIds()
      throws Exception
  {
    AuthorizationManager roleLocator = lookup(AuthorizationManager.class, "Simple");

    Set<String> roleIds = this.toIdSet(roleLocator.listRoles());
    Assert.assertTrue(roleIds.contains("role-xyz"));
    Assert.assertTrue(roleIds.contains("role-abc"));
    Assert.assertTrue(roleIds.contains("role-123"));
  }

  private Set<String> toIdSet(Set<Role> roles) {
    Set<String> ids = new HashSet<String>();

    for (Role role : roles) {
      ids.add(role.getRoleId());
    }

    return ids;
  }

}
