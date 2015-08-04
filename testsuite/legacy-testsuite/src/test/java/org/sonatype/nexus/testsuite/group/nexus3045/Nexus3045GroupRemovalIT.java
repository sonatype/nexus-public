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
package org.sonatype.nexus.testsuite.group.nexus3045;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.RoutesMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.Matchers.hasItem;

public class Nexus3045GroupRemovalIT
    extends AbstractNexusIntegrationTest
{

  private RoutesMessageUtil routesUtil;

  private RepositoryMessageUtil repoUtil;

  private GroupMessageUtil groupUtil;

  public Nexus3045GroupRemovalIT()
      throws ComponentLookupException
  {
    super();
    routesUtil = new RoutesMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
    repoUtil = new RepositoryMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
    groupUtil = new GroupMessageUtil(this, XStreamFactory.getXmlXStream(), MediaType.APPLICATION_XML);
  }

  private static final String GROUP_ROUTE_ID = "297bf5de34f";

  private static final String REPO_ROUTE_ID = "29574c59c49";

  @Test
  public void removeGroup()
      throws IOException
  {
    Assert.assertNotNull(routesUtil.getRoute(GROUP_ROUTE_ID));

    RepositoryGroupResource resource = this.groupUtil.getGroup("public");
    Response response = null;
    try {
      response = this.groupUtil.sendMessage(Method.DELETE, resource);
      Assert.assertTrue(response.getStatus().isSuccess());
      RequestFacade.releaseResponse(response);

      response = routesUtil.getRouteResponse(GROUP_ROUTE_ID);
      Assert.assertEquals(404, response.getStatus().getCode());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

  @Test
  public void removeRepository()
      throws IOException
  {
    Assert.assertNotNull(routesUtil.getRoute(REPO_ROUTE_ID));

    RepositoryBaseResource resource = this.repoUtil.getRepository("releases");
    Response response = this.repoUtil.sendMessage(Method.DELETE, resource);
    Assert.assertTrue(response.getStatus().isSuccess());

    RepositoryRouteResource route = routesUtil.getRoute(REPO_ROUTE_ID);
    Assert.assertNotNull(route);
    MatcherAssert.assertThat(getRepoIds(route.getRepositories()), hasItem("thirdparty"));
    MatcherAssert.assertThat(getRepoIds(route.getRepositories()),
        CoreMatchers.not(hasItem("releases")));
  }

  private List<String> getRepoIds(List<RepositoryRouteMemberRepository> repositories) {
    List<String> repoIds = new ArrayList<String>();
    for (RepositoryRouteMemberRepository repo : repositories) {
      repoIds.add(repo.getId());
    }
    return repoIds;
  }
}
