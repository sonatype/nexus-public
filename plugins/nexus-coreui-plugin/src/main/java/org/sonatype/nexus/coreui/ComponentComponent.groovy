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
package org.sonatype.nexus.coreui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.browse.BrowseService
import org.sonatype.nexus.repository.browse.QueryOptions
import org.sonatype.nexus.repository.maintenance.MaintenanceService
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.ContentPermissionChecker
import org.sonatype.nexus.repository.security.RepositorySelector
import org.sonatype.nexus.repository.security.VariableResolverAdapter
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.JexlExpressionValidator
import org.sonatype.nexus.selector.VariableSource
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.hibernate.validator.constraints.NotEmpty

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Component {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Component')
class ComponentComponent
    extends DirectComponentSupport
{

  private static final Closure COMPONENT_CONVERTER = { Component component, String repositoryName ->
    new ComponentXO(
        id: EntityHelper.id(component).value,
        repositoryName: repositoryName,
        group: component.group(),
        name: component.name(),
        version: component.version(),
        format: component.format()
    )
  }

  private static final Closure ASSET_CONVERTER = { Asset asset, String componentName, String repositoryName,
                                                   Map<String, String> repoNamesForBuckets ->
    new AssetXO(
        id: EntityHelper.id(asset).value,
        name: asset.name() ?: componentName,
        format: asset.format(),
        contentType: asset.contentType() ?: 'unknown',
        size: asset.size() ?: 0,
        repositoryName: repositoryName,
        containingRepositoryName: repoNamesForBuckets[asset.bucketId()],
        blobCreated: asset.blobCreated()?.toDate(),
        blobUpdated: asset.blobUpdated()?.toDate(),
        lastDownloaded: asset.lastDownloaded()?.toDate(),
        blobRef: asset.blobRef() ? asset.blobRef().toString() : '',
        componentId: asset.componentId?.value,
        attributes: asset.attributes().backing()
    )
  }

  @Inject
  SecurityHelper securityHelper

  @Inject
  RepositoryManager repositoryManager

  @Inject
  ContentPermissionChecker contentPermissionChecker

  @Inject
  VariableResolverAdapterManager variableResolverAdapterManager

  @Inject
  JexlExpressionValidator jexlExpressionValidator

  @Inject
  BrowseService browseService

  @Inject
  MaintenanceService maintenanceService;

  @DirectMethod
  @Timed
  @ExceptionMetered
  PagedResponse<ComponentXO> read(final StoreLoadParameters parameters) {
    Repository repository = repositoryManager.get(parameters.getFilter('repositoryName'))
    if (!repository.configuration.online) {
      return null
    }
    def result = browseService.browseComponents(
        repository,
        toQueryOptions(parameters))
    return new PagedResponse<ComponentXO>(
        result.total,
        result.results.collect(COMPONENT_CONVERTER.rcurry(repository.name)))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<AssetXO> readComponentAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)
    if (!repository.configuration.online) {
      return null
    }
    def result = browseService.browseComponentAssets(repository, parameters.getFilter('componentId'));
    def repoNamesForBuckets = browseService.getRepositoryBucketNames(repository)
    return result.results.collect(ASSET_CONVERTER.rcurry(parameters.getFilter('componentName'), repository.name, repoNamesForBuckets))
  }

  private List<Repository> getPreviewRepositories(final RepositorySelector repositorySelector) {
    if (!repositorySelector.allRepositories) {
      return ImmutableList.of(repositoryManager.get(repositorySelector.name))
    }

    if (!repositorySelector.allFormats) {
      return repositoryManager.browse().findResults { Repository repository ->
        return repository.format.value == repositorySelector.format ? repository : null
      }
    }

    return repositoryManager.browse().collect()
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  PagedResponse<AssetXO> previewAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    String jexlExpression = parameters.getFilter('jexlExpression')
    if (!jexlExpression || !repositoryName) {
      return null
    }

    RepositorySelector repositorySelector = RepositorySelector.fromSelector(repositoryName)

    jexlExpressionValidator.validate(jexlExpression)
    List<Repository> selectedRepositories = getPreviewRepositories(repositorySelector)
    if (!selectedRepositories.size()) {
      return null
    }

    def result = browseService.previewAssets(
        repositorySelector,
        selectedRepositories,
        jexlExpression,
        toQueryOptions(parameters))
    return new PagedResponse<AssetXO>(
        result.total,
        result.results.collect(ASSET_CONVERTER.rcurry(null, null, [:])) // buckets not needed for asset preview screen
    )
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  PagedResponse<AssetXO> readAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)
    if (!repository.configuration.online) {
      return null
    }
    def result = browseService.browseAssets(repository, toQueryOptions(parameters))
    def repoNamesForBuckets = browseService.getRepositoryBucketNames(repository)
    return new PagedResponse<AssetXO>(
        result.total,
        result.results.collect(ASSET_CONVERTER.rcurry(null, repositoryName, repoNamesForBuckets))
    )
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void deleteComponent(@NotEmpty final String componentId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()

    Component component;
    try {
      storageTx.begin()
      component = storageTx.findComponent(new DetachedEntityId(componentId))
    }
    finally {
      storageTx.close()
    }
    if (component != null) {
      maintenanceService.deleteComponent(repository, component)
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void deleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    String format = repository.format.toString()

    Asset asset;
    try {
      storageTx.begin()
      asset = storageTx.findAsset(new DetachedEntityId(assetId), storageTx.findBucket(repository))
    }
    finally {
      storageTx.close()
    }
    if (asset != null) {
      maintenanceService.deleteAsset(repository, asset)
    }
  }
  
  /**
   * Retrieve a component by its entity id.
   *
   * @return found component or null
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  @Nullable
  ComponentXO readComponent(@NotEmpty String componentId, @NotEmpty String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    Component component;
    List<Asset> assets;
    try {
      storageTx.begin()
      component = storageTx.findComponentInBucket(new DetachedEntityId(componentId), storageTx.findBucket(repository))
      assets = Lists.newArrayList(storageTx.browseAssets(component))
    }
    finally {
      storageTx.close()
    }
    ensurePermissions(repository, assets, BreadActions.READ)
    return component ? COMPONENT_CONVERTER.call(component, repository.name) as ComponentXO : null
  }

  /**
   * Retrieve an asset by its entity id.
   *
   * @return found asset or null
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  @Nullable
  AssetXO readAsset(@NotEmpty String assetId, @NotEmpty String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    Asset asset;
    try {
      storageTx.begin()
      asset = storageTx.findAsset(new DetachedEntityId(assetId), storageTx.findBucket(repository))
    }
    finally {
      storageTx.close()
    }
    ensurePermissions(repository, Collections.singletonList(asset), BreadActions.READ)
    if (asset) {
      def repoNamesForBuckets = browseService.getRepositoryBucketNames(repository)
      return ASSET_CONVERTER.call(asset, null, repository.name, repoNamesForBuckets)
    }
    else {
      return null
    }
  }

  private QueryOptions toQueryOptions(StoreLoadParameters storeLoadParameters) {
    def sort = storeLoadParameters.sort?.get(0)

    return new QueryOptions(
        storeLoadParameters.getFilter('filter'),
        sort?.property,
        sort?.direction,
        storeLoadParameters.start,
        storeLoadParameters.limit)
  }

  /**
   * Ensures that the action is permitted on at least one asset in the collection.
   *
   * @throws AuthorizationException
   */
  private void ensurePermissions(final Repository repository,
                                 final Iterable<Asset> assets,
                                 final String action)
  {
    checkNotNull(repository)
    checkNotNull(assets)
    checkNotNull(action)
    String format = repository.getFormat().getValue()
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format)
    for (Asset asset : assets) {
      VariableSource variableSource = variableResolverAdapter.fromAsset(asset)
      if (contentPermissionChecker.isPermitted(repository.getName(), format, action, variableSource)) {
        return
      }
    }
    throw new AuthorizationException()
  }
}
