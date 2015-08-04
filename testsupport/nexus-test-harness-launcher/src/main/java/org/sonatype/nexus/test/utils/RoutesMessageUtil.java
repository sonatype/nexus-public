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
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryRouteListResource;
import org.sonatype.nexus.rest.model.RepositoryRouteListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryRouteMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryRouteResource;
import org.sonatype.nexus.rest.model.RepositoryRouteResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;

import com.thoughtworks.xstream.XStream;
import org.codehaus.plexus.util.StringUtils;
import org.hamcrest.text.IsEmptyString;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class RoutesMessageUtil
    extends ITUtil
{
  public static final String SERVICE_PART = RequestFacade.SERVICE_LOCAL + "repo_routes";

  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(RoutesMessageUtil.class);

  public RoutesMessageUtil(AbstractNexusIntegrationTest test, XStream xstream, MediaType mediaType) {
    super(test);
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public RepositoryRouteResource getRoute(String routeId)
      throws IOException
  {
    Response response = null;
    try {
      response = getRouteResponse(routeId);

      assertThat(response, isSuccessful());

      return this.getResourceFromText(response.getEntity().getText());
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response getRouteResponse(String routeId)
      throws IOException
  {
    Response response = RequestFacade.doGetRequest("service/local/repo_routes/" + routeId);
    return response;
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RepositoryRouteResource resource)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String resourceId = (resource.getId() == null) ? "" : "/" + resource.getId();
    String serviceURI = "service/local/repo_routes" + resourceId;

    if (method != Method.GET || method != Method.DELETE) {
      RepositoryRouteResourceResponse requestResponse = new RepositoryRouteResourceResponse();
      requestResponse.setData(resource);

      // now set the payload
      representation.setPayload(requestResponse);
    }

    return RequestFacade.sendMessage(serviceURI, method, representation);
  }

  /**
   * Use {@link #getResourceFromText(String)} instead.
   */
  @Deprecated
  public RepositoryRouteResource getResourceFromResponse(Response response)
      throws IOException
  {
    String responseString = response.getEntity().getText();
    LOG.debug("responseText: " + responseString);

    Assert.assertTrue(response.getStatus() + "\n" + responseString, response.getStatus().isSuccess());

    return getResourceFromText(responseString);
  }

  public RepositoryRouteResource getResourceFromText(String responseString) {
    assertThat(responseString, not(IsEmptyString.isEmptyOrNullString()));
    XStreamRepresentation representation = new XStreamRepresentation(xstream, responseString, mediaType);

    RepositoryRouteResourceResponse resourceResponse =
        (RepositoryRouteResourceResponse) representation.getPayload(new RepositoryRouteResourceResponse());

    return resourceResponse.getData();
  }

  public void validateSame(List<RepositoryRouteMemberRepository> repos1, List<RepositoryRouteMemberRepository> repos2) {
    Assert.assertEquals(repos1.size(), repos2.size());

    for (int ii = 0; ii < repos1.size(); ii++) {
      RepositoryRouteMemberRepository repo1 = repos1.get(ii);
      RepositoryRouteMemberRepository repo2 = repos2.get(ii);
      this.validateSame(repo1, repo2);
    }
  }

  public void validateSameRepoIds(List<RepositoryRouteMemberRepository> repos1, List<String> repos2) {
    Assert.assertEquals(repos1.size(), repos2.size());

    // this is ordered
    for (int ii = 0; ii < repos1.size(); ii++) {
      RepositoryRouteMemberRepository repo1 = repos1.get(ii);
      String repo2 = repos2.get(ii);
      Assert.assertEquals(repo2, repo1.getId());
    }
  }

  public void validateSame(RepositoryRouteMemberRepository repo1, RepositoryRouteMemberRepository repo2) {
    // we only care about the Id field
    Assert.assertEquals(repo2.getId(), repo1.getId());
  }

  public void validateRoutesConfig(RepositoryRouteResource resource)
      throws IOException
  {

    CPathMappingItem cRoute = getTest().getNexusConfigUtil().getRoute(resource.getId());

    String msg =
        "Should be the same route. \n Expected:\n" + new XStream().toXML(resource) + " \n \n Got: \n"
            + new XStream().toXML(cRoute);

    Assert.assertEquals(msg, cRoute.getId(), resource.getId());
    Assert.assertEquals(msg, cRoute.getGroupId(), resource.getGroupId());
    Assert.assertEquals(msg, cRoute.getRoutePatterns(), Collections.singletonList(resource.getPattern()));
    Assert.assertEquals(msg, cRoute.getRouteType(), resource.getRuleType());

    this.validateSameRepoIds(resource.getRepositories(), cRoute.getRepositories());

  }

  public void validateResponseErrorXml(String xml) {

    ErrorResponse errorResponse = (ErrorResponse) xstream.fromXML(xml, new ErrorResponse());

    Assert.assertTrue("Error response is empty.", errorResponse.getErrors().size() > 0);

    for (Iterator<ErrorMessage> iter = errorResponse.getErrors().iterator(); iter.hasNext(); ) {
      ErrorMessage error = iter.next();
      Assert.assertFalse("Response Error message is empty.", StringUtils.isEmpty(error.getMsg()));

    }

  }

  @SuppressWarnings("unchecked")
  public static List<RepositoryRouteListResource> getList()
      throws IOException
  {
    String serviceURI = "service/local/repo_routes";

    String entityText = RequestFacade.doGetForText(serviceURI);
    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), entityText,
            MediaType.APPLICATION_XML);

    RepositoryRouteListResourceResponse resourceResponse =
        (RepositoryRouteListResourceResponse) representation.getPayload(new RepositoryRouteListResourceResponse());

    return resourceResponse.getData();
  }

  public static void removeAllRoutes()
      throws IOException
  {
    List<RepositoryRouteListResource> routes = getList();
    for (RepositoryRouteListResource route : routes) {
      Status status = delete(route.getResourceURI()).getStatus();
      Assert.assertTrue("Unable to delete route: '" + route.getResourceURI() + "', due to: "
          + status.getDescription(), status.isSuccess());
    }
  }

  public static Response delete(String resourceUri)
      throws IOException
  {
    return RequestFacade.sendMessage(new URL(resourceUri), Method.DELETE, null);
  }

}
