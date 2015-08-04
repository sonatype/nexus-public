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
package org.sonatype.nexus.testsuite.group.nexus532;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.sonatype.nexus.test.utils.StatusMatchers.isNotFound;

/**
 * CRUD tests for XML request/response.
 */
public class Nexus532GroupsCrudXmlIT
    extends AbstractNexusIntegrationTest
{

  protected GroupMessageUtil messageUtil;

  public Nexus532GroupsCrudXmlIT() {
    this.messageUtil = new GroupMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void createGroupTest()
      throws IOException
  {

    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setId("createTestGroup");
    resource.setName("createTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");

    createMembers(resource);

    // this also validates
    this.messageUtil.createGroup(resource);
  }

  @Test
  public void notFoundTest()
      throws Exception
  {
    String groupId = "nonexisted-group-from-mars";

    RequestFacade.doGetForStatus(GroupMessageUtil.SERVICE_PART + "/" + groupId, isNotFound());
  }

  @Test
  public void readTest()
      throws IOException
  {

    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setId("readTestGroup");
    resource.setName("readTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");

    createMembers(resource);

    // this also validates
    this.messageUtil.createGroup(resource);

    RepositoryGroupResource responseRepo = this.messageUtil.getGroup(resource.getId());

    // validate they are the same
    this.messageUtil.validateResourceResponse(resource, responseRepo);

  }

  protected void createMembers(RepositoryGroupResource resource) {
    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId("nexus-test-harness-repo");
    resource.addRepository(member);
  }

  @Test
  public void updateTest()
      throws IOException
  {

    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setId("updateTestGroup");
    resource.setName("updateTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");

    createMembers(resource);

    // this also validates
    resource = this.messageUtil.createGroup(resource);

    // udpdate the group
    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId("nexus-test-harness-repo2");
    resource.addRepository(member);

    this.messageUtil.updateGroup(resource);

  }

  @Test
  public void deleteTest()
      throws IOException
  {
    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setId("deleteTestGroup");
    resource.setName("deleteTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");

    createMembers(resource);

    // this also validates
    resource = this.messageUtil.createGroup(resource);

    // now delete it...
    // use the new ID
    Response response = this.messageUtil.sendMessage(Method.DELETE, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not delete Repository: " + response.getStatus());
    }
    Assert.assertNull(getNexusConfigUtil().getRepo(resource.getId()));
  }

  @Test
  public void listTest()
      throws IOException
  {

    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setId("listTestGroup");
    resource.setName("listTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");

    createMembers(resource);

    // this also validates
    resource = this.messageUtil.createGroup(resource);

    // now get the lists
    List<RepositoryGroupListResource> groups = this.messageUtil.getList();

    for (Iterator<RepositoryGroupListResource> iter = groups.iterator(); iter.hasNext(); ) {
      RepositoryGroupListResource group = iter.next();
      M2GroupRepositoryConfiguration cGroup = getNexusConfigUtil().getGroup(group.getId());

      Assert.assertNotNull("CRepositoryGroup", cGroup);
    }
  }
}
