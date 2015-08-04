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
package org.sonatype.nexus.testsuite.repo.nexus133;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.nexus.test.utils.TargetMessageUtil;

import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * CRUD tests for JSON request/response.
 */
public class Nexus133TargetCrudJsonIT
    extends AbstractNexusIntegrationTest
{

  protected TargetMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new TargetMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  @Test
  public void createTargetTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("createTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    patterns.add(".*bar.*");
    resource.setPatterns(patterns);

    this.messageUtil.createTarget(resource);
  }

  public void readTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("readTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    patterns.add(".*bar.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Repository Target: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure the id != null
    Assert.assertTrue(StringUtils.isNotEmpty(responseResource.getId()));

    // make sure it was added
    this.messageUtil.verifyTargetsConfig(responseResource);

    // update the Id
    resource.setId(responseResource.getId());

    response = this.messageUtil.sendMessage(Method.GET, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not GET Repository Target: " + response.getStatus());
    }

    // get the Resource object
    responseResource = this.messageUtil.getResourceFromResponse(response);

    // validate
    this.messageUtil.verifyTargetsConfig(responseResource);

  }

  @Test
  public void listTest() throws IOException {
    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "listTest" );
    resource.setContentClass("maven1");
    resource.setName("listTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Repository Target: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure the id != null
    Assert.assertTrue(StringUtils.isNotEmpty(responseResource.getId()));

    // make sure it was added
    this.messageUtil.verifyTargetsConfig(responseResource);

    // now that we have at least one element stored (more from other tests, most likely)


    // NEED to work around a GET problem with the REST client
    List<RepositoryTargetListResource> targets = TargetMessageUtil.getList();
    // the response is a list of RepositoryTargetListResource, so we need a different compare method.
    this.messageUtil.verifyCompleteTargetsConfig(targets);


  }

  @Test
  public void udpateTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("udpateTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    patterns.add(".*bar.*");
    resource.setPatterns(patterns);

    resource = this.messageUtil.createTarget(resource);

    resource.setName("udpateTestRenamed");
    resource.setContentClass("maven2");
    patterns.clear();
    patterns.add(".*new.*");
    patterns.add(".*patterns.*");

    Response response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Repository Target: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure it was updated
    this.messageUtil.verifyTargetsConfig(responseResource);

  }

  @Test
  public void deleteTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("deleteTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    patterns.add(".*bar.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Repository Target: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure the id != null
    Assert.assertTrue(StringUtils.isNotEmpty(responseResource.getId()));

    // make sure it was added so we know if it was removed
    this.messageUtil.verifyTargetsConfig(responseResource);

    // use the new IDs
    // response = this.deleteTarget( responseResource.getId() );

    response = this.messageUtil.sendMessage(Method.DELETE, responseResource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not delete Repository Target: " + response.getStatus());
    }

    this.messageUtil.verifyTargetsConfig(new ArrayList<RepositoryTargetResource>());
  }


}
