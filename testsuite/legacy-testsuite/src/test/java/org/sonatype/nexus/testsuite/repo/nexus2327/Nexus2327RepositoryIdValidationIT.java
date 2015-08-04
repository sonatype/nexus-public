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
package org.sonatype.nexus.testsuite.repo.nexus2327;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus2327RepositoryIdValidationIT
    extends AbstractNexusIntegrationTest
{
  private RepositoryMessageUtil repositoryMsgUtil;

  private GroupMessageUtil groupMsgUtil;

  public Nexus2327RepositoryIdValidationIT()
      throws Exception
  {
    repositoryMsgUtil = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);

    groupMsgUtil = new GroupMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void repositoryIdLegal()
      throws Exception
  {
    RepositoryResource resource = new RepositoryResource();
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());

    resource.setId("repoaA1-_.");
    Response resp = repositoryMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertTrue(resp.getStatus().isSuccess());
  }

  @Test
  public void repositoryIdIllegal()
      throws Exception
  {
    RepositoryResource resource = new RepositoryResource();
    resource.setRepoType("hosted"); // [hosted, proxy, virtual]
    resource.setName("Create Test Repo");
    resource.setProvider("maven2");
    resource.setFormat("maven2");
    resource.setRepoPolicy(RepositoryPolicy.RELEASE.name());

    resource.setId("repo/");
    Response resp = repositoryMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("repo,");
    resp = repositoryMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("repo*");
    resp = repositoryMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("repo>");
    resp = repositoryMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());
  }

  @Test
  public void groupIdLegal()
      throws Exception
  {
    RepositoryGroupResource resource = new RepositoryGroupResource();
    resource.setName("createTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");
    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId("nexus-test-harness-repo");
    resource.addRepository(member);

    resource.setId("groupaA0-_.");
    Response resp = groupMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertTrue(resp.getStatus().isSuccess());
  }

  @Test
  public void groupIdIllegal()
      throws Exception
  {
    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setName("createTestGroup");
    resource.setFormat("maven2");
    resource.setProvider("maven2");
    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId("nexus-test-harness-repo");
    resource.addRepository(member);

    resource.setId("group/");
    Response resp = groupMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("group,");
    resp = groupMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("group*");
    resp = groupMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());

    resource.setId("group>");
    resp = groupMsgUtil.sendMessage(Method.POST, resource);
    Assert.assertFalse(resp.getStatus().isSuccess());
  }

}
