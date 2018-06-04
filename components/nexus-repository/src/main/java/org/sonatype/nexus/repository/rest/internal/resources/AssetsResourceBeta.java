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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.next, use {@link AssetsResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(AssetsResourceBeta.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class AssetsResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/assets";

  private final AssetsResource delegate;

  @Inject
  public AssetsResourceBeta(final BrowseService browseService,
                            final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                            final AssetEntityAdapter assetEntityAdapter,
                            final MaintenanceService maintenanceService,
                            @Named("asset") final ContinuationTokenHelper continuationTokenHelper)
  {
    delegate = new AssetsResource(browseService, repositoryManagerRESTAdapter, assetEntityAdapter, maintenanceService,
        continuationTokenHelper);
  }

  @GET
  public Page<AssetXO> getAssets(@QueryParam("continuationToken") final String continuationToken,
                                 @QueryParam("repository") final String repositoryId)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, AssetsResource.RESOURCE_URI);
    return delegate.getAssets(continuationToken, repositoryId);
  }

  @GET
  @Path("/{id}")
  public AssetXO getAssetById(@PathParam("id") final String id)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, AssetsResource.RESOURCE_URI);
    return delegate.getAssetById(id);
  }

  @DELETE
  @Path("/{id}")
  public void deleteAsset(@PathParam("id")
                          final String id)
  {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, AssetsResource.RESOURCE_URI);
    delegate.deleteAsset(id);
  }
}
