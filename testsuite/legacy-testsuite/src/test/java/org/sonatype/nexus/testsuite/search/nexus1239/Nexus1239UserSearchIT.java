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

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.PlexusUserResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus1239UserSearchIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void userExactSearchTest()
      throws IOException
  {

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
  public void userSearchTest()
      throws IOException
  {

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
  public void emptySearchTest()
      throws IOException
  {

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.searchPlexusUsers("default", "VOID");
    Assert.assertEquals(0, users.size());
  }
}
