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
public class Nexus385RoutesValidationIT
    extends AbstractNexusIntegrationTest
{

  protected RoutesMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new RoutesMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void createNoGroupIdTest()
      throws IOException
  {
    // EXPLANATION
    // When no groupId sent with route, Nexus _defaults_ it to '*', meaning
    // all repositories to "mimic" the pre-this-change behaviour

    RepositoryRouteResource resource = new RepositoryRouteResource();
    // resource.setGroupId( "nexus-test" );
    resource.setPattern(".*createNoGroupIdTest.*");
    resource.setRuleType("exclusive");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 201 || !responseText.contains("<groupId>*</groupId>")) {
      Assert.fail("Should have returned a 201, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText + ", and the omitted groupId should be defaulted with '*'");
    }
  }

  @Test
  public void createNoRuleTypeTest()
      throws IOException
  {

    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern(".*createNoRuleTypeTest.*");
    // resource.setRuleType( "exclusive" );

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @Test
  public void createNoPatternTest()
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    // resource.setPattern( ".*createNoPatternTest.*" );
    resource.setRuleType("exclusive");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @Test
  public void createWithInvalidPatternTest()
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern("*.createWithInvalidPatternTest.*");
    resource.setRuleType("exclusive");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @Test
  public void createWithInvalidGroupTest()
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("INVALID");
    resource.setPattern("*.createWithInvalidPatternTest.*");
    resource.setRuleType("exclusive");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);
  }

  @Test
  public void createWithInvalidRuleTypeTest()
      throws IOException
  {

    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern("*.createWithInvalidRuleTypeTest.*");
    resource.setRuleType("createWithInvalidRuleTypeTest");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("nexus-test-harness-repo");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);
  }

  @Test
  public void createNoReposTest()
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern("*.createWithInvalidRuleTypeTest.*");
    resource.setRuleType("exclusive");

    // RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    // memberRepo1.setId( "nexus-test-harness-repo" );
    // resource.addRepository( memberRepo1 );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);
  }

  @Test
  public void createWithInvalidReposTest()
      throws IOException
  {
    RepositoryRouteResource resource = new RepositoryRouteResource();
    resource.setGroupId("nexus-test");
    resource.setPattern("*.createWithInvalidRuleTypeTest.*");
    resource.setRuleType("exclusive");

    RepositoryRouteMemberRepository memberRepo1 = new RepositoryRouteMemberRepository();
    memberRepo1.setId("INVALID");
    resource.addRepository(memberRepo1);

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    String responseText = response.getEntity().getText();
    if (response.getStatus().getCode() != 400) {
      Assert.fail("Should have returned a 400, but returned: " + response.getStatus() + "\nresponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);
  }

}
