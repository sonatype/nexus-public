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
package org.sonatype.nexus.testsuite.security.nexus1563;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.nexus.test.utils.UserCreationUtil;
import org.sonatype.security.rest.model.RoleResource;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.StatusMatchers.isSuccess;

public class Nexus1563ExternalRealmsLoginIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void security() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void loginExternalUser()
      throws Exception
  {
    TestContext testContext = TestContainer.getInstance().getTestContext();

    RoleResource role = new RoleResource();
    role.setId("role-123");
    role.setName("Role role-123");
    role.setDescription("Role role-123 external map");
    role.setSessionTimeout(60);
    role.addRole("nx-admin");
    testContext.useAdminForRequests();
    roleUtil.createRole(role);

    testContext.setUsername("admin-simple");
    testContext.setPassword("admin123");
    assertThat(UserCreationUtil.login(), isSuccess());
  }
}
