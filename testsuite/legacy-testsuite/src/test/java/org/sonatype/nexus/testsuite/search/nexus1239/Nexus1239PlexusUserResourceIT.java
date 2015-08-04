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

public class Nexus1239PlexusUserResourceIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getUserTestWithSource()
      throws IOException
  {

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    PlexusUserResource user = userUtil.getPlexusUser("default", "admin");
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

  @SuppressWarnings("unchecked")
  @Test
  public void getUserTestWithOutSource()
      throws IOException
  {

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    PlexusUserResource user = userUtil.getPlexusUser(null, "admin");
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

  @SuppressWarnings("unchecked")
  @Test
  public void getUserTestWithAllSource()
      throws IOException
  {

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    PlexusUserResource user = userUtil.getPlexusUser("all", "admin");
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

  @SuppressWarnings("unchecked")
  @Test
  public void getUsersTest()
      throws IOException
  {
    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.getPlexusUsers("default");

    List<String> userIds = new ArrayList<String>();

    for (PlexusUserResource plexusUserResource : users) {
      userIds.add(plexusUserResource.getUserId());
    }

    Assert.assertTrue(userIds.contains("admin"));
    Assert.assertTrue(userIds.contains("anonymous"));
    Assert.assertTrue(userIds.contains("deployment"));
    Assert.assertTrue(userIds.contains("test-user"));
    Assert.assertEquals("Users: " + userIds, users.size(), 4);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getUsersTestAllSource()
      throws IOException
  {
    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.getPlexusUsers("all");

    List<String> userIds = new ArrayList<String>();

    for (PlexusUserResource plexusUserResource : users) {
      userIds.add(plexusUserResource.getUserId());
    }

    Assert.assertTrue(userIds.contains("admin"));
    Assert.assertTrue(userIds.contains("anonymous"));
    Assert.assertTrue(userIds.contains("deployment"));
    Assert.assertTrue(userIds.contains("test-user"));
    // Assert.assertEquals( "Users: "+ userIds, 4, users.size() );

    // NOTE: this needs to be at least the number of users expected in the default realm, the In-Memory realm add
    // another user locator, and there is no way to disable it.
    Assert.assertTrue("Users: " + userIds, users.size() >= 4);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void junkSourceTest()
      throws IOException
  {

    UserMessageUtil userUtil = new UserMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusUserResource> users = userUtil.getPlexusUsers("VOID");
    Assert.assertEquals(0, users.size());
  }

}
