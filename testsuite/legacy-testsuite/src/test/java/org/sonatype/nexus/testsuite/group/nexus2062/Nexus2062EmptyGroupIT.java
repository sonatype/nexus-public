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
package org.sonatype.nexus.testsuite.group.nexus2062;


import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus2062EmptyGroupIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void createEmptyGroup()
      throws Exception
  {
    GroupMessageUtil groupUtil = new GroupMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);

    RepositoryGroupResource resource = new RepositoryGroupResource();
    resource.setExposed(true);
    resource.setFormat("maven2");
    resource.setId("emptygroup");
    resource.setName("emptygroup");
    resource.setProvider("maven2");

    resource = groupUtil.createGroup(resource);

    Assert.assertEquals(0, resource.getRepositories().size());
  }

  @Test
  public void createGroupWithRepoAndDelete()
      throws Exception
  {
    GroupMessageUtil groupUtil = new GroupMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);

    RepositoryGroupResource resource = new RepositoryGroupResource();
    resource.setExposed(true);
    resource.setFormat("maven2");
    resource.setId("nonemptygroup");
    resource.setName("nonemptygroup");
    resource.setProvider("maven2");

    RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();
    member.setId(REPO_TEST_HARNESS_REPO);
    resource.addRepository(member);

    resource = groupUtil.createGroup(resource);

    resource.getRepositories().clear();

    resource = groupUtil.updateGroup(resource);

    Assert.assertEquals(0, resource.getRepositories().size());
  }
}
