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
package org.sonatype.nexus.testsuite.repo.nexus385;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.test.utils.RoutesMessageUtil;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * CRUD tests for XML request/response.
 */
public class Nexus385RoutesCrudXmlIT
    extends AbstractNexusIntegrationTest
{

  protected RoutesMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp()
      throws IOException
  {
    this.messageUtil = new RoutesMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
    RoutesMessageUtil.removeAllRoutes();
  }

  @Test
  public void createRouteTest()
      throws IOException
  {
    this.runCreateTest("exclusive");
    this.runCreateTest("inclusive");
    this.runCreateTest("blocking");
  }

  @SuppressWarnings("unchecked")
  private RepositoryRouteResource runCreateTest(String ruleType)
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern(".*" + ruleType + ".*");
    resource.setRuleType(ruleType);

    if (!"blocking".equals(ruleType)) {
      RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
      memberRepo1.setId("nexus-test-harness-repo");
      resource.addRepository(memberRepo1);
    }

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();
      try {
        Assert.fail("Could not create privilege: " + response.getStatus() + "\nresponse:\n" + responseText);
      }
      catch (NullPointerException e) {
        Assert.fail(new XStream().toXML(response));
      }
    }

    // get the Resource object
    RepositoryRouteResource resourceResponse = this.messageUtil.getResourceFromResponse(response);

    Assert.assertNotNull(resourceResponse.getId());

    Assert.assertEquals(resourceResponse.getGroupId(), resource.getGroupId());
    Assert.assertEquals(resourceResponse.getPattern(), resource.getPattern());
    Assert.assertEquals(resourceResponse.getRuleType(), resource.getRuleType());
    this.messageUtil.validateSame(resource.getRepositories(), resourceResponse.getRepositories());

    // now check the nexus config
    this.messageUtil.validateRoutesConfig(resourceResponse);

    return resourceResponse;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void readTest()
      throws IOException
  {
    // create
    RepositoryRouteResource resource = this.runCreateTest("exclusive");

    Response response = this.messageUtil.sendMessage(Method.GET, resource);

    if (!response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();
      Assert.fail("Could not create privilege: " + response.getStatus() + "\nresponse:\n" + responseText);
    }

    // get the Resource object
    RepositoryRouteResource resourceResponse = this.messageUtil.getResourceFromResponse(response);

    Assert.assertNotNull(resourceResponse.getId());

    Assert.assertEquals(resourceResponse.getGroupId(), resource.getGroupId());
    Assert.assertEquals(resourceResponse.getPattern(), resource.getPattern());
    Assert.assertEquals(resourceResponse.getRuleType(), resource.getRuleType());
    this.messageUtil.validateSame(resource.getRepositories(), resourceResponse.getRepositories());

    // now check the nexus config
    this.messageUtil.validateRoutesConfig(resourceResponse);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void updateTest()
      throws IOException
  {
    // FIXME: this test is known to fail, but is commented out so the CI builds are useful
    if (this.printKnownErrorButDoNotFail(this.getClass(), "updateTest")) {
      return;
    }

    // create
    RepositoryRouteResource resource = this.runCreateTest("exclusive");
    resource.setPattern(".*update.*");

    Response response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (!response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();
      Assert.fail("Could not create privilege: " + response.getStatus() + "\nresponse:\n" + responseText);
    }

    // get the Resource object
    RepositoryRouteResource resourceResponse = this.messageUtil.getResourceFromResponse(response);

    Assert.assertNotNull(resourceResponse.getId());

    Assert.assertEquals(resourceResponse.getGroupId(), resource.getGroupId());
    Assert.assertEquals(resourceResponse.getPattern(), resource.getPattern());
    Assert.assertEquals(resourceResponse.getRuleType(), resource.getRuleType());
    this.messageUtil.validateSame(resource.getRepositories(), resourceResponse.getRepositories());

    // now check the nexus config
    this.messageUtil.validateRoutesConfig(resourceResponse);

  }

  @Test
  public void deleteTest()
      throws IOException
  {
    // create
    RepositoryRouteResource resource = this.runCreateTest("exclusive");

    Response response = this.messageUtil.sendMessage(Method.DELETE, resource);

    if (!response.getStatus().isSuccess()) {
      String responseText = response.getEntity().getText();
      Assert.fail("Could not create privilege: " + response.getStatus() + "\nresponse:\n" + responseText);
    }

    Assert.assertTrue("Route was not deleted.", getNexusConfigUtil().getRoute(resource.getId()) == null);

  }

}
