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
package org.sonatype.nexus.test.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.PrivilegeResourceRequest;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.security.rest.model.PrivilegeListResourceResponse;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.PrivilegeStatusResourceResponse;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class PrivilegesMessageUtil
    extends ITUtil
{
  private XStream xstream;

  private MediaType mediaType;

  private static Logger log = LoggerFactory.getLogger(PrivilegesMessageUtil.class);

  public PrivilegesMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public List<PrivilegeStatusResource> createPrivileges(PrivilegeResource resource)
      throws IOException
  {
    Response response = null;
    List<PrivilegeStatusResource> statusResources;
    try {
      response = this.sendMessage(Method.POST, resource);
      statusResources = this.getResourceListFromResponse(response);
      assertThat(response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    getTest().getSecurityConfigUtil().verifyPrivileges(statusResources);

    return statusResources;
  }

  public PrivilegeStatusResource getPrivilegeResource(String id)
      throws IOException
  {
    Response response = this.sendMessage(Method.GET, null, id);
    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not get Privilege: " + response.getStatus() + "\n" + response.getEntity().getText());
    }
    return this.getResourceFromResponse(response);
  }

  public List<PrivilegeStatusResource> getList()
      throws IOException
  {
    Response response = this.sendMessage(Method.GET, null);
    if (!response.getStatus().isSuccess()) {
      Assert.fail("Could not get Privilege: " + response.getStatus() + "\n" + response.getEntity().getText());
    }
    return this.getResourceListFromResponse(response);
  }

  public Response sendMessage(Method method, PrivilegeResource resource)
      throws IOException
  {
    return this.sendMessage(method, resource, "");
  }

  public Response sendMessage(Method method, PrivilegeResource resource, String id)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String privId = (method == Method.POST) ? "" : "/" + id;
    String serviceURI = "service/local/privileges" + privId;

    if (method == Method.POST) {
      serviceURI += "_target";
    }

    if (method == Method.POST || method == Method.PUT) // adding put so we can check for the 405, without a
    // resource you get a 400
    {
      PrivilegeResourceRequest requestResponse = new PrivilegeResourceRequest();
      requestResponse.setData(resource);

      // now set the payload
      representation.setPayload(requestResponse);
      log.debug(method.getName() + ": " + representation.getText());
    }

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  public PrivilegeStatusResource getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);

    PrivilegeStatusResourceResponse resourceResponse =
        (PrivilegeStatusResourceResponse) representation.getPayload(new PrivilegeStatusResourceResponse());

    return resourceResponse.getData();

  }

  public List<PrivilegeStatusResource> getResourceListFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();

    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);

    PrivilegeListResourceResponse resourceResponse =
        (PrivilegeListResourceResponse) representation.getPayload(new PrivilegeListResourceResponse());

    return resourceResponse.getData();
  }

  public void validateResponseErrorXml(String xml) {
    // to trick xstream
    // REMEMBER! You cannot use the XStreamInitializer 1:1 from Server!
    // It does n->1 mapping (maps different types to field data), while the client
    // has to do 1->n mapping (it knows what _will_ 'data' field contain)
    xstream.alias("data", PrivilegeListResourceResponse.class);

    ErrorResponse errorResponse = (ErrorResponse) xstream.fromXML(xml, new ErrorResponse());

    Assert.assertTrue("Error response is empty.", errorResponse.getErrors().size() > 0);

    for (Iterator<ErrorMessage> iter = errorResponse.getErrors().iterator(); iter.hasNext(); ) {
      ErrorMessage error = iter.next();
      Assert.assertFalse("Response Error message is empty.", StringUtils.isEmpty(error.getMsg()));

    }

  }

  public void assertExists(String... ids)
      throws IOException
  {
    StringBuilder result = new StringBuilder();
    for (String id : ids) {
      Response response = this.sendMessage(Method.GET, null, id);
      if (!response.getStatus().isSuccess()) {
        result.append("Privilege not found '").append(id).append("': ");
        result.append(response.getEntity().getText()).append('\n');
      }
    }

    if (result.length() != 0) {
      Assert.fail(result.toString());
    }
  }

  public void assertNotExists(String... ids)
      throws IOException
  {
    StringBuilder result = new StringBuilder();
    for (String id : ids) {
      Response response = this.sendMessage(Method.GET, null, id);
      if (!Status.CLIENT_ERROR_NOT_FOUND.equals(response.getStatus())) {
        result.append("Privilege shouldn't exist '").append(id).append("': ");
        result.append(response.getEntity().getText()).append('\n');
      }
    }

    if (result.length() != 0) {
      Assert.fail(result.toString());
    }
  }

  public Response delete(String privId)
      throws IOException
  {
    return RequestFacade.sendMessage("service/local/privileges/" + privId, Method.DELETE);
  }

}
