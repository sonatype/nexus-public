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
package org.sonatype.nexus.testsuite.repo.nexus1765;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.rest.model.RepositoryStatusResource;
import org.sonatype.nexus.rest.model.RepositoryStatusResourceResponse;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccess;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.respondsWithStatusCode;

public class Nexus1765RepositoryServicesIT
    extends AbstractPrivilegeTest
{
  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testGetRepoStatus()
      throws Exception
  {
    this.giveUserPrivilege(TEST_USER_NAME, "55"); //nexus:repostatus:read
    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String repoId = this.getTestRepositoryId();
    String uriPart = RepositoryMessageUtil.SERVICE_PART + "/" + repoId + "/status";

    RequestFacade.doGet(uriPart, respondsWithStatusCode(403));
  }

  @Test
  public void testSetRepoStatus()
      throws Exception
  {

    this.giveUserPrivilege(TEST_USER_NAME, "55"); //nexus:repostatus:read
    this.giveUserPrivilege(TEST_USER_NAME, "56"); //nexus:repostatus:update

    String repoId = this.getTestRepositoryId();

    RepositoryStatusResource repoStatus = repoUtil.getStatus(repoId);
    repoStatus.setProxyMode(ProxyMode.BLOCKED_AUTO.name());

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    XStreamRepresentation representation = new XStreamRepresentation(
        this.getXMLXStream(),
        "",
        MediaType.APPLICATION_XML);
    RepositoryStatusResourceResponse resourceResponse = new RepositoryStatusResourceResponse();
    resourceResponse.setData(repoStatus);
    representation.setPayload(resourceResponse);

    final String uriPart = RepositoryMessageUtil.SERVICE_PART + "/" + repoId + "/status";
    RequestFacade.doPutForStatus(uriPart, representation, respondsWithStatusCode(403));
  }

  @Test
  public void testGetRepoMeta()
      throws Exception
  {
    this.giveUserPrivilege(TEST_USER_NAME, "67"); //nexus:repometa:read

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String repoId = this.getTestRepositoryId();
    String uriPart = RepositoryMessageUtil.SERVICE_PART + "/" + repoId + "/meta";

    RequestFacade.doGet(uriPart, respondsWithStatusCode(403));
  }

  @Test
  public void testGetRepoContent()
      throws Exception
  {
    this.giveUserPrivilege(TEST_USER_NAME, "T1"); //read all M2

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String repoId = this.getTestRepositoryId();
    String uriPart = RepositoryMessageUtil.SERVICE_PART + "/" + repoId + "/content/";
    final Status status = RequestFacade.doGetForStatus(uriPart);
    assertThat(status, isSuccess());
  }

  @Test
  public void testGetRepoIndexContent()
      throws Exception
  {
    this.giveUserPrivilege(TEST_USER_NAME, "T1"); //read all M2

    // use test user
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    String repoId = this.getTestRepositoryId();
    String uriPart = RepositoryMessageUtil.SERVICE_PART + "/" + repoId + "/index_content/";
    final Status status = RequestFacade.doGetForStatus(uriPart);
    assertThat(status, isSuccess());
  }

}
