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
import org.sonatype.nexus.repository.security.RepositorySelector
import org.sonatype.nexus.repository.security.RepositoryViewPermission
import org.sonatype.nexus.repository.security.VariableResolverAdapter
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter
import org.sonatype.nexus.repository.storage.ComponentMaintenance
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.JexlExpressionValidator
import org.sonatype.nexus.selector.VariableSource
import org.sonatype.nexus.validation.Validate

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
  ContentPermissionChecker contentPermissionChecker

  @Inject
  VariableResolverAdapterManager variableResolverAdapterManager

  @Inject
  JexlExpressionValidator jexlExpressionValidator

  @DirectMethod
  @Timed
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
  @Timed
  List<AssetXO> readComponentAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)

    if (!repository.configuration.online) {
      return null
    }

    def componentId = parameters.getFilter('componentId')
    return readRepositoryComponentAssets(repository, componentId)
  }

  /**
   * Find all Assets related to the given component. Note that the Repository passed in is not necessarily the
   * Repository where the component resides (in the case of a group Repository).
   */
  private List<AssetXO> readRepositoryComponentAssets(final Repository repository, final String componentId) {
    checkNotNull(repository)
    checkNotNull(componentId)
    List<Asset> assets
    Component component
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()
      component = storageTx.findComponent(new DetachedEntityId(componentId))
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
          variableResolverAdapter.fromAsset(it))
    }.collect(ASSET_CONVERTER.rcurry(component.name(), repository.name))
  }
  
  private List<Repository> getPreviewRepositories(String repositoryName) {
    RepositorySelector repositorySelector = RepositorySelector.fromSelector(repositoryName)
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

  String parseJexlExpression(String expression) {
    //posted question here, http://www.prjhub.com/#/issues/7476 as why we can't just have orients bulit in escaping for double quotes
    return expression.replaceAll('"', '\'').replaceAll('\\s', ' ')
  }

  @DirectMethod
  @Timed
  PagedResponse<AssetXO> previewAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    String jexlExpression = parameters.getFilter('jexlExpression')

    if (!jexlExpression || !repositoryName) {
      return null
    }

    jexlExpressionValidator.validate(jexlExpression)

    List<Repository> selectedRepositories = getPreviewRepositories(repositoryName)

    if (!selectedRepositories.size()) {
      return null
    }

    def querySuffix = getQuerySuffix(selectedRepositories, parameters)

    StorageTx storageTx = selectedRepositories[0].facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()

      def repositories
      def repositoriesAsString = ''
      if (selectedRepositories.size() == 1 && groupType == selectedRepositories[0].type) {
        repositories = selectedRepositories[0].facet(GroupFacet).leafMembers()
        repositoriesAsString = (repositories*.name).join(',')
      }
      else {
        repositories = selectedRepositories
      }

      def whereClause = 'contentAuth(@this) == true and contentExpression(@this, :jexlExpression, :repositoryName, ' +
          ':repositoriesAsString) == true'
      def queryParams = [repositoryName: repositoryName, jexlExpression: parseJexlExpression(jexlExpression), repositoriesAsString:
          repositoriesAsString]
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause += " AND ${MetadataNodeEntityAdapter.P_NAME} LIKE :nameFilter"
        queryParams['nameFilter'] = "%${filter}%"
      }

      def countAssets = storageTx.countAssets(whereClause, queryParams, repositories, null)
      List<AssetXO> results = storageTx.findAssets(whereClause, queryParams, repositories, querySuffix).
          collect(ASSET_CONVERTER.rcurry(null, null))

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
  @Timed
  PagedResponse<AssetXO> readAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter('repositoryName')
    Repository repository = repositoryManager.get(repositoryName)

    if (!repository.configuration.online) {
      return null
    }

    def querySuffix = getQuerySuffix(ImmutableList.of(repository), parameters)

    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin()

      def repositories
      if (groupType == repository.type) {
        repositories = repository.facet(GroupFacet).leafMembers()
      }
      else {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = 'contentAuth(@this) == true'
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause += " AND ${MetadataNodeEntityAdapter.P_NAME} LIKE :nameFilter"
        queryParams = [
            'nameFilter': "%${filter}%"
        ]
      }

      def countAssets = storageTx.countAssets(whereClause, queryParams, repositories, null)
      List<AssetXO> results = storageTx.findAssets(whereClause, queryParams, repositories, querySuffix).
          collect(ASSET_CONVERTER.rcurry(null, repositoryName))

      return new PagedResponse<AssetXO>(
          countAssets,
          results
      )
    }
    finally {
      storageTx.close()
    }
  }

  private def getQuerySuffix(List<Repository> repositories, StoreLoadParameters parameters) {
    def querySuffix = ''
    def sort = parameters.sort?.get(0)
    if (sort) {
      querySuffix += " ORDER BY ${sort.property} ${sort.direction} "
    }
    if (parameters.start) {
      querySuffix += " SKIP ${parameters.start}"
    }
    if (parameters.limit) {
      querySuffix += " LIMIT ${parameters.limit}"
    }

    return querySuffix
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
    return asset ? ASSET_CONVERTER.call(asset, null, repository.name) as AssetXO : null
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
      if (contentPermissionChecker
          .isPermitted(repository.getName(), format, action, variableSource)) {
        return
      }
    }
    throw new AuthorizationException()
  }
}
