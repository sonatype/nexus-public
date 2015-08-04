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
package org.sonatype.nexus.testsuite.security.nexus999;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.ChangePasswordUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Status;

public class Nexus999SetUsersPasswordIT
    extends AbstractPrivilegeTest
{
  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void changePassword()
      throws Exception
  {

    Status status = ChangePasswordUtils.changePassword("test-user", "newPassword");
    Assert.assertEquals("Status", status.getCode(), 204);

    // we need to change the password around for this
    status = ChangePasswordUtils.changePassword("test-user", TEST_USER_PASSWORD);
    Assert.assertEquals("Status", status.getCode(), 204);
  }

  @Test
  public void withPermission()
      throws Exception
  {
    overwriteUserRole(
        TEST_USER_NAME,
        "anonymous-with-login-setpw",
        "1",
        "2" /* login */,
        "6",
        "14",
        "17",
        "19",
        "44",
        "54",
        "55",
        "57",
        "58",
        "59",
        "72"/* set pw */,
        "T1",
        "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to change my own password
    Status status = ChangePasswordUtils.changePassword("test-user", "newPassword");
    Assert.assertEquals("Status", status.getCode(), 204);

    // we need to change the password around for this
    TestContainer.getInstance().getTestContext().setPassword("newPassword");
    status = ChangePasswordUtils.changePassword("test-user", "newPassword");
    Assert.assertEquals("Status", status.getCode(), 204);

    status = ChangePasswordUtils.changePassword("test-user", TEST_USER_PASSWORD);
    Assert.assertEquals("Status", status.getCode(), 204);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
  }

  @Test
  public void withoutPermission()
      throws Exception
  {
    overwriteUserRole(
        TEST_USER_NAME,
        "anonymous-with-login-but-setpw",
        "1",
        "2" /* login */,
        "6",
        "14",
        "17",
        "19",
        "44",
        "54",
        "55",
        "57",
        "58",
        "59", /* "72" set pw, */
        "T1",
        "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // NOT Should be able to forgot my own username
    Status status = ChangePasswordUtils.changePassword("test-user", "123admin");
    Assert.assertEquals(403, status.getCode());

    // NOT Should be able to forgot anyone username
    status = ChangePasswordUtils.changePassword("admin", "123admin");
    Assert.assertEquals(403, status.getCode());
  }

}
