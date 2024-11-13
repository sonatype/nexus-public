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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.util.List;
import java.util.Map;
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

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.rest.internal.resources.doc.AssetsResourceDoc;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.repository.content.rest.AssetXOBuilder.fromAsset;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO.fromString;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.27
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
@Path(AssetsResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class AssetsResource
    extends AssetsResourceSupport
    implements Resource, AssetsResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/assets";

  private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  private final MaintenanceService maintenanceService;

  private final Map<String, AssetXODescriptor> assetDescriptors;

  @Inject
  public AssetsResource(
      final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter,
      final MaintenanceService maintenanceService,
      final ContentAuthHelper contentAuthHelper,
      final Map<String, AssetXODescriptor> assetDescriptors)
  {
    super(contentAuthHelper);
    this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
    this.maintenanceService = checkNotNull(maintenanceService);
    this.assetDescriptors = assetDescriptors;
  }

  @GET
  @Override
  public Page<AssetXO> getAssets(
      @Nullable @QueryParam("continuationToken") final String continuationToken,
      @QueryParam("repository") final String repositoryId)
  {
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryId);
    List<FluentAsset> assets = browse(repository,continuationToken);
    return new Page<>(toAssetXOs(repository, assets, this.assetDescriptors), nextContinuationToken(assets));
  }

  @GET
  @Path("/{id}")
  @Override
  public AssetXO getAssetById(@PathParam("id") final String id) {
    RepositoryItemIDXO repositoryItemIDXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());
    Asset asset = getAsset(id, repository, new DetachedEntityId(repositoryItemIDXO.getId()));
    return fromAsset(asset, repository, this.assetDescriptors);
  }

  @DELETE
  @Path("/{id}")
  @Override
  public void deleteAsset(@PathParam("id") final String id) {
    RepositoryItemIDXO repositoryItemIDXO = fromString(id);
    Repository repository = repositoryManagerRESTAdapter.getRepository(repositoryItemIDXO.getRepositoryId());

    DetachedEntityId entityId = new DetachedEntityId(repositoryItemIDXO.getId());
    Asset asset = getAsset(id, repository, entityId);
    maintenanceService.deleteAsset(repository, asset);
  }

  private Asset getAsset(final String id, final Repository repository, final DetachedEntityId entityId)
  {
    try {
      return repository.facet(ContentFacet.class).assets()
          .find(entityId)
          .filter(assetPermitted(repository.getFormat().getValue(), repository.getName()))
          .orElseThrow(() -> new NotFoundException("Unable to locate asset with id " + id));
    }
    catch (IllegalArgumentException e) {
      log.debug("IllegalArgumentException caught retrieving asset with id {}", entityId, e);
      throw new WebApplicationException(format("Unable to process asset with id %s", entityId), UNPROCESSABLE_ENTITY);
    }
  }

  private static List<AssetXO> toAssetXOs(final Repository repository, final List<FluentAsset> assets,
                                          final Map<String, AssetXODescriptor> assetDescriptors) {
    return assets.stream()
        .map(asset -> fromAsset(asset, repository, assetDescriptors))
        .collect(toList());
  }

  private static String nextContinuationToken(final List<FluentAsset> assets) {
    int size = assets.size();
    return size < PAGE_SIZE_LIMIT ? null : toExternalId(internalAssetId(assets.get(size - 1))).getValue();
  }
}
