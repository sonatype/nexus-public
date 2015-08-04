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
package org.sonatype.nexus.testsuite.misc.nexus930;

import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.PlexusComponentListResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResource;
import org.sonatype.nexus.rest.model.RepositoryContentClassListResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * Test the AutoDiscoverComponent a
 */
public class Nexus930AutoDiscoverComponentIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testInvalidRole()
      throws Exception
  {
    Response response1 = sendMessage("JUNK-foo-Bar-JUNK", this.getXMLXStream(), MediaType.APPLICATION_XML);
    Assert.assertTrue(response1.getStatus().isClientError());
    Assert.assertEquals(404, response1.getStatus().getCode());
  }

  @Test
  public void testContentClassComponentListPlexusResource()
      throws Exception
  {
    String role = "repo_content_classes";
    // do admin
    List<RepositoryContentClassListResource> result1 =
        this.getContentClasses(this.getXMLXStream(), MediaType.APPLICATION_XML);
    Assert.assertTrue(result1.size() > 0);

    // 403 test
    this.overwriteUserRole(TEST_USER_NAME, "login-only" + role, "2");
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
    this.getContentClasses(this.getXMLXStream(), MediaType.APPLICATION_XML, 403);

    // only content class priv
    this.overwriteUserRole(TEST_USER_NAME, "content-classes" + role, "70");
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
    Assert.assertNotNull(this.getContentClasses(this.getXMLXStream(), MediaType.APPLICATION_XML));
  }

  @Test
  public void testScheduledTaskTypeComonentListPlexusResource()
      throws Exception
  {
    String role = "schedule_types";
    // do admin
    List<PlexusComponentListResource> result1 =
        this.getResult(role, this.getXMLXStream(), MediaType.APPLICATION_XML);
    Assert.assertTrue("Expected list larger then 1.", result1.size() > 1);

    // 403 test
    this.overwriteUserRole(TEST_USER_NAME, "login-only" + role, "2");
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
    Response response = sendMessage(role, this.getXMLXStream(), MediaType.APPLICATION_XML);
    Assert.assertTrue("Expected Error: Status was: " + response.getStatus().getCode(),
        response.getStatus().isClientError());
    Assert.assertEquals(403, response.getStatus().getCode());

    // only content class priv
    this.overwriteUserRole(TEST_USER_NAME, "schedule_types" + role, "71");
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);
    response = sendMessage(role, this.getXMLXStream(), MediaType.APPLICATION_XML);
    Assert.assertTrue(response.getStatus().isSuccess());

  }

  private List<RepositoryContentClassListResource> getContentClasses(XStream xstream, MediaType mediaType)
      throws IOException
  {
    return getContentClasses(xstream, mediaType, -1);
  }

  private List<RepositoryContentClassListResource> getContentClasses(XStream xstream, MediaType mediaType,
                                                                     int failureId)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = "service/local/components/repo_content_classes";

    Response response = RequestFacade.sendMessage(serviceURI, Method.GET, representation);

    if (failureId > -1) {
      Assert.assertEquals(failureId, response.getStatus().getCode());
      return null;
    }
    else {
      String responseString = response.getEntity().getText();

      representation = new XStreamRepresentation(xstream, responseString, mediaType);

      RepositoryContentClassListResourceResponse resourceResponse =
          (RepositoryContentClassListResourceResponse) representation
              .getPayload(new RepositoryContentClassListResourceResponse());

      return resourceResponse.getData();
    }
  }

  private List<PlexusComponentListResource> getResult(String role, XStream xstream, MediaType mediaType)
      throws IOException
  {
    String responseString = this.sendMessage(role, xstream, mediaType).getEntity().getText();

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);

    PlexusComponentListResourceResponse resourceResponse =
        (PlexusComponentListResourceResponse) representation.getPayload(new PlexusComponentListResourceResponse());

    return resourceResponse.getData();
  }

  private Response sendMessage(String role, XStream xstream, MediaType mediaType)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = "service/local/components/" + role;

    return RequestFacade.sendMessage(serviceURI, Method.GET, representation);
  }

}
