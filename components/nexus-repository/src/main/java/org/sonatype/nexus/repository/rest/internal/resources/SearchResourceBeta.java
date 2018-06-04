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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.rest.SearchResourceExtension;
import org.sonatype.nexus.repository.rest.SearchUtils;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.rest.internal.resources.SearchResource.SEARCH_AND_DOWNLOAD_URI;
import static org.sonatype.nexus.repository.rest.internal.resources.SearchResource.SEARCH_ASSET_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.next, use {@link SearchResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(SearchResourceBeta.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class SearchResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/search";

  private final SearchResource delegate;

  @Inject
  public SearchResourceBeta(final SearchUtils searchUtils,
                            final AssetMapUtils assetMapUtils,
                            final BrowseService browseService,
                            final SearchService searchService,
                            final TokenEncoder tokenEncoder,
                            final ComponentXOFactory componentXOFactory,
                            final Set<SearchResourceExtension> searchResourceExtensions)
  {
    delegate = new SearchResource(searchUtils, assetMapUtils, browseService, searchService, tokenEncoder,
        componentXOFactory, searchResourceExtensions);
  }

  @GET
  public Page<ComponentXO> search(
      @QueryParam("continuationToken") final String continuationToken,
      @Context final UriInfo uriInfo)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, SearchResource.RESOURCE_URI);
    return delegate.search(continuationToken, uriInfo);
  }

  @GET
  @Path(SEARCH_ASSET_URI)
  public Page<AssetXO> searchAssets(
      @QueryParam("continuationToken") final String continuationToken,
      @Context final UriInfo uriInfo)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, SearchResource.RESOURCE_URI);
    return delegate.searchAssets(continuationToken, uriInfo);
  }


  @GET
  @Path(SEARCH_AND_DOWNLOAD_URI)
  public Response searchAndDownloadAssets(@Context final UriInfo uriInfo)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, SearchResource.RESOURCE_URI);
    return delegate.searchAndDownloadAssets(uriInfo);
  }
}
