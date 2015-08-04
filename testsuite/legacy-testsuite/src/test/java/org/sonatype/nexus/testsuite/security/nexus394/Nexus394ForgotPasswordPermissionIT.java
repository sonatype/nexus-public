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
package org.sonatype.nexus.testsuite.security.nexus394;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.ForgotPasswordUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Response;

/**
 * Test the privilege for forgot password.
 */
public class Nexus394ForgotPasswordPermissionIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void withPermission()
      throws Exception
  {
    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-forgot", "1", "2" /* login */, "6", "14", "17", "19",
        "44", "54", "55", "57"/* forgot */, "58", "59", "T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // NOT Should be able to forgot anyone password
    Response response =
        ForgotPasswordUtils.get(this).recoverUserPassword("anonymous", "changeme2@yourcompany.com");
    Assert.assertFalse("Status", response.getStatus().isSuccess());

    // Should be able to forgot my own password
    response = ForgotPasswordUtils.get(this).recoverUserPassword(TEST_USER_NAME, "nexus-dev2@sonatype.org");
    Assert.assertTrue("Status", response.getStatus().isSuccess());

  }

}
