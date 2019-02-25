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
import javax.ws.rs.WebApplicationException

import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.EntityHelper
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
import org.sonatype.nexus.repository.storage.BucketStore
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentFinder
import org.sonatype.nexus.repository.storage.ComponentStore
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.SelectorFactory
import org.sonatype.nexus.selector.VariableSource
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty

import static com.google.common.base.Preconditions.checkNotNull
import static javax.ws.rs.core.Response.Status
import static org.sonatype.nexus.repository.storage.DefaultComponentFinder.DEFAULT_COMPONENT_FINDER_KEY

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

  static final ComponentXO COMPONENT_CONVERTER(Component component, String repositoryName) {
    return new ComponentXO(
        id: EntityHelper.id(component).value,
        repositoryName: repositoryName,
        group: component.group(),
        name: component.name(),
        version: component.version(),
        format: component.format()
    )
  }

  private static final Closure ASSET_CONVERTER = {
    Asset asset,
    String componentName,
    String repositoryName,
    String privilegedRepositoryName ->
      new AssetXO(
          id: EntityHelper.id(asset).value,
          name: asset.name() ?: componentName,
          format: asset.format(),
          contentType: asset.contentType() ?: 'unknown',
          size: asset.size() ?: 0,
          repositoryName: repositoryName,
          containingRepositoryName: privilegedRepositoryName,
          blobCreated: asset.blobCreated()?.toDate(),
          blobUpdated: asset.blobUpdated()?.toDate(),
          lastDownloaded: asset.lastDownloaded()?.toDate(),
          blobRef: asset.blobRef() ? asset.blobRef().toString() : '',
          componentId: asset.componentId() ? asset.componentId().value : '',
          attributes: asset.attributes().backing(),
          createdBy: asset.createdBy(),
          createdByIp: asset.createdByIp()
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
  SelectorFactory selectorFactory

  @Inject
  BrowseService browseService

  @Inject
  MaintenanceService maintenanceService

  @Inject
  ComponentStore componentStore

  @Inject
  ObjectMapper objectMapper

  @Inject
  Map<String, ComponentFinder> componentFinders

  @Inject
  BucketStore bucketStore

  @DirectMethod
  @Timed
  @ExceptionMetered
  List<AssetXO> readComponentAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)
    if (!repository.configuration.online) {
      return null
    }

    ComponentXO componentXO = objectMapper.readValue(parameters.getFilter('componentModel'), ComponentXO.class)

    ComponentFinder componentFinder = componentFinders.get(componentXO.format)
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY)
    }

    List<Component> components = componentFinder.findMatchingComponents(repository, componentXO.id,
        componentXO.group, componentXO.name, componentXO.version)

    def browseResult = browseService.browseComponentAssets(repository, components.get(0))

    return createAssetXOs(browseResult.results, componentXO.name, repository)
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
  @RequiresPermissions('nexus:selectors:*')
  PagedResponse<AssetXO> previewAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    String expression = parameters.getFilter('expression')
    String type = parameters.getFilter('type')
    if (!expression || !type || !repositoryName) {
      return null
    }

    selectorFactory.validateSelector(type, expression)

    RepositorySelector repositorySelector = RepositorySelector.fromSelector(repositoryName)
    List<Repository> selectedRepositories = getPreviewRepositories(repositorySelector)
    if (!selectedRepositories.size()) {
      return null
    }

    def result = browseService.previewAssets(
        repositorySelector,
        selectedRepositories,
        expression,
        toQueryOptions(parameters))
    return new PagedResponse<AssetXO>(
        result.total,
        result.results.collect(ASSET_CONVERTER.rcurry(null, null, null)) // buckets not needed for asset preview screen
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

    return new PagedResponse<AssetXO>(
        result.total,
        createAssetXOs(result.results, null, repository))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteComponent(@NotEmpty final String componentModelString)
  {
    ComponentXO componentXO = objectMapper.readValue(componentModelString, ComponentXO.class)
    Repository repository = repositoryManager.get(componentXO.repositoryName)
    List<Component> components = getComponents(componentXO, repository)

    for (Component component : components) {
      if (!maintenanceService.canDeleteComponent(repository, component)) {
        return false
      }
    }

    return true
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  Set<String> deleteComponent(@NotEmpty final String componentModelString)
  {
    ComponentXO componentXO = objectMapper.readValue(componentModelString, ComponentXO.class)
    Repository repository = repositoryManager.get(componentXO.repositoryName)
    List<Component> components = getComponents(componentXO, repository)

    Set<String> deletedAssets = new HashSet<>()
    for (Component component : components) {
      deletedAssets.addAll(maintenanceService.deleteComponent(repository, component))
    }
    return deletedAssets
  }

  private List<Component> getComponents(final ComponentXO componentXO, Repository repository) {
    ComponentFinder componentFinder = componentFinders.get(componentXO.format)
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY)
    }

    return componentFinder.findMatchingComponents(repository, componentXO.id, componentXO.group,
        componentXO.name, componentXO.version)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName)
  {
    Repository repository = repositoryManager.get(repositoryName)
    Asset asset = getAsset(assetId, repository)

    if (asset != null) {
      return maintenanceService.canDeleteAsset(repository, asset)
    }

    return false
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  Set<String> deleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    Asset asset = getAsset(assetId, repository)

    if (asset != null) {
      return maintenanceService.deleteAsset(repository, asset)
    }

    return Collections.emptySet()
  }

  private Asset getAsset(final String assetId, final Repository repository) {
    Asset asset = null
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()

    try {
      storageTx.begin()
      asset = storageTx.findAsset(new DetachedEntityId(assetId), storageTx.findBucket(repository))
    }
    finally {
      storageTx.close()
    }

    return asset
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
    Component component
    List<Asset> assets
    try {
      storageTx.begin()
      component = storageTx.findComponent(new DetachedEntityId(componentId))
      if (component == null) {
        throw new WebApplicationException(Status.NOT_FOUND)
      }

      Iterable<Asset> browsedAssets = storageTx.browseAssets(component)
      if (browsedAssets == null || Iterables.isEmpty(browsedAssets)) {
        throw new WebApplicationException(Status.NOT_FOUND)
      }

      assets = Lists.newArrayList(browsedAssets)
    }
    finally {
      storageTx.close()
    }
    ensurePermissions(repository, assets, BreadActions.BROWSE)
    return COMPONENT_CONVERTER(component, repository.name)
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

    Asset asset = browseService.getAssetById(new DetachedEntityId(assetId), repository)

    if (asset == null) {
      throw new WebApplicationException(Status.NOT_FOUND)
    }

    String permittedRepositoryName = ensurePermissions(
        repositoryManager.get(
            bucketStore.getById(asset.bucketId()).repositoryName), Collections.singletonList(asset), BreadActions.BROWSE)

    if (asset) {
      return ASSET_CONVERTER.call(asset, null, repositoryName, permittedRepositoryName)
    }
    else {
      return null
    }
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName)
  {
    Repository repository = repositoryManager.get(repositoryName)
    return maintenanceService.canDeleteFolder(repository, path)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void deleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    maintenanceService.deleteFolder(repository, path)
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
   * Ensures that the action is permitted on at least one asset in the collection via any one
   * of the passed in repositories
   *
   * @param repository
   * @param assets
   * @param action
   * @return the repository that the user has privilege to see the asset(s) through
   * @throws AuthorizationException
   */
  private String ensurePermissions(final Repository repository,
                                   final Iterable<Asset> assets,
                                   final String action)
  {
    checkNotNull(repository)
    checkNotNull(assets)
    checkNotNull(action)

    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(repository.format.value)

    List<String> repositoryNames = repositoryManager.findContainingGroups(repository.name)
    repositoryNames.add(0, repository.name)

    for (Asset asset : assets) {
      VariableSource variableSource = variableResolverAdapter.fromAsset(asset)
      String repositoryName = getPrivilegedRepositoryName(repositoryNames, repository.format.value, action, variableSource)
      if (repositoryName) {
        return repositoryName
      }
    }

    throw new AuthorizationException()
  }

  private List<AssetXO> createAssetXOs(List<Asset> assets,
                                       String componentName,
                                       Repository repository) {
    List<AssetXO> assetXOs = new ArrayList<>()
    for (Asset asset : assets) {
      def privilegedRepositoryName = getPrivilegedRepositoryName(repository, asset)
      assetXOs.add(ASSET_CONVERTER.call(asset, componentName, repository.name, privilegedRepositoryName))
    }
    return assetXOs
  }

  private String getPrivilegedRepositoryName(Repository repository, Asset asset) {
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(repository.format.value)
    VariableSource variableSource = variableResolverAdapter.fromAsset(asset)
    String assetRepositoryName = bucketStore.getById(asset.bucketId()).repositoryName
    List<String> repositoryNames = repositoryManager.findContainingGroups(assetRepositoryName)
    repositoryNames.add(0, assetRepositoryName)
    return getPrivilegedRepositoryName(repositoryNames, repository.format.value, BreadActions.BROWSE, variableSource)
  }

  private String getPrivilegedRepositoryName(List<String> repositoryNames, String format, String action, VariableSource variableSource) {
    for (String repositoryName : repositoryNames) {
      if (contentPermissionChecker.isPermitted(repositoryName, format, action, variableSource)) {
        return repositoryName
      }
    }

    return null
  }
}
