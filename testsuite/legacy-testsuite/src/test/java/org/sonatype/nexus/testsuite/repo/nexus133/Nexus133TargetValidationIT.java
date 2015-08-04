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
 * Extra CRUD validation tests.
 */
public class Nexus133TargetValidationIT
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
  public void noPatternsTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("noPatternsTest");

    // List<String> patterns = new ArrayList<String>();
    // patterns.add( ".*foo.*" );
    // patterns.add( ".*bar.*" );
    // resource.setPatterns( patterns );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.startsWith("{\"errors\":"));
  }

  @Test
  public void noNameTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName(null);

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.startsWith("{\"errors\":"));
  }

  @Test
  public void invalidRegExTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("invalidRegExTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add("*.foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.startsWith("{\"errors\":"));
  }

  @Test
  public void invalidContentClass()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("INVALID_CLASS");
    resource.setName("invalidContentClass");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus() + "\n" + responseText);
    }
    Assert.assertTrue("Response text did not contain an error message. \nResponse Text:\n " + responseText,
        responseText.startsWith("{\"errors\":"));
  }

  @Test
  public void duplicateTargetTest()
      throws IOException
  {
    RepositoryTargetResource resource = new RepositoryTargetResource();
    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("duplicateTargetTest");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Target: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    String id1 = responseResource.getId();

    // make sure it was updated
    this.messageUtil.verifyTargetsConfig(responseResource);

    // send again
    response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create Target: " + response.getStatus());
    }

    // get the Resource object
    responseResource = this.messageUtil.getResourceFromResponse(response);
    String id2 = responseResource.getId();

    // make sure it was updated
    this.messageUtil.verifyTargetsConfig(responseResource);

    Assert.assertNotSame(id1, id2);

  }

  @Test
  public void updateValidation()
      throws IOException
  {
    RepositoryTargetResource resource = new RepositoryTargetResource();
    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("updateValidation");

    List<String> patterns = new ArrayList<String>();
    patterns.add(".*foo.*");
    resource.setPatterns(patterns);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not create user: " + response.getStatus());
    }

    // get the Resource object
    RepositoryTargetResource responseResource = this.messageUtil.getResourceFromResponse(response);

    // make sure the id != null
    Assert.assertTrue(StringUtils.isNotEmpty(responseResource.getId()));

    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    // verify config
    this.messageUtil.verifyTargetsConfig(responseResource);

    // update the Id
    resource.setId(responseResource.getId());

        /*
         * NO Name
         */
    resource.setContentClass("maven1");
    resource.setName(null);
    patterns.clear();
    patterns.add(".*new.*");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus());
    }
    String responseText = response.getEntity().getText();
    Assert.assertTrue("responseText does not contain an error message:\n" + responseText,
        responseText.startsWith("{\"errors\":"));

        /*
         * Invalid RegEx
         */

    resource.setContentClass("maven1");
    resource.setName("updateValidation");
    patterns.clear();
    patterns.add("*.new.*");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

        /*
         * NO Patterns
         */
    resource.setContentClass("maven1");
    resource.setName("updateValidation");
    patterns.clear();

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

        /*
         * NO Content Class
         */
    resource.setContentClass(null);
    resource.setName("updateValidation");
    patterns.clear();
    patterns.add(".*new.*");

    response = this.messageUtil.sendMessage(Method.PUT, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("Target should not have been created: " + response.getStatus());
    }
    Assert.assertTrue(response.getEntity().getText().startsWith("{\"errors\":"));

  }

  @Test
  public void maven1ContentClassTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven1");
    resource.setName("maven1ContentClassTest");

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

    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    this.messageUtil.verifyTargetsConfig(responseResource);
  }

  @Test
  public void maven2ContentClassTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("maven2");
    resource.setName("maven2ContentClassTest");

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

    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    this.messageUtil.verifyTargetsConfig(responseResource);
  }

  //@Test
  // eclipseContentClass is disabled for beta5!
  public void eclipseContentClassTest()
      throws IOException
  {
    // m2namespace
    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("eclipse-update-site");
    resource.setName("eclipseContentClassTest");

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

    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    this.messageUtil.verifyTargetsConfig(responseResource);
  }

  //@Test
  // m2NamespaceContentclass is disabled for beta5!
  public void m2NamespaceContentClassTest()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // resource.setId( "createTest" );
    resource.setContentClass("m2namespace");
    resource.setName("m2NamespaceContentClassTest");

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

    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    this.messageUtil.verifyTargetsConfig(responseResource);
  }

  @Test
  public void createTestWithId()
      throws IOException
  {

    RepositoryTargetResource resource = new RepositoryTargetResource();

    // FIXME: This should be allowed
    // resource.setId( "createTestWithId" );
    resource.setContentClass("maven1");
    resource.setName("createTestWithId");

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

    // FIXME: This should be allowed
    // Assert.assertEquals( resource.getId(), responseResource.getId() );
    Assert.assertEquals(responseResource.getContentClass(), resource.getContentClass());
    Assert.assertEquals(responseResource.getName(), resource.getName());
    Assert.assertEquals(resource.getPatterns(), responseResource.getPatterns());

    this.messageUtil.verifyTargetsConfig(responseResource);
  }

}
