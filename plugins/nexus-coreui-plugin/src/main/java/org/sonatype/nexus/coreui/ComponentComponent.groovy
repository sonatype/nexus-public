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

import com.google.common.collect.ImmutableList
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.EntityHelper
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.MissingFacetException
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.security.BreadActions
import org.sonatype.nexus.repository.security.RepositoryViewPermission
import org.sonatype.nexus.repository.storage.*
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.validation.Validate

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

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
        attributes: asset.attributes().backing()
    )
  }

  @Inject
  SecurityHelper securityHelper

  @Inject
  RepositoryManager repositoryManager

  @DirectMethod
  PagedResponse<ComponentXO> read(final StoreLoadParameters parameters) {
    Repository repository = repositoryManager.get(parameters.getFilter('repositoryName'))
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.BROWSE))

    if (!repository.configuration.online) {
      return null
    }

    def sort = parameters.sort?.get(0)
    def querySuffix = ''
    if (sort) {
      if (GroupType.NAME != repository.type.value) {
        // optimization to match component-bucket-group-name-version index when querying on a single repository
        querySuffix += " ORDER BY ${StorageFacet.P_BUCKET} ${sort.direction},${sort.property} ${sort.direction}"
      }
      else {
        querySuffix += " ORDER BY ${sort.property} ${sort.direction}"
      }
      if (sort.property == StorageFacet.P_GROUP) {
        querySuffix += ", ${StorageFacet.P_NAME} ASC,${StorageFacet.P_VERSION} ASC"
      }
      else if (sort.property == StorageFacet.P_NAME) {
        querySuffix += ", ${StorageFacet.P_VERSION} ASC,${StorageFacet.P_GROUP} ASC"
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
      storageTx.begin();

      def repositories
      try {
        repositories = repository.facet(GroupFacet).leafMembers()
        querySuffix = " GROUP BY ${StorageFacet.P_GROUP},${StorageFacet.P_NAME},${StorageFacet.P_VERSION}" + querySuffix
      }
      catch (MissingFacetException e) {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = null
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause = "${StorageFacet.P_NAME} LIKE :nameFilter OR ${StorageFacet.P_GROUP} LIKE :groupFilter OR ${StorageFacet.P_VERSION} LIKE :versionFilter"
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
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.BROWSE))

    if (!repository.configuration.online) {
      return null
    }

    def componentId = parameters.getFilter('componentId')

    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin();

      def repositories
      try {
        repositories = repository.facet(GroupFacet).leafMembers()
      }
      catch (MissingFacetException e) {
        repositories = ImmutableList.of(repository)
      }

      if (repositories.size() == 1) {
        Component component = storageTx.findComponent(new DetachedEntityId(componentId), storageTx.findBucket(repositories[0]))
        if (component == null) {
          log.warn 'Component {} not found', componentId
          return null
        }

        return storageTx.browseAssets(component).collect(ASSET_CONVERTER.rcurry(component.name(), repositoryName))
      }
      else {
        def componentGroup = parameters.getFilter('componentGroup')
        def componentName = parameters.getFilter('componentName')
        def componentVersion = parameters.getFilter('componentVersion')

        if (componentName) {
          def whereClause = "${StorageFacet.P_COMPONENT}.${StorageFacet.P_NAME} = :name"
          def params = ['name': componentName]
          if (componentGroup) {
            whereClause += " AND ${StorageFacet.P_COMPONENT}.${StorageFacet.P_GROUP} = :group"
            params << ['group': componentGroup]
          }
          if (componentVersion) {
            whereClause += " AND ${StorageFacet.P_COMPONENT}.${StorageFacet.P_VERSION} = :version"
            params << ['version': componentVersion]
          }
          def groupBy = " GROUP BY ${StorageFacet.P_NAME}"
          return storageTx.findAssets(whereClause, params, repositories, groupBy).
              collect(ASSET_CONVERTER.rcurry(componentName, repositoryName))
        }
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
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.BROWSE))

    if (!repository.configuration.online) {
      return null
    }

    def sort = parameters.sort?.get(0)
    def querySuffix = ''
    if (sort) {
      if (GroupType.NAME != repository.type.value) {
        // optimization to match asset-bucket-name index when querying on a single repository
        querySuffix += " ORDER BY ${StorageFacet.P_BUCKET} ${sort.direction},${sort.property} ${sort.direction}"
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
      storageTx.begin();

      def repositories
      try {
        repositories = repository.facet(GroupFacet).leafMembers()
        querySuffix = " GROUP BY ${StorageFacet.P_NAME}" + querySuffix
      }
      catch (MissingFacetException e) {
        repositories = ImmutableList.of(repository)
      }

      def whereClause = null
      def queryParams = null
      def filter = parameters.getFilter('filter')
      if (filter) {
        whereClause = "${StorageFacet.P_NAME} LIKE :nameFilter"
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
  void deleteAsset(@NotEmpty String assetId, @NotEmpty String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.DELETE))

    try {
      ComponentMaintenance componentMaintenance = repository.facet(ComponentMaintenance.class)
      componentMaintenance.deleteAsset(new DetachedEntityId(assetId))
    }
    catch (MissingFacetException e) {
      // TODO: Move this direct use of DefaultComponentMaintenance facet to eliminate the missing facet check
      StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
      try {
        storageTx.begin();
        Asset asset = storageTx.findAsset(new DetachedEntityId(assetId), storageTx.findBucket(repository))
        log.info 'Deleting asset: {}', asset
        storageTx.deleteAsset(asset)
        storageTx.commit()
      }
      finally {
        storageTx.close()
      }
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
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.READ))
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin();
      Component component = storageTx.findComponent(new DetachedEntityId(componentId), storageTx.findBucket(repository))
      return component ? COMPONENT_CONVERTER.call(component, repository.name) as ComponentXO : null
    }
    finally {
      storageTx.close()
    }
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
    securityHelper.ensurePermitted(new RepositoryViewPermission(repository, BreadActions.READ))
    StorageTx storageTx = repository.facet(StorageFacet).txSupplier().get()
    try {
      storageTx.begin();
      Asset asset = storageTx.findAsset(new DetachedEntityId(assetId), storageTx.findBucket(repository))
      return asset ? ASSET_CONVERTER.call(asset, null, repository.name) as AssetXO : null
    }
    finally {
      storageTx.close()
    }
  }

}
