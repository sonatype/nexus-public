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

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.ContinuationTokenHelper.ContinuationTokenException;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.AssetsResourceDoc;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.rest.api.AssetXO.fromAsset;
import static org.sonatype.nexus.repository.rest.internal.api.RepositoryItemIDXO.fromString;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_ACCEPTABLE;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.3
 */
@Named
@Singleton
@Path(AssetsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class AssetsResource
    extends ComponentSupport
    implements Resource, AssetsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/assets";

  private final BrowseService browseService;

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final MaintenanceService maintenanceService;

  private final ContinuationTokenHelper continuationTokenHelper;

  @Inject
  public AssetsResource(final BrowseService browseService,
                        final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
                        final AssetEntityAdapter assetEntityAdapter,
                        final MaintenanceService maintenanceService,
                        @Named("asset") final ContinuationTokenHelper continuationTokenHelper)
  {
    this.browseService = checkNotNull(browseService);
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.continuationTokenHelper = checkNotNull(continuationTokenHelper);
  }


  @GET
  public Page<AssetXO> getAssets(@QueryParam("continuationToken") final String continuationToken,
      @QueryParam("repository") final String repositoryId)
  {
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);

    BrowseResult<Asset> assetBrowseResult = browseService.browseAssets(
        repository,
        new QueryOptions(null, "id", "asc", 0, 10, lastIdFromContinuationToken(continuationToken)));

    List<AssetXO> assetXOs = assetBrowseResult.getResults().stream()
        .map(asset -> fromAsset(asset, repository))
        .collect(toList());
    return new Page<>(assetXOs, assetBrowseResult.getTotal() > assetBrowseResult.getResults().size() ?
        continuationTokenHelper.getTokenFromId(getLast(assetBrowseResult.getResults())) : null);
  }

  @Nullable
  private String lastIdFromContinuationToken(final String continuationToken) {
    try {
      return continuationTokenHelper.getIdFromToken(continuationToken);
    }
    catch (ContinuationTokenException e) {
      log.debug(e.getMessage(), e);
      throw new WebApplicationException(NOT_ACCEPTABLE);
    }
  }

  @GET
  @Path("/{id}")
  public AssetXO getAssetById(@PathParam("id") final String id)
  {
    RepositoryItemIDXO repositoryItemIDXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

    Asset asset = getAsset(id, repository, new DetachedEntityId(repositoryItemIDXO.getId()));
    return fromAsset(asset, repository);
  }

  @DELETE
  @Path("/{id}")
  public void deleteAsset(@PathParam("id")
                          final String id)
  {
    RepositoryItemIDXO repositoryItemIDXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

    DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
    Asset asset = getAsset(id, repository, entityId);

    if (repository != null && asset != null) {
      maintenanceService.deleteAsset(repository, asset);
    }
  }

  private Asset getAsset(final String id, final Repository repository, final DetachedEntityId entityId)
  {
    try {
      return ofNullable(browseService
          .getAssetById(assetEntityAdapter.recordIdentity(entityId), repository))
          .orElseThrow(() -> new NotFoundException("Unable to locate asset with id " + id));
    }
    catch (IllegalArgumentException e) {
      log.debug("IllegalArgumentException caught retrieving asset with id {}", entityId, e);
      throw new WebApplicationException(format("Unable to process asset with id %s", entityId), UNPROCESSABLE_ENTITY);
    }
  }
}
