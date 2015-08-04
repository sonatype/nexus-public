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
package org.sonatype.nexus.testsuite.repo.nexus2138;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.test.utils.RoutesMessageUtil;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.ResponseMatchers.isSuccessful;
import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus2138RouteFilteringIT
    extends AbstractPrivilegeTest
{

  @Test
  public void testRouteList()
      throws Exception
  {
    // create some test routes
    this.createRouteTest("public", this.getTestRepositoryId());

    this.createRouteTest("public", this.getTestRepositoryId(), "nexus-test-harness-release-repo");

    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertEquals(0, this.routeUtil.getList().size());
  }

  @Test
  public void testFilteredList()
      throws Exception
  {
    RoutesMessageUtil.removeAllRoutes();

    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");

    this.createRouteTest("public", this.getTestRepositoryId());

    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + "public");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + this.getTestRepositoryId());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    List<RepositoryRouteListResource> routes = RoutesMessageUtil.getList();
    Assert.assertEquals(1, routes.size());
    Assert.assertEquals(routes.get(0).getGroupId(), "public");
  }

  @Test
  public void testFilteredListWithOutAccessToGroup()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + this.getTestRepositoryId());

    this.createRouteTest("public", this.getTestRepositoryId());

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertEquals(0, RoutesMessageUtil.getList().size());
  }

  @Test
  public void testForbiddendGet()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");

    String response = this.createRouteTest("public", this.getTestRepositoryId());
    RepositoryRouteResource routeResource = this.routeUtil.getResourceFromText(response);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet(RoutesMessageUtil.SERVICE_PART + "/" + routeResource.getId(),
        respondsWithStatusCode(Status.CLIENT_ERROR_FORBIDDEN.getCode()));
  }

  @Test
  public void testForbiddendGetWithAccessToGroup()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");

    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + "public");

    String response = this.createRouteTest("public", this.getTestRepositoryId());
    RepositoryRouteResource routeResource = this.routeUtil.getResourceFromText(response);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet(RoutesMessageUtil.SERVICE_PART + "/" + routeResource.getId(),
        respondsWithStatusCode(Status.CLIENT_ERROR_FORBIDDEN.getCode()));
  }

  @Test
  public void testForbiddendGetWithOutAccessToGroup()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + this.getTestRepositoryId());

    String response = this.createRouteTest("public", this.getTestRepositoryId());
    RepositoryRouteResource routeResource = this.routeUtil.getResourceFromText(response);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet(RoutesMessageUtil.SERVICE_PART + "/" + routeResource.getId(),
        respondsWithStatusCode(Status.CLIENT_ERROR_FORBIDDEN.getCode()));
  }

  @Test
  public void testForbiddendGetWithAccessToGroupAndOneRepo()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-routing-admin");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + "public");
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + this.getTestRepositoryId());

    String response =
        this.createRouteTest("public", this.getTestRepositoryId(), "nexus-test-harness-release-repo");
    RepositoryRouteResource routeResource = this.routeUtil.getResourceFromText(response);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    RequestFacade.doGet(RoutesMessageUtil.SERVICE_PART + "/" + routeResource.getId(),
        respondsWithStatusCode(Status.CLIENT_ERROR_FORBIDDEN.getCode()));
  }

  private String createRouteTest(String groupId, String... repoIds)
      throws Exception
  {
    RepositoryRouteResource routeResource = new RepositoryRouteResource();
    routeResource.setGroupId(groupId);
    routeResource.setRuleType(RepositoryRouteResource.EXCLUSION_RULE_TYPE);
    routeResource.setPattern(".*/test-me/.*");

    for (String memberRepoId : repoIds) {
      RepositoryRouteMemberRepository memberRepoReleases = new RepositoryRouteMemberRepository();
      memberRepoReleases.setId(memberRepoId);
      routeResource.addRepository(memberRepoReleases);
    }

    Response response = this.routeUtil.sendMessage(Method.POST, routeResource);
    try {
      assertThat(response, isSuccessful());
      return response.getEntity().getText();
    }
    finally {
      RequestFacade.releaseResponse(response);
    }


  }
}
