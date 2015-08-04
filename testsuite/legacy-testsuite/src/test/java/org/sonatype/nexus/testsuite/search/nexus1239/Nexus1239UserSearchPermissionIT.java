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
package org.sonatype.nexus.testsuite.search.nexus1239;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.ITGroups.SECURITY;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.PlexusUserResource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.restlet.data.MediaType;

import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus1239UserSearchPermissionIT
    extends AbstractPrivilegeTest
{

  @Test
  @Category(SECURITY.class)
  public void userExactSearchTest()
      throws IOException
  {
    this.giveUserPrivilege(TEST_USER_NAME, "39");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.searchPlexusUsers("default", "admin");

    Assert.assertEquals(1, users.size());
    PlexusUserResource user = users.get(0);
    Assert.assertEquals(user.getUserId(), "admin");
    Assert.assertEquals(user.getEmail(), "changeme@yourcompany.com");
    Assert.assertEquals(user.getFirstName(), "Administrator");
    Assert.assertEquals(user.getSource(), "default");

    List<PlexusRoleResource> roles = user.getRoles();
    Assert.assertEquals(1, roles.size());

    PlexusRoleResource role = roles.get(0);
    Assert.assertEquals(role.getName(), "Nexus Administrator Role");
    Assert.assertEquals(role.getRoleId(), "nx-admin");
    Assert.assertEquals(role.getSource(), "default");
  }

  @Test
  @Category(SECURITY.class)
  public void userSearchTest()
      throws IOException
  {

    this.giveUserPrivilege(TEST_USER_NAME, "39");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.searchPlexusUsers("default", "a");

    List<String> userIds = new ArrayList<String>();

    for (PlexusUserResource plexusUserResource : users) {
      userIds.add(plexusUserResource.getUserId());
    }

    Assert.assertEquals(2, users.size());
    Assert.assertTrue(userIds.contains("admin"));
    Assert.assertTrue(userIds.contains("anonymous"));
  }

  @Test
  @Category(SECURITY.class)
  public void emptySearchTest()
      throws IOException
  {
    this.giveUserPrivilege(TEST_USER_NAME, "39");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.searchPlexusUsers("default", "VOID");
    Assert.assertEquals(0, users.size());
  }

  public void noAccessTest()
      throws IOException
  {

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String uriPart = RequestFacade.SERVICE_LOCAL + "user_search/default/a";

    RequestFacade.doGet(uriPart, respondsWithStatusCode(403));
  }

}
