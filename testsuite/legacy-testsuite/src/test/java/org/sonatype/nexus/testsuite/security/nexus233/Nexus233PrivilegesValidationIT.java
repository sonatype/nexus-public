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
package org.sonatype.nexus.testsuite.security.nexus233;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.security.realms.privileges.application.ApplicationPrivilegeDescriptor;

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
public class Nexus233PrivilegesValidationIT
    extends AbstractNexusIntegrationTest
{

  protected PrivilegesMessageUtil messageUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    this.messageUtil = new PrivilegesMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createWithInvalidMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("INVALID");
    resource.setMethod(methods);
    resource.setName("createWithInvalidMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }
    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void createNoMethodTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    // methods.add( "read" );
    resource.setMethod(methods);
    resource.setName("createNoMethodTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void createNoNameTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    // resource.setName( "createNoMethodTest" );
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId("testTarget");

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void createNoTypeTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("createNoTypeTest");
    // resource.setType( "target" );
    // resource.setRepositoryTargetId( "testTarget" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);

    if (response.getStatus().isSuccess()) {
      Assert.fail("No type, POST should've failed");
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void createNoRepoTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    resource.setMethod(methods);
    resource.setName("createNoRepoTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    // resource.setRepositoryTargetId( "testTarget" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void createWithInvalidAndValidMethodsTest()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();

    List methods = new ArrayList<String>();
    methods.add("read");
    methods.add("INVALID");
    resource.setMethod(methods);
    resource.setName("createWithInvalidAndValidMethodsTest");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    // resource.setRepositoryTargetId( "testTarget" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);

    Assert.assertNull(getSecurityConfigUtil().getCPrivilegeByName("createWithInvalidAndValidMethodsTest - (read)"));
  }

  @Test
  public void createApplicationResource()
      throws IOException
  {
    PrivilegeResource resource = new PrivilegeResource();
    resource.addMethod("read");
    resource.setName("createApplicationResource");
    resource.setType(ApplicationPrivilegeDescriptor.TYPE);
    // resource.setRepositoryTargetId( "testTarget" );

    Response response = this.messageUtil.sendMessage(Method.POST, resource);
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 400) {
      Assert.fail("Privilege should not have been created: " + response.getStatus() + "\nreponse:\n"
          + responseText);
    }

    this.messageUtil.validateResponseErrorXml(responseText);
  }

  @Test
  public void readInvalidIdTest()
      throws IOException
  {

    Response response = this.messageUtil.sendMessage(Method.GET, null, "INVALID");
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 404) {
      Assert.fail("A 404 should have been returned: " + response.getStatus() + "\nreponse:\n" + responseText);
    }

  }

  @Test
  public void deleteInvalidIdTest()
      throws IOException
  {

    Response response = this.messageUtil.sendMessage(Method.DELETE, null, "INVALID");
    String responseText = response.getEntity().getText();

    if (response.getStatus().getCode() != 404) {
      Assert.fail("A 404 should have been returned: " + response.getStatus() + "\nreponse:\n" + responseText);
    }

  }

}
