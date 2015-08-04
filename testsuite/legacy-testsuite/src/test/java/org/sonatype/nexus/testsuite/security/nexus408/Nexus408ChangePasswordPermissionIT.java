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
package org.sonatype.nexus.testsuite.security.nexus408;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.ITGroups.SECURITY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.ChangePasswordUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restlet.data.Status;

/**
 * Test the privilege for changing a users password..
 */
public class Nexus408ChangePasswordPermissionIT
    extends AbstractPrivilegeTest
{
  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(SECURITY.class)
  public void withPermission()
      throws Exception
  {
    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-changepw", "1", "2" /* login */, "6", "14", "17",
        "19", "44", "54", "55", "57", "58", "59", "64"/* change pw */, "T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to change my own password
    Status status = ChangePasswordUtils.changePassword("test-user", "admin123", "123admin");
    Assert.assertTrue(status.isSuccess());

    // password changed ! should fail
    status = ChangePasswordUtils.changePassword("test-user", "admin123", "123admin");
    Assert.assertEquals(401, status.getCode());
    status = ChangePasswordUtils.changePassword("test-user", "123admin", "admin123");
    Assert.assertEquals(401, status.getCode());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword("123admin");

    // should pass
    status = ChangePasswordUtils.changePassword("test-user", "123admin", "admin123");
    Assert.assertTrue(status.isSuccess());

    // should NOT be able to change another users password
  }

  @Test
  @Category(SECURITY.class)
  public void withoutPermission()
      throws Exception
  {
    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-but-changepw", "1", "2" /* login */, "6", "14",
        "17", "19", "44", "54", "55", "57", "58", "59", /* "64" change pw, */"T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // NOT Should be able to forgot my own username
    Status status = ChangePasswordUtils.changePassword("test-user", "admin123", "123admin");
    Assert.assertEquals(403, status.getCode());
  }

}
