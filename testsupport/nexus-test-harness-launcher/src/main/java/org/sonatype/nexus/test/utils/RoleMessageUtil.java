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
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.security.rest.model.ExternalRoleMappingListResourceResponse;
import org.sonatype.security.rest.model.ExternalRoleMappingResource;
import org.sonatype.security.rest.model.PlexusRoleListResourceResponse;
import org.sonatype.security.rest.model.PlexusRoleResource;
import org.sonatype.security.rest.model.RoleListResourceResponse;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.RoleResourceRequest;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class RoleMessageUtil
    extends ITUtil
{
  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(RoleMessageUtil.class);

  public RoleMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public RoleResource createRole(RoleResource role)
      throws IOException
  {
    Response response = null;
    String entityText;
    try {
      response = this.sendMessage(Method.POST, role);
      entityText = response.getEntity().getText();
      assertThat("Could not create role", response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
    // get the Resource object
    RoleResource responseResource = this.getResourceFromResponse(entityText);

    // make sure the id != null
    Assert.assertNotNull("Result:\n" + this.xStream.toXML(responseResource), responseResource.getId());

    if (role.getId() != null) {
      Assert.assertEquals(responseResource.getId(), role.getId());
    }

    Assert.assertEquals(responseResource.getDescription(), role.getDescription());
    Assert.assertEquals(responseResource.getName(), role.getName());
    Assert.assertEquals(role.getSessionTimeout(), responseResource.getSessionTimeout());
    Assert.assertEquals(role.getPrivileges(), responseResource.getPrivileges());
    Assert.assertEquals(role.getRoles(), responseResource.getRoles());

    getTest().getSecurityConfigUtil().verifyRole(responseResource);

    return responseResource;
  }

  public RoleResource getRole(String roleId)
      throws IOException
  {

    Response response = null;
    try {
      response = this.sendMessage(Method.GET, null, roleId);
      RoleResource resource = this.getResourceFromResponse(response);
      assertThat("Could not find role", response, isSuccessful());
      return resource;
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RoleResource resource)
      throws IOException
  {
    return this.sendMessage(method, resource, resource.getId());
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  private Response sendMessage(Method method, RoleResource resource, String id)
      throws IOException
  {

    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String roleId = (method == Method.POST) ? "" : "/" + id;

    String serviceURI = "service/local/roles" + roleId;

    if (method == Method.POST || method == Method.PUT) {
      RoleResourceRequest userRequest = new RoleResourceRequest();
      userRequest.setData(resource);

      // now set the payload
      representation.setPayload(userRequest);
    }

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  /**
   * This should be replaced with a REST Call, but the REST client does not set the Accept correctly on GET's/
   */
  @SuppressWarnings("unchecked")
  public List<RoleResource> getList()
      throws IOException
  {
    Response response = null;
    String entityText;
    try {
      response = RequestFacade.doGetRequest("service/local/roles");
      entityText = response.getEntity().getText();
      assertThat(response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), entityText, MediaType.APPLICATION_XML);

    RoleListResourceResponse resourceResponse =
        (RoleListResourceResponse) representation.getPayload(new RoleListResourceResponse());

    return resourceResponse.getData();

  }

  public RoleResource getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();
    LOG.debug(" getResourceFromResponse: " + responseString);

    return getResourceFromResponse(responseString);
  }

  public RoleResource getResourceFromResponse(String responseString)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);
    RoleResourceRequest roleResourceRequest =
        (RoleResourceRequest) representation.getPayload(new RoleResourceRequest());

    return roleResourceRequest.getData();
  }

  private static XStream xStream;

  static {
    xStream = XStreamFactory.getXmlXStream();
  }

  public static Status update(RoleResource role)
      throws IOException
  {
    RoleResourceRequest request = new RoleResourceRequest();
    request.setData(role);

    XStreamRepresentation representation = new XStreamRepresentation(xStream, "", MediaType.APPLICATION_XML);
    representation.setPayload(request);

    String serviceURI = "service/local/roles/" + role.getId();
    return RequestFacade.doPutForStatus(serviceURI, representation, isSuccessful());
  }

  /**
   * @param roleId the role id to find
   * @return null if role not found, otherwise the resource
   */
  public RoleResource findRole(String roleId)
      throws IOException
  {
    Response response = null;
    try {
      response = this.sendMessage(Method.GET, null, roleId);
      if (!response.getStatus().isSuccess()) {
        return null;
      }
      return this.getResourceFromResponse(response);
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

  @SuppressWarnings("unchecked")
  public List<ExternalRoleMappingResource> getExternalRoleMap(String source)
      throws IOException
  {
    // external_role_map
    String uriPart = RequestFacade.SERVICE_LOCAL + "external_role_map/" + source;
    Response response = null;
    String entityText;
    try {
      response = RequestFacade.sendMessage(uriPart, Method.GET, new StringRepresentation("", this.mediaType));
      entityText = response.getEntity().getText();
      assertThat(response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }

    ExternalRoleMappingListResourceResponse result =
        (ExternalRoleMappingListResourceResponse) this.parseResponseText(entityText,
            new ExternalRoleMappingListResourceResponse());

    return result.getData();
  }

  @SuppressWarnings("unchecked")
  public List<PlexusRoleResource> getRoles(String source)
      throws IOException
  {
    // plexus_roles
    String uriPart = RequestFacade.SERVICE_LOCAL + "plexus_roles/" + source;


    Response response = null;
    String entityText;
    try {
      response = RequestFacade.sendMessage(uriPart, Method.GET, new StringRepresentation("", this.mediaType));
      entityText = response.getEntity().getText();
      assertThat(response, isSuccessful());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
    LOG.debug("response: " + entityText);

    PlexusRoleListResourceResponse result =
        (PlexusRoleListResourceResponse) this.parseResponseText(entityText,
            new PlexusRoleListResourceResponse());

    return result.getData();
  }

  public Object parseResponseText(String responseString, Object responseType)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);

    return representation.getPayload(responseType);
  }

}
