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
package org.sonatype.nexus.testsuite.p2.nxcm1916;

import java.io.IOException;

import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class NXCM1916CRUDP2RepositoryIT
    extends AbstractNexusProxyP2IT
{

  private final RepositoryMessageUtil messageUtil;

  public NXCM1916CRUDP2RepositoryIT()
      throws ComponentLookupException
  {
    super("nxcm1916");
    messageUtil = new RepositoryMessageUtil(this, getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void createRepositoryTest()
      throws IOException
  {

    final RepositoryResource resource = new RepositoryResource();

    resource.setId("createTestRepo");
    resource.setRepoType("hosted");
    resource.setName("Create Test Repo");
    resource.setProvider("p2");
    resource.setFormat("p2");
    resource.setRepoPolicy(RepositoryPolicy.MIXED.name());

    messageUtil.createRepository(resource);
  }

  @Test
  public void readTest()
      throws IOException
  {

    final RepositoryResource resource = new RepositoryResource();

    resource.setId("readTestRepo");
    resource.setRepoType("hosted");
    resource.setName("Read Test Repo");
    resource.setProvider("p2");
    resource.setFormat("p2");
    resource.setRepoPolicy(RepositoryPolicy.MIXED.name());

    messageUtil.createRepository(resource);

    final RepositoryResource responseRepo = (RepositoryResource) messageUtil.getRepository(resource.getId());

    messageUtil.validateResourceResponse(resource, responseRepo);

  }

  @Test
  public void updateTest()
      throws IOException
  {

    RepositoryResource resource = new RepositoryResource();

    resource.setId("updateTestRepo");
    resource.setRepoType("hosted");
    resource.setName("Update Test Repo");
    resource.setProvider("p2");
    resource.setFormat("p2");
    resource.setRepoPolicy(RepositoryPolicy.MIXED.name());

    resource = (RepositoryResource) messageUtil.createRepository(resource);

    resource.setName("updated repo");

    messageUtil.updateRepo(resource);

  }

  @Test
  public void deleteTest()
      throws IOException
  {
    RepositoryResource resource = new RepositoryResource();

    resource.setId("deleteTestRepo");
    resource.setRepoType("hosted");
    resource.setName("Delete Test Repo");
    resource.setProvider("p2");
    resource.setFormat("p2");
    resource.setRepoPolicy(RepositoryPolicy.MIXED.name());

    resource = (RepositoryResource) messageUtil.createRepository(resource);

    final Response response = messageUtil.sendMessage(Method.DELETE, resource);

    assertThat(response.getStatus().isSuccess(), is(true));
    assertThat(getNexusConfigUtil().getRepo(resource.getId()), is(nullValue()));
  }

}
