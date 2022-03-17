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
package org.sonatype.nexus.testsuite.security.nexus1170;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.AuthenticationLoginResource;
import org.sonatype.security.rest.model.AuthenticationLoginResourceResponse;
import org.sonatype.security.rest.model.ClientPermission;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus1170ReducePermissionCheckingIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testAdminPrivileges()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    List<ClientPermission> permissions = this.getPermissions();

    Assert.assertEquals(this.getExpectedPrivilegeCount(), permissions.size());

    for (ClientPermission clientPermission : permissions) {
      Assert.assertEquals(15, clientPermission.getValue());
    }
  }

  @Test
  public void testDeploymentUserPrivileges()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername("test-user");
    TestContainer.getInstance().getTestContext().setPassword("admin123");

    List<ClientPermission> permissions = this.getPermissions();

    Assert.assertEquals(this.getExpectedPrivilegeCount(), permissions.size());
    this.checkPermission(permissions, "nexus:*", 0);
    this.checkPermission(permissions, "nexus:status", 1);
    this.checkPermission(permissions, "nexus:authentication", 1);
    this.checkPermission(permissions, "nexus:settings", 0);
    this.checkPermission(permissions, "nexus:repositories", 1);
    this.checkPermission(permissions, "nexus:repotemplates", 0);
    this.checkPermission(permissions, "nexus:repogroups", 1);
    this.checkPermission(permissions, "nexus:index", 1);
    this.checkPermission(permissions, "nexus:identify", 1);
    this.checkPermission(permissions, "nexus:attributes", 0);

    this.checkPermission(permissions, "nexus:cache", 0);
    this.checkPermission(permissions, "nexus:routes", 0);
    this.checkPermission(permissions, "nexus:tasks", 0);
    this.checkPermission(permissions, "security:privileges", 0);
    this.checkPermission(permissions, "security:roles", 0);
    this.checkPermission(permissions, "security:users", 0);
    this.checkPermission(permissions, "nexus:logs", 0);
    this.checkPermission(permissions, "nexus:configuration", 0);
    // no longer available by default
    // this.checkPermission( permissions, "nexus:feeds", 1 );
    this.checkPermission(permissions, "nexus:targets", 0);

    this.checkPermission(permissions, "nexus:wastebasket", 0);
    this.checkPermission(permissions, "nexus:artifact", 1);
    this.checkPermission(permissions, "nexus:repostatus", 1);
    this.checkPermission(permissions, "security:usersforgotpw", 9);
    this.checkPermission(permissions, "security:usersforgotid", 9);
    this.checkPermission(permissions, "security:userschangepw", 9);

    this.checkPermission(permissions, "nexus:command", 0);
    this.checkPermission(permissions, "nexus:repometa", 0);
    this.checkPermission(permissions, "nexus:tasksrun", 0);
    this.checkPermission(permissions, "nexus:tasktypes", 0);
    this.checkPermission(permissions, "nexus:componentscontentclasses", 1);
    this.checkPermission(permissions, "nexus:componentscheduletypes", 0);
    this.checkPermission(permissions, "security:userssetpw", 0);
    this.checkPermission(permissions, "nexus:componentrealmtypes", 0);
    this.checkPermission(permissions, "nexus:componentsrepotypes", 1);
    this.checkPermission(permissions, "security:componentsuserlocatortypes", 0);

    this.checkPermission(permissions, "apikey:access", 15);

    for (ClientPermission outPermission : permissions) {
      int count = 0;
      for (ClientPermission inPermission : permissions) {
        if (outPermission.getId().equals(inPermission.getId())) {
          count++;
        }
        if (count > 1) {
          Assert.fail("Duplicate privilege: " + outPermission.getId() + " found count: " + count);
        }
      }

    }

  }

  private void checkPermission(List<ClientPermission> permissions, String permission, int expectedValue) {
    for (ClientPermission clientPermission : permissions) {

      if (clientPermission.getId().equals(permission)) {
        Assert.assertEquals(expectedValue, clientPermission.getValue());
        return;
      }

    }
    Assert.fail("Did not find permission: " + permissions);
  }

  private int getExpectedPrivilegeCount()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    Set<String> privIds = new HashSet<String>();
    List<PrivilegeStatusResource> privs =
        new PrivilegesMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML).getList();
    for (PrivilegeStatusResource priv : privs) {
      if (priv.getType().equals("method")) {
        for (PrivilegeProperty prop : priv.getProperties()) {
          if (prop.getKey().equals("permission")) {
            privIds.add(prop.getValue());
          }
        }
      }
    }
    return privIds.size();
    // return getUserPrivs( TestContainer.getInstance().getTestContext().getUsername() ).size();
  }

  private List<ClientPermission> getPermissions()
      throws IOException
  {
    Response response =
        RequestFacade.sendMessage(RequestFacade.SERVICE_LOCAL + "authentication/login", Method.GET);

    String responseText = response.getEntity().getText();

    if (response.getStatus().isError()) {
      Assert.fail(response.getStatus() + "\n" + responseText);
    }

    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    AuthenticationLoginResourceResponse resourceResponse =
        (AuthenticationLoginResourceResponse) representation.getPayload(new AuthenticationLoginResourceResponse());

    AuthenticationLoginResource resource = resourceResponse.getData();

    return resource.getClientPermissions().getPermissions();
  }
}
