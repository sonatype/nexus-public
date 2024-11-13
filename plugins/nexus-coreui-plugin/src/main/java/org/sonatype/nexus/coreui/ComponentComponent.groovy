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
import javax.validation.constraints.NotEmpty

import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.query.PageResult
import org.sonatype.nexus.repository.query.QueryOptions
import org.sonatype.nexus.repository.security.ContentPermissionChecker
import org.sonatype.nexus.repository.security.RepositorySelector
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.SelectorFactory
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableList
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

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
  ObjectMapper objectMapper

  @Inject
  ComponentHelper componentHelper

  @Inject
  Map<String, AssetAttributeTransformer> formatTransformations;

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

    return componentHelper.readComponentAssets(repository, componentXO)
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

    PageResult<AssetXO> result = componentHelper.previewAssets(
        repositorySelector,
        selectedRepositories,
        expression,
        toQueryOptions(parameters))

    return new PagedResponse<AssetXO>(result.total, result.results)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteComponent(@NotEmpty final String componentModelString) {
    ComponentXO componentXO = objectMapper.readValue(componentModelString, ComponentXO.class)
    Repository repository = repositoryManager.get(componentXO.repositoryName)
    return componentHelper.canDeleteComponent(repository, componentXO)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  Set<String> deleteComponent(@NotEmpty final String componentModelString) {
    ComponentXO componentXO = objectMapper.readValue(componentModelString, ComponentXO.class)
    Repository repository = repositoryManager.get(componentXO.repositoryName)
    return componentHelper.deleteComponent(repository, componentXO)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    return componentHelper.canDeleteAsset(repository, new DetachedEntityId(assetId))
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  Set<String> deleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    // GSON used by DirectJNgine can exclude some of the Guava collection types
    return new HashSet<>(componentHelper.deleteAsset(repository, new DetachedEntityId(assetId)))
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
    return componentHelper.readComponent(repository, new DetachedEntityId(componentId))
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
    AssetXO assetXO = componentHelper.readAsset(repository, new DetachedEntityId(assetId))
    Optional.ofNullable(formatTransformations.get(assetXO.format))
        .ifPresent {transformation -> transformation.transform(assetXO) }
    return assetXO;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  boolean canDeleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    return componentHelper.canDeleteFolder(repository, path)
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  void deleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName)
    componentHelper.deleteFolder(repository, path)
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
}
