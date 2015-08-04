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
package org.sonatype.nexus.testsuite.security.nexus650;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.nexus.test.utils.ChangePasswordUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.Status;

/**
 * Changes users password, restarts nexus, and verify password is correct.
 */
public class Nexus650ChangePasswordAndRebootIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void changePasswordAndReboot() throws Exception {
    this.giveUserRole(TEST_USER_NAME, "nx-admin");

    TestContext context = TestContainer.getInstance().getTestContext();

    context.setUsername(TEST_USER_NAME);
    context.setPassword(TEST_USER_PASSWORD);


    String newPassword = "123password";
    Status status = ChangePasswordUtils.changePassword(TEST_USER_NAME, TEST_USER_PASSWORD, newPassword);
    Assert.assertTrue("Status: ", status.isSuccess());

    // now change the password
    context.setPassword(newPassword);

    // reboot
    restartNexus();

    // now we can verify everything worked out
    Assert.assertTrue("Nexus is not running", getNexusStatusUtil().isNexusRunning());

  }

}
