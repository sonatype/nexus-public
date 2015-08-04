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
package org.sonatype.nexus.testsuite.security.nexus393;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.ResetPasswordUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Response;

/**
 * Test the privilege for password reset.
 *
 * TODO Move this tests to Sonatype Security
 */
public class Nexus393ResetPasswordWithoutPermissionIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void resetWithoutPermission()
      throws Exception
  {
    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-but-reset", "1", "2" /* login */, "6", "14", "17",
        "19", "44", "54", "55", "57", "58", /* "59" reset , */"T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // NOT Shouldn't be able to reset anyone password
    String username = "another-user";
    Response response = ResetPasswordUtils.resetPassword(username);
    Assert.assertEquals("Status: " + response.getStatus() + "\n" + response.getEntity(), response.getStatus().getCode(),
        403);

    // NOT Should be able to reset my own password
    username = TEST_USER_NAME;
    response = ResetPasswordUtils.resetPassword(username);
    Assert.assertEquals("Status: " + response.getStatus() + "\n" + response.getEntity(), response.getStatus().getCode(),
        403);

  }

}
