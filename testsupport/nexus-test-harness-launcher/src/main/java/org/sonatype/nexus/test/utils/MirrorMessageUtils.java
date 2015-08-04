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

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListRequest;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.restlet.data.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sonatype.nexus.test.utils.NexusRequestMatchers.isSuccessful;

public class MirrorMessageUtils
{
  private XStream xstream;

  private MediaType mediaType;

  private static final Logger LOG = LoggerFactory.getLogger(MirrorMessageUtils.class);

  public MirrorMessageUtils(XStream xstream, MediaType mediaType) {
    super();
    this.xstream = xstream;
    this.mediaType = mediaType;
  }

  public MirrorResourceListResponse getMirrors(String repositoryId)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = "service/local/repository_mirrors/" + repositoryId;

    String responseText = RequestFacade.doGetForText(serviceURI, representation, isSuccessful());
    LOG.debug(" getResourceFromResponse: " + responseText);

    representation = new XStreamRepresentation(xstream, responseText, mediaType);

    // this
    MirrorResourceListResponse resourceResponse =
        (MirrorResourceListResponse) representation.getPayload(new MirrorResourceListResponse());

    Assert.assertNotNull("Resource Response shouldn't be null", resourceResponse);

    for (MirrorResource resource : (List<MirrorResource>) resourceResponse.getData()) {
      Assert.assertNotNull("Id shouldn't be null", resource.getId());
    }

    return resourceResponse;
  }

  public MirrorResourceListResponse setMirrors(String repositoryId, MirrorResourceListRequest resourceRequest)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = "service/local/repository_mirrors/" + repositoryId;

    // now set the payload
    representation.setPayload(resourceRequest);

    String responseText = RequestFacade.doPostForText(serviceURI, representation, isSuccessful());
    LOG.debug(" getResourceFromResponse: " + responseText);

    representation = new XStreamRepresentation(xstream, responseText, mediaType);

    // this
    MirrorResourceListResponse resourceResponse =
        (MirrorResourceListResponse) representation.getPayload(new MirrorResourceListResponse());

    Assert.assertNotNull("Resource Response shouldn't be null", resourceResponse);

    for (MirrorResource resource : (List<MirrorResource>) resourceResponse.getData()) {
      Assert.assertNotNull(resource.getId(), "Id shouldn't be null");
    }

    for (int i = 0; i < resourceResponse.getData().size(); i++) {
      Assert.assertEquals(((MirrorResource) resourceRequest.getData().get(i)).getUrl(),
          ((MirrorResource) resourceResponse.getData().get(i)).getUrl());
    }

    return resourceResponse;
  }


  public MirrorResourceListResponse getPredefinedMirrors(String repositoryId)
      throws IOException
  {
    XStreamRepresentation representation = new XStreamRepresentation(xstream, "", mediaType);

    String serviceURI = "service/local/repository_predefined_mirrors/" + repositoryId;

    String responseText = RequestFacade.doGetForText(serviceURI, representation, isSuccessful());
    LOG.debug(" getResourceFromResponse: " + responseText);

    representation = new XStreamRepresentation(xstream, responseText, mediaType);

    // this
    MirrorResourceListResponse resourceResponse =
        (MirrorResourceListResponse) representation.getPayload(new MirrorResourceListResponse());

    Assert.assertNotNull("Resource Response shouldn't be null", resourceResponse);

    for (MirrorResource resource : (List<MirrorResource>) resourceResponse.getData()) {
      Assert.assertNotNull(resource.getUrl(), "URL shouldn't be null");
    }

    return resourceResponse;
  }
}
