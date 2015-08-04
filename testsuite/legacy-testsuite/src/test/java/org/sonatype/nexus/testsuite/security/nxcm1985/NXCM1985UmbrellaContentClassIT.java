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
package org.sonatype.nexus.testsuite.security.nxcm1985;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.proxy.registry.RootContentClass;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.TargetMessageUtil;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the privilege for CRUD operations.
 */
public class NXCM1985UmbrellaContentClassIT
    extends AbstractPrivilegeTest
{
  private Set<String> rootPrivIds = new HashSet<String>();

  @Test
  public void validatePrivs()
      throws Exception
  {
    createPrivs();

    resetTestUserPrivs(false);

    // create failure
    Gav gav = GavUtil.newGav("nxcm1985", "artifact", "1.0");
    int status =
        getDeployUtils().deployUsingGavWithRest(getTestRepositoryId(), gav, getTestFile("artifact.jar"));
    Assert.assertEquals("Status", status, 403);

    resetTestUserPrivs(true);

    // create success
    status = getDeployUtils().deployUsingGavWithRest(getTestRepositoryId(), gav, getTestFile("artifact.jar"));
    Assert.assertEquals("Status", status, 201);

    resetTestUserPrivs(false);

    // read failure
    String serviceURI =
        "content/repositories/" + this.getTestRepositoryId() + "/" + this.getRelitiveArtifactPath(gav);
    Response response = RequestFacade.sendMessage(serviceURI, Method.GET);
    Assert.assertEquals("Status", response.getStatus().getCode(), 403);

    resetTestUserPrivs(true);

    // read success
    response = RequestFacade.sendMessage(serviceURI, Method.GET);
    Assert.assertEquals("Status", response.getStatus().getCode(), 200);

    resetTestUserPrivs(false);

    // delete failure
    serviceURI = "content/repositories/" + this.getTestRepositoryId() + "/nxcm1985";
    response = RequestFacade.sendMessage(serviceURI, Method.DELETE);
    Assert.assertEquals("Status", response.getStatus().getCode(), 403);

    resetTestUserPrivs(true);

    // delete success
    response = RequestFacade.sendMessage(serviceURI, Method.DELETE);
    Assert.assertEquals("Status", response.getStatus().getCode(), 204);
  }

  private void resetTestUserPrivs(boolean addPrivs)
      throws Exception
  {
    super.resetTestUserPrivs();

    if (addPrivs) {
      addPrivilege(TEST_USER_NAME, "65", rootPrivIds.toArray(new String[0]));
    }

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
  }

  private void createPrivs()
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    List<RepositoryTargetListResource> targets = TargetMessageUtil.getList();

    String targetId = null;

    for (RepositoryTargetListResource target : targets) {
      if (target.getContentClass().equals(RootContentClass.ID)) {
        targetId = target.getId();
        break;
      }
    }

    if (targetId == null) {
      Assert.fail("Target not found!");
    }

    PrivilegesMessageUtil util = new PrivilegesMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);

    PrivilegeResource resource = new PrivilegeResource();

    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId(targetId);
    resource.setName("nxcm1985root");
    resource.setDescription("nxcm1985root");
    resource.setMethod(Arrays.asList("create", "read", "update", "delete"));

    List<PrivilegeStatusResource> privs = util.createPrivileges(resource);

    for (PrivilegeStatusResource priv : privs) {
      rootPrivIds.add(priv.getId());
    }
  }
}
