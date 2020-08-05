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
package org.sonatype.nexus.coreui.internal.orient

import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.ws.rs.WebApplicationException

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.coreui.AssetXO
import org.sonatype.nexus.coreui.ComponentHelper
import org.sonatype.nexus.coreui.ComponentXO
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.browse.BrowseService
import org.sonatype.nexus.repository.maintenance.MaintenanceService
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.query.PageResult
import org.sonatype.nexus.repository.query.QueryOptions
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
import org.sonatype.nexus.selector.VariableSource

import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import org.apache.shiro.authz.AuthorizationException

import static com.google.common.base.Preconditions.checkNotNull
import static javax.ws.rs.core.Response.Status
import static org.sonatype.nexus.repository.storage.DefaultComponentFinder.DEFAULT_COMPONENT_FINDER_KEY

/**
 * Orient {@link ComponentHelper}.
 *
 * @since 3.26
 */
@Priority(Integer.MAX_VALUE) // make sure this implementation appears above the datastore one in mixed-mode
@Named
@Singleton
class OrientComponentHelper
    extends ComponentSupport
    implements ComponentHelper
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
  RepositoryManager repositoryManager

  @Inject
  ContentPermissionChecker contentPermissionChecker

  @Inject
  VariableResolverAdapterManager variableResolverAdapterManager

  @Inject
  BrowseService browseService

  @Inject
  MaintenanceService maintenanceService

  @Inject
  ComponentStore componentStore

  @Inject
  Map<String, ComponentFinder> componentFinders

  @Inject
  BucketStore bucketStore

  List<AssetXO> readComponentAssets(final Repository repository, final ComponentXO componentXO) {
    ComponentFinder componentFinder = componentFinders.get(componentXO.format)
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY)
    }

    List<Component> components = componentFinder.findMatchingComponents(repository, componentXO.id,
        componentXO.group, componentXO.name, componentXO.version)

    def browseResult = browseService.browseComponentAssets(repository, components.get(0))

    return createAssetXOs(browseResult.results, componentXO.name, repository)
  }

  PageResult<AssetXO> previewAssets(
      final RepositorySelector repositorySelector,
      final List<Repository> selectedRepositories,
      final String jexlExpression,
      final QueryOptions queryOptions) {

    PageResult<Asset> page = browseService.previewAssets(
        repositorySelector,
        selectedRepositories,
        jexlExpression,
        queryOptions)

    return new PageResult<AssetXO>(
        page.total,
        page.results.collect(ASSET_CONVERTER.rcurry(null, null, null)) // buckets not needed for asset preview screen
    )
  }

  ComponentXO readComponent(final Repository repository, final EntityId componentId) {
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    Component component
    List<Asset> assets
    try {
      storageTx.begin()
      component = storageTx.findComponent(componentId)
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

  boolean canDeleteComponent(final Repository repository, final ComponentXO componentXO) {
    List<Component> components = getComponents(repository, componentXO)

    for (Component component : components) {
      if (!maintenanceService.canDeleteComponent(repository, component)) {
        return false
      }
    }

    return true
  }

  Set<String> deleteComponent(final Repository repository, final ComponentXO componentXO) {
    List<Component> components = getComponents(repository, componentXO)

    Set<String> deletedAssets = new HashSet<>()
    for (Component component : components) {
      deletedAssets.addAll(maintenanceService.deleteComponent(repository, component))
    }
    return deletedAssets
  }

  private List<Component> getComponents(final Repository repository, final ComponentXO componentXO) {
    ComponentFinder componentFinder = componentFinders.get(componentXO.format)
    if (null == componentFinder) {
      componentFinder = componentFinders.get(DEFAULT_COMPONENT_FINDER_KEY)
    }

    return componentFinder.findMatchingComponents(repository, componentXO.id, componentXO.group,
        componentXO.name, componentXO.version)
  }

  AssetXO readAsset(final Repository repository, final EntityId assetId) {
    Asset asset = browseService.getAssetById(assetId, repository)

    if (asset == null) {
      throw new WebApplicationException(Status.NOT_FOUND)
    }

    String permittedRepositoryName = ensurePermissions(
        repositoryManager.get(
            bucketStore.getById(asset.bucketId()).repositoryName), Collections.singletonList(asset), BreadActions.BROWSE)

    if (asset) {
      return ASSET_CONVERTER.call(asset, null, repository.name, permittedRepositoryName)
    }
    else {
      return null
    }
  }

  boolean canDeleteAsset(final Repository repository, final EntityId assetId) {
    Asset asset = getAsset(repository, assetId)

    if (asset != null) {
      return maintenanceService.canDeleteAsset(repository, asset)
    }

    return false
  }

  Set<String> deleteAsset(final Repository repository, final EntityId assetId) {
    Asset asset = getAsset(repository, assetId)

    if (asset != null) {
      return maintenanceService.deleteAsset(repository, asset)
    }

    return Collections.emptySet()
  }

  private Asset getAsset(final Repository repository, final EntityId assetId) {
    Asset asset = null
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()

    try {
      storageTx.begin()
      asset = storageTx.findAsset(assetId, storageTx.findBucket(repository))
    }
    finally {
      storageTx.close()
    }

    return asset
  }

  boolean canDeleteFolder(final Repository repository, final String path) {
    return maintenanceService.canDeleteFolder(repository, path)
  }

  void deleteFolder(final Repository repository, final String path) {
    maintenanceService.deleteFolder(repository, path)
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
