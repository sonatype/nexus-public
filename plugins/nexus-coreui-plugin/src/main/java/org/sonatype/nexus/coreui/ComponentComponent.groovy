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
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.IllegalOperationException
import org.sonatype.nexus.repository.MissingFacetException
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.ContentPermissionChecker
import org.sonatype.nexus.repository.security.RepositoryViewPermission
import org.sonatype.nexus.repository.security.VariableResolverAdapter
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.AssetEntityAdapter
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter
import org.sonatype.nexus.repository.storage.ComponentMaintenance
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorConfigurationStore
import org.sonatype.nexus.selector.VariableSource
import org.sonatype.nexus.validation.Validate

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

  private static final Closure ASSET_CONVERTER = { Asset asset, String componentName, String repositoryName ->
    new AssetXO(
        id: EntityHelper.id(asset).value,
        name: asset.name() ?: componentName,
        format: asset.format(),
        contentType: asset.contentType() ?: 'unknown',
        size: asset.size() ?: 0,
        repositoryName: repositoryName,
        lastUpdated: asset.lastUpdated().toDate(),
        lastAccessed: asset.lastAccessed()?.toDate(),
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
  GroupType groupType

  @Inject
  SelectorConfigurationStore selectorConfigurationStore

  @Inject
  ContentPermissionChecker contentPermissionChecker

  @Inject
  VariableResolverAdapterManager variableResolverAdapterManager

  @DirectMethod
  PagedResponse<ComponentXO> read(final StoreLoadParameters parameters) {
    Repository repository = repositoryManager.get(parameters.getFilter('repositoryName'))

    if (!repository.configuration.online) {
      return null
    }

    def sort = parameters.sort?.get(0)
    def querySuffix = ''
    if (sort) {
      if (GroupType.NAME != repository.type.value) {
        // optimization to match component-bucket-group-name-version index when querying on a single repository
        querySuffix += " ORDER BY ${MetadataNodeEntityAdapter.P_BUCKET} ${sort.direction},${sort.property} ${sort.direction}"
      }
      else {
        querySuffix += " ORDER BY ${sort.property} ${sort.direction}"
      }
      if (sort.property == ComponentEntityAdapter.P_GROUP) {
        querySuffix += ", ${MetadataNodeEntityAdapter.P_NAME} ASC,${ComponentEntityAdapter.P_VERSION} ASC"
      }
      else if (sort.property == MetadataNodeEntityAdapter.P_NAME) {
        querySuffix += ", ${ComponentEntityAdapter.P_VERSION} ASC,${ComponentEntityAdapter.P_GROUP} ASC"
      }
    }
    if (parameters.start) {
      querySuffix += " SKIP ${parameters.start}"
    }
    if (parameters.limit) {
      querySuffix += " LIMIT ${parameters.limit}"
    }

    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()

      def repositories
      if (groupType == repository.type) {
        repositories = repository.facet(GroupFacet).leafMembers()
        querySuffix = " GROUP BY ${ComponentEntityAdapter.P_GROUP},${MetadataNodeEntityAdapter.P_NAME},${ComponentEntityAdapter.P_VERSION}" +
            querySuffix
      }
      else {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = "contentAuth(@this) == true"
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause += " AND ${MetadataNodeEntityAdapter.P_NAME} LIKE :nameFilter OR ${ComponentEntityAdapter.P_GROUP} LIKE :groupFilter OR ${ComponentEntityAdapter.P_VERSION} LIKE :versionFilter"
        queryParams = [
            'nameFilter'   : "%${filter}%",
            'groupFilter'  : "%${filter}%",
            'versionFilter': "%${filter}%"
        ]
      }

      def countComponents = storageTx.countComponents(whereClause, queryParams, repositories, null)
      List<ComponentXO> results = storageTx.findComponents(whereClause, queryParams, repositories, querySuffix)
          .collect(COMPONENT_CONVERTER.rcurry(repository.name))

      return new PagedResponse<ComponentXO>(
          countComponents,
          results
      )
    }
    finally {
      storageTx.close()
    }
  }

  @DirectMethod
  List<AssetXO> readComponentAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)

    if (!repository.configuration.online) {
      return null
    }

    def componentId = parameters.getFilter('componentId')

    def repositories
    if (groupType == repository.type) {
      repositories = repository.facet(GroupFacet).leafMembers()
    }
    else {
      repositories = ImmutableList.of(repository)
    }

    if (repositories.size() == 1) {
      return readRepositoryComponentAssets(repository, componentId)
    }
    return readRepositoryComponentAssets(repository, repositories, parameters)
  }

  private List<AssetXO> readRepositoryComponentAssets(final Repository repository, final String componentId) {
    checkNotNull(repository)
    checkNotNull(componentId)
    List<Asset> assets
    Component component
    List<SelectorConfiguration> selectorConfigurations = selectorConfigurationStore.browse()
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()
      component = storageTx.findComponent(new DetachedEntityId(componentId), storageTx.findBucket(repository))
      if (component == null) {
        log.warn 'Component {} not found', componentId
        return null
      }
      assets = Lists.newArrayList(storageTx.browseAssets(component))
    }
    finally {
      storageTx.close()
    }
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(component.format())
    return assets.findAll {
      contentPermissionChecker.isPermitted(
          repository.name,
          it.format(),
          BreadActions.BROWSE,
          selectorConfigurations,
          variableResolverAdapter.fromAsset(it))
    }.collect(ASSET_CONVERTER.rcurry(component.name(), repository.name))
  }

  private List<AssetXO> readRepositoryComponentAssets(final Repository repository,
                                                      final List<Repository> repositories,
                                                      final StoreLoadParameters parameters)
  {
    checkNotNull(repository)
    checkNotNull(repositories)
    checkNotNull(parameters)
    def componentGroup = parameters.getFilter('componentGroup')
    def componentName = parameters.getFilter('componentName')
    def componentVersion = parameters.getFilter('componentVersion')
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()
      if (componentName) {
        def whereClause = "contentAuth(@this) == true"
        whereClause += " AND ${AssetEntityAdapter.P_COMPONENT}.${MetadataNodeEntityAdapter.P_NAME} = :name"
        def params = ['name': componentName]
        if (componentGroup) {
          whereClause += " AND ${AssetEntityAdapter.P_COMPONENT}.${ComponentEntityAdapter.P_GROUP} = :group"
          params << ['group': componentGroup]
        }
        if (componentVersion) {
          whereClause += " AND ${AssetEntityAdapter.P_COMPONENT}.${ComponentEntityAdapter.P_VERSION} = :version"
          params << ['version': componentVersion]
        }
        def groupBy = " GROUP BY ${MetadataNodeEntityAdapter.P_NAME}"
        return storageTx.findAssets(whereClause, params, repositories, groupBy).
            collect(ASSET_CONVERTER.rcurry(componentName, repository.name))
      }
      return null
    }
    finally {
      storageTx.close()
    }
  }

  @DirectMethod
  PagedResponse<AssetXO> readAssets(final StoreLoadParameters parameters) {
    Repository repository = repositoryManager.get(parameters.getFilter('repositoryName'))

    if (!repository.configuration.online) {
      return null
    }

    def sort = parameters.sort?.get(0)
    def querySuffix = ''
    if (sort) {
      if (GroupType.NAME != repository.type.value) {
        // optimization to match asset-bucket-name index when querying on a single repository
        querySuffix += " ORDER BY ${MetadataNodeEntityAdapter.P_BUCKET} ${sort.direction},${sort.property} ${sort.direction}"
      }
      else {
        querySuffix += " ORDER BY ${sort.property} ${sort.direction}"
      }
    }
    if (parameters.start) {
      querySuffix += " SKIP ${parameters.start}"
    }
    if (parameters.limit) {
      querySuffix += " LIMIT ${parameters.limit}"
    }

    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()

      def repositories
      if (groupType == repository.type) {
        repositories = repository.facet(GroupFacet).leafMembers()
        querySuffix = " GROUP BY ${MetadataNodeEntityAdapter.P_NAME}" + querySuffix
      }
      else {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = "contentAuth(@this) == true"
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause += " AND ${MetadataNodeEntityAdapter.P_NAME} LIKE :nameFilter"
        queryParams = [
            'nameFilter': "%${filter}%"
        ]
      }

      def countAssets = storageTx.countAssets(whereClause, queryParams, repositories, null)
      List<AssetXO> results = storageTx.findAssets(whereClause, queryParams, repositories, querySuffix)
          .collect(ASSET_CONVERTER.rcurry(null, repository.name))

      return new PagedResponse<AssetXO>(
          countAssets,
          results
      )
    }
    finally {
      storageTx.close()
    }
  }

  @DirectMethod
  @RequiresAuthentication
  @Validate
  void deleteComponent(@NotEmpty final String componentId, @NotEmpty final String repositoryName) {
    deleteEntity(componentId, repositoryName, { ComponentMaintenance facet, EntityId entityId ->
      facet.deleteComponent(entityId)
    })
  }

  @DirectMethod
  @RequiresAuthentication
  @Validate
  void deleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    deleteEntity(assetId, repositoryName, { ComponentMaintenance facet, EntityId entityId ->
      facet.deleteAsset(entityId)
    })
  }

  void deleteEntity(final String entityId, final String repositoryName, final Closure action) {
    Repository repository = repositoryManager.get(repositoryName)
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.DELETE))

    try {
      ComponentMaintenance componentMaintenanceFacet = repository.facet(ComponentMaintenance.class)
      action.call(componentMaintenanceFacet, new DetachedEntityId(entityId))
    }
    catch (MissingFacetException e) {
      throw new IllegalOperationException(
          "Deleting from repository '$repositoryName' of type '$repository.type' is not supported"
      )
    }
  }

  /**
   * Retrieve a component by its entity id.
   *
   * @return found component or null
   */
  @DirectMethod
  @Validate
  @Nullable
  ComponentXO readComponent(@NotEmpty String componentId, @NotEmpty String repositoryName) {
    List<SelectorConfiguration> selectorConfigurations = selectorConfigurationStore.browse()
    Repository repository = repositoryManager.get(repositoryName)
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    Component component;
    List<Asset> assets;
    try {
      storageTx.begin()
      component = storageTx.findComponent(new DetachedEntityId(componentId), storageTx.findBucket(repository))
      assets = Lists.newArrayList(storageTx.browseAssets(component))
    }
    finally {
      storageTx.close()
    }
    ensurePermissions(repository, selectorConfigurations, assets, BreadActions.READ)
    return component ? COMPONENT_CONVERTER.call(component, repository.name) as ComponentXO : null
  }

  /**
   * Retrieve an asset by its entity id.
   *
   * @return found asset or null
   */
  @DirectMethod
  @Validate
  @Nullable
  AssetXO readAsset(@NotEmpty String assetId, @NotEmpty String repositoryName) {
    List<SelectorConfiguration> selectorConfigurations = selectorConfigurationStore.browse()
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
    ensurePermissions(repository, selectorConfigurations, Collections.singletonList(asset), BreadActions.READ)
    return asset ? ASSET_CONVERTER.call(asset, null, repository.name) as AssetXO : null
  }

  /**
   * Ensures that the action is permitted on at least one asset in the collection.
   *
   * @throws AuthorizationException
   */
  private void ensurePermissions(final Repository repository,
                                 final Collection<SelectorConfiguration> selectorConfigurations,
                                 final Iterable<Asset> assets,
                                 final String action)
  {
    checkNotNull(repository)
    checkNotNull(selectorConfigurations)
    checkNotNull(assets)
    checkNotNull(action)
    String format = repository.getFormat().getValue()
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format)
    for (Asset asset : assets) {
      VariableSource variableSource = variableResolverAdapter.fromAsset(asset)
      if (contentPermissionChecker
          .isPermitted(repository.getName(), format, action, selectorConfigurations,
          variableSource)) {
        return
      }
    }
    throw new AuthorizationException()
  }
}
