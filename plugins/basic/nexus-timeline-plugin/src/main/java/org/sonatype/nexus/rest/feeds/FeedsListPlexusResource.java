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
package org.sonatype.nexus.rest.feeds;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.feeds.sources.FeedSource;
import org.sonatype.nexus.rest.model.FeedListResource;
import org.sonatype.nexus.rest.model.FeedListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A resource that lists existing feeds.
 *
 * @author cstamas
 * @author dip
 */
@Path(FeedsListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
@Named
@Singleton
public class FeedsListPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String RESOURCE_URI = "/feeds";

  private final List<FeedSource> feeds;

  @Inject
  public FeedsListPlexusResource(final List<FeedSource> feeds) {
    this.feeds = checkNotNull(feeds);
  }

  @Override
  public Object getPayloadInstance() {
    // RO resource, no payload
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:feeds]");
  }

  /**
   * Get the list of feeds available from the nexus server.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = FeedListResourceResponse.class)
  public Object get(Context context, Request req, Response res, Variant variant)
      throws ResourceException
  {
    FeedListResourceResponse response = new FeedListResourceResponse();

    List<FeedSource> sources = feeds;

    for (FeedSource source : sources) {
      FeedListResource resource = new FeedListResource();

      resource.setResourceURI(createChildReference(req, this, source.getFeedKey()).toString());

      resource.setName(source.getFeedName());

      response.addData(resource);
    }

    return response;
  }

}
