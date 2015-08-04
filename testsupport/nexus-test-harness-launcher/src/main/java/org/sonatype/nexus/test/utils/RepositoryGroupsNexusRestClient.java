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

import org.sonatype.nexus.integrationtests.NexusRestClient;
import org.sonatype.nexus.rest.model.RepositoryGroupListResource;
import org.sonatype.nexus.rest.model.RepositoryGroupListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class RepositoryGroupsNexusRestClient
{

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryGroupsNexusRestClient.class);

  public static final String SERVICE_PART = NexusRestClient.SERVICE_LOCAL + "repo_groups";

  private final NexusRestClient nexusRestClient;

  private final XStream xstream;

  private final MediaType mediaType;

  public RepositoryGroupsNexusRestClient(final NexusRestClient nexusRestClient) {
    this(nexusRestClient, XStreamFactory.getJsonXStream(), MediaType.APPLICATION_JSON);
  }

  public RepositoryGroupsNexusRestClient(final NexusRestClient nexusRestClient,
                                         final XStream xstream,
                                         final MediaType mediaType)
  {
    this.nexusRestClient = checkNotNull(nexusRestClient);
    this.xstream = checkNotNull(xstream);
    this.mediaType = checkNotNull(mediaType);
  }

  public RepositoryGroupResource createGroup(final RepositoryGroupResource group)
      throws IOException
  {
    XStreamRepresentation representation = request(group);

    String payload = nexusRestClient.doPostForText(SERVICE_PART, representation, isSuccessful());

    return response(payload);
  }

  public RepositoryGroupResource updateGroup(final RepositoryGroupResource group)
      throws IOException
  {
    XStreamRepresentation representation = request(group);

    String payload = nexusRestClient.doPutForText(SERVICE_PART + "/" + group.getId(), representation,
        isSuccessful());

    return response(payload);
  }

  private RepositoryGroupResource response(final String payload) {
    XStreamRepresentation representation = representation();

    representation.setText(payload);

    RepositoryGroupResourceResponse resourceResponse =
        (RepositoryGroupResourceResponse) representation.getPayload(new RepositoryGroupResourceResponse());

    return resourceResponse.getData();
  }

  private XStreamRepresentation request(final RepositoryGroupResource group) {
    XStreamRepresentation representation = representation();

    RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
    request.setData(group);
    representation.setPayload(request);
    return representation;
  }

  private XStreamRepresentation representation() {
    return new XStreamRepresentation(xstream, "", mediaType);
  }

  public RepositoryGroupResource getGroup(String groupId)
      throws IOException
  {
    String responseText = nexusRestClient.doGetForText(SERVICE_PART + "/" + groupId);
    LOG.debug("responseText: \n" + responseText);

    // this should use call to: getResourceFromResponse
    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    RepositoryGroupResourceResponse resourceResponse =
        (RepositoryGroupResourceResponse) representation.getPayload(new RepositoryGroupResourceResponse());

    return resourceResponse.getData();
  }

  /**
   * IMPORTANT: Make sure to release the Response in a finally block when you are done with it.
   */
  public Response sendMessage(Method method, RepositoryGroupResource resource, String id)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String idPart = (method == Method.POST) ? "" : "/" + id;
    String serviceURI = SERVICE_PART + idPart;

    RepositoryGroupResourceResponse repoResponseRequest = new RepositoryGroupResourceResponse();
    repoResponseRequest.setData(resource);

    // now set the payload
    representation.setPayload(repoResponseRequest);

    LOG.debug("sendMessage: " + representation.getText());

    return nexusRestClient.sendMessage(serviceURI, method, representation);
  }

  public List<RepositoryGroupListResource> getList()
      throws IOException
  {
    String responseText = nexusRestClient.doGetForText(SERVICE_PART);
    LOG.debug("responseText: \n" + responseText);

    XStreamRepresentation representation =
        new XStreamRepresentation(XStreamFactory.getXmlXStream(), responseText, MediaType.APPLICATION_XML);

    RepositoryGroupListResourceResponse resourceResponse =
        (RepositoryGroupListResourceResponse) representation.getPayload(new RepositoryGroupListResourceResponse());

    return resourceResponse.getData();

  }
}
