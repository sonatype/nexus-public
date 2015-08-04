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
package org.sonatype.nexus.testsuite.security.nexus504;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.integrationtests.TestContext;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.UserCreationUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.RoleResource;

import com.thoughtworks.xstream.XStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.StatusMatchers.hasStatusCode;
import static org.sonatype.nexus.test.utils.StatusMatchers.isSuccess;

/**
 * Created a role without the Login to UI privilege => Created a user and associated the role to that user => After the
 * user was created, I edited the role associated to that user and added the Login to UI privilege => Note that the
 * user
 * was still not able to log in. However, all new users I created associated to that role had the ability to log in.
 */
public class Nexus504ChangeRoleIT
    extends AbstractPrivilegeTest
{

  private static final String NEXUS504_USER = "nexus504-user";

  private static final String NEXUS504_ROLE = "nexus504-role";

  private RoleMessageUtil roleUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void init() {
    XStream xstream = this.getXMLXStream();

    this.userUtil = new UserMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.roleUtil = new RoleMessageUtil(this, xstream, MediaType.APPLICATION_XML);
  }

  @Test
  public void test()
      throws Exception
  {
    // use admin
    TestContext testContext = TestContainer.getInstance().getTestContext();

    // user is created at security.xml

    testContext.setUsername(NEXUS504_USER);
    testContext.setPassword(TEST_USER_PASSWORD);

    assertThat(UserCreationUtil.login(), hasStatusCode(403));

    // add login privilege to role
    testContext.useAdminForRequests();

    RoleResource role = roleUtil.getRole(NEXUS504_ROLE);
    role.addPrivilege("2"/* login */);
    assertThat("Unable to add login privilege to role " + NEXUS504_ROLE + "\n"
        + RoleMessageUtil.update(role).getDescription(), RoleMessageUtil.update(role), isSuccess());

    // try to login again
    testContext.setUsername(NEXUS504_USER);
    testContext.setPassword(TEST_USER_PASSWORD);
    Status status2 = UserCreationUtil.login();
    assertThat(status2, hasStatusCode(200));
  }
}
