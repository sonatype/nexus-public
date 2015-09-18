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

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.role.RoleIdentifier;

import org.junit.Assert;
import org.junit.Test;

public class MissingRoleUserManagerTest
    extends AbstractSecurityTest
{
  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return MissingRoleUserManagerTestSecurity.securityModel();
  }

  @Test
  public void testInvalidRoleMapping() throws Exception {
    SecuritySystem userManager = getSecuritySystem();

    User user = userManager.getUser("jcoder");
    Assert.assertNotNull(user);

    Set<String> roleIds = new HashSet<String>();
    for (RoleIdentifier role : user.getRoles()) {
      Assert.assertNotNull("User has null role.", role);
      roleIds.add(role.getRoleId());
    }
    Assert.assertFalse(roleIds.contains("INVALID-ROLE-BLA-BLA"));
  }
}
