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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.List;

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
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.browse.api.AssetXO;
import org.sonatype.nexus.repository.browse.internal.api.AssetXOID;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getLast;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.repository.browse.internal.api.AssetXOID.fromString;

/**
 * @since 3.3
 */
@Named
@Singleton
@Path(AssetsResource.RESOURCE_URI)
@Api(value = "assets", description = "Operations to get and delete assets")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class AssetsResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/rest/v1/assets";

  private final BrowseService browseService;

  private final RepositoryManager repositoryManager;

  private final AssetEntityAdapter assetEntityAdapter;

  private final MaintenanceService maintenanceService;

  @Inject
  public AssetsResource(final BrowseService browseService,
                        final RepositoryManager repositoryManager,
                        final AssetEntityAdapter assetEntityAdapter,
                        final MaintenanceService maintenanceService)
  {
    this.browseService = checkNotNull(browseService);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
  }


  @GET
  @ApiOperation("List all assets")
  public Page<AssetXO> getAssets(
      @ApiParam(value = "A token returned by a prior request. If present, the next page of results are returned")
      @QueryParam("continuationToken")
      final String continuationToken,

      @ApiParam(value = "ID of the repository from which you would like to retrieve assets.", required = true)
      @QueryParam("repositoryId")
      final String repositoryId)
  {
    Repository repository = getRepository(repositoryId);

    final String lastId;
    if (continuationToken != null) {
      lastId = assetEntityAdapter.recordIdentity(new DetachedEntityId(continuationToken)).toString();
    }
    else {
      lastId = null;
    }

    BrowseResult<Asset> assetBrowseResult = browseService.browseAssets(
        repository,
        new QueryOptions(null, "id", "asc", 0, 10, lastId));

    List<AssetXO> assetXOs = assetBrowseResult.getResults().stream().map(asset -> fromAsset(asset, repository))
        .collect(toList());

    return new Page<>(assetXOs, assetBrowseResult.getTotal() > assetBrowseResult.getResults().size() ?
        id(getLast(assetBrowseResult.getResults())).getValue() : null);
  }

  @VisibleForTesting
  AssetXO fromAsset(Asset asset, Repository repository) {

    String internalId = id(asset).getValue();

    AssetXO assetXO = new AssetXO();
    assetXO.setCoordinates(asset.name());
    assetXO.setDownloadUrl(repository.getUrl() + "/" + asset.name());
    assetXO.setId(new AssetXOID(repository.getName(), internalId).getValue());

    return assetXO;
  }

  private Repository getRepository(String repositoryId) {
    if (repositoryId == null) {
      throw new WebApplicationException("repositoryId is required.", 422);
    }
    return ofNullable(repositoryManager.get(repositoryId))
        .orElseThrow(() -> new NotFoundException("Unable to locate repository with id " + repositoryId));
  }

  @GET
  @Path("/{id}")
  @ApiOperation("Get a single asset")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Asset not found")
  })
  public AssetXO getAssetById(@ApiParam(value = "Id of the asset to get")
                              @PathParam("id")
                              final String id)
  {
    AssetXOID assetXOID = getAssetXOID(id);
    Repository repository = getRepository(assetXOID.getRepositoryId());

    Asset asset = getAsset(id, repository, new DetachedEntityId(assetXOID.getId()));
    return fromAsset(asset, repository);
  }

  private AssetXOID getAssetXOID(String id) {
    try {
      return fromString(id);
    }
    catch (IllegalArgumentException e) {
      log.debug("Unable to parse id: {}, returning 404.", id, e);
      throw new NotFoundException("Unable to locate asset with id " + id);
    }
  }

  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Delete a single asset")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Asset not found")
  })
  public void deleteAsset(@ApiParam(value = "Id of the asset to delete")
                          @PathParam("id")
                          final String id)
  {
    AssetXOID assetXOID = getAssetXOID(id);
    Repository repository = getRepository(assetXOID.getRepositoryId());

    DetachedEntityId entityId = new DetachedEntityId(assetXOID.getId());
    Asset asset = getAsset(id, repository, entityId);

    maintenanceService.deleteAsset(repository, asset);
  }

  private Asset getAsset(final String id, final Repository repository, final DetachedEntityId entityId)
  {
    return ofNullable(browseService
        .getAssetById(assetEntityAdapter.recordIdentity(entityId), repository))
        .orElseThrow(() -> new NotFoundException("Unable to locate asset with id " + id));
  }
}
