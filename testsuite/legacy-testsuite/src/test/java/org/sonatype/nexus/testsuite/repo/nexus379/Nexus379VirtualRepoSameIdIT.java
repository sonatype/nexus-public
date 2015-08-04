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
package org.sonatype.nexus.testsuite.repo.nexus379;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test to make sure a Virtual repo cannot have the same Id as an real repository.
 */
public class Nexus379VirtualRepoSameIdIT
    extends AbstractNexusIntegrationTest
{

  protected RepositoryMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest()
      throws ComponentLookupException
  {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void testVirtualRepoWithSameId()
      throws IOException
  {

    // create a repository
    RepositoryResource repo = new RepositoryResource();

    repo.setId("testVirtualRepoWithSameId");
    repo.setRepoType("hosted"); // [hosted, proxy, virtual]
    repo.setName("testVirtualRepoWithSameId");
    repo.setProvider("maven2");
    // format is neglected by server from now on, provider is the new guy in the town
    repo.setFormat("maven2");
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repo = (RepositoryResource) this.messageUtil.createRepository(repo);

    // now create a virtual one, this should fail

    // create a repository
    RepositoryShadowResource virtualRepo = new RepositoryShadowResource();

    virtualRepo.setId("testVirtualRepoWithSameId");
    virtualRepo.setRepoType("virtual"); // [hosted, proxy, virtual]
    virtualRepo.setName("testVirtualRepoWithSameId");
    virtualRepo.setProvider("m2-m1-shadow");
    // format is neglected by server from now on, provider is the new guy in the town
    virtualRepo.setFormat("maven1");
    virtualRepo.setShadowOf("testVirtualRepoWithSameId");
    Response response = this.messageUtil.sendMessage(Method.POST, virtualRepo);

    Assert.assertEquals("Status:" + "\n" + response.getEntity().getText(), response.getStatus().getCode(), 400);

  }

}
