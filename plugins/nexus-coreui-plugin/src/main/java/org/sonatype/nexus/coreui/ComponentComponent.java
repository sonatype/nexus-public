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
package org.sonatype.nexus.coreui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotEmpty;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;

/**
 * Component {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "coreui_Component")
public class ComponentComponent
    extends DirectComponentSupport
{
  private final RepositoryManager repositoryManager;

  private final SelectorFactory selectorFactory;

  private final JsonMapper jsonMapper;

  private final ComponentHelper componentHelper;

  private final Map<String, AssetAttributeTransformer> formatTransformations;

  @Inject
  public ComponentComponent(
      final RepositoryManager repositoryManager,
      final SelectorFactory selectorFactory,
      final JsonMapper jsonMapper,
      final ComponentHelper componentHelper,
      final Map<String, AssetAttributeTransformer> formatTransformations)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.selectorFactory = checkNotNull(selectorFactory);
    this.jsonMapper = checkNotNull(jsonMapper);
    this.componentHelper = checkNotNull(componentHelper);
    this.formatTransformations = checkNotNull(formatTransformations);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  public List<AssetXO> readComponentAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter("repositoryName");
    Repository repository = repositoryManager.get(repositoryName);
    if (!repository.getConfiguration().isOnline()) {
      return Collections.emptyList();
    }

    ComponentXO componentXO = readComponent(parameters.getFilter("componentModel"));
    return componentHelper.readComponentAssets(repository, componentXO);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:selectors:*")
  public PagedResponse<AssetXO> previewAssets(final StoreLoadParameters parameters) {
    String repositoryName = parameters.getFilter("repositoryName");
    String expression = parameters.getFilter("expression");
    String type = parameters.getFilter("type");
    if (Strings2.isBlank(expression) || Strings2.isBlank(type) || Strings2.isBlank(repositoryName)) {
      return null;
    }

    selectorFactory.validateSelector(type, expression);

    RepositorySelector repositorySelector = RepositorySelector.fromSelector(repositoryName);
    List<Repository> selectedRepositories = getPreviewRepositories(repositorySelector);
    if (selectedRepositories.isEmpty()) {
      return null;
    }

    PageResult<AssetXO> result = componentHelper.previewAssets(
        repositorySelector,
        selectedRepositories,
        expression,
        toQueryOptions(parameters));

    return new PagedResponse<>(result.getTotal(), result.getResults());
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public boolean canDeleteComponent(@NotEmpty final String componentModelString) {
    ComponentXO componentXO = readComponent(componentModelString);
    Repository repository = repositoryManager.get(componentXO.getRepositoryName());
    return componentHelper.canDeleteComponent(repository, componentXO);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public Set<String> deleteComponent(@NotEmpty final String componentModelString) {
    ComponentXO componentXO = readComponent(componentModelString);
    Repository repository = repositoryManager.get(componentXO.getRepositoryName());
    return componentHelper.deleteComponent(repository, componentXO);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public boolean canDeleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    return componentHelper.canDeleteAsset(repository, new DetachedEntityId(assetId));
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public Set<String> deleteAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    // GSON used by DirectJNgine can exclude some of the Guava collection types
    return new HashSet<>(componentHelper.deleteAsset(repository, new DetachedEntityId(assetId)));
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
  public ComponentXO readComponent(@NotEmpty final String componentId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    return componentHelper.readComponent(repository, new DetachedEntityId(componentId));
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  @Nullable
  public AssetXO readAsset(@NotEmpty final String assetId, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    AssetXO assetXO = componentHelper.readAsset(repository, new DetachedEntityId(assetId));
    Optional.ofNullable(formatTransformations.get(assetXO.getFormat()))
        .ifPresent(transformation -> transformation.transform(assetXO));
    return assetXO;
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public boolean canDeleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    return componentHelper.canDeleteFolder(repository, path);
  }

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @Validate
  public void deleteFolder(@NotEmpty final String path, @NotEmpty final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    componentHelper.deleteFolder(repository, path);
  }

  private QueryOptions toQueryOptions(StoreLoadParameters storeLoadParameters) {
    StoreLoadParameters.Sort sort = storeLoadParameters.getSort() != null ? storeLoadParameters.getSort().get(0) : null;
    return new QueryOptions(
        storeLoadParameters.getFilter("filter"),
        sort != null ? sort.getProperty() : null,
        sort != null ? sort.getDirection() : null,
        storeLoadParameters.getStart(),
        storeLoadParameters.getLimit());
  }

  private List<Repository> getPreviewRepositories(final RepositorySelector repositorySelector) {
    if (!repositorySelector.isAllRepositories()) {
      return Collections.singletonList(repositoryManager.get(repositorySelector.getName()));
    }

    if (!repositorySelector.isAllFormats()) {
      return stream(repositoryManager.browse())
          .filter(repository -> repository.getFormat().getValue().equals(repositorySelector.getFormat()))
          .collect(Collectors.toList()); // NOSONAR
    }

    return stream(repositoryManager.browse()).collect(Collectors.toList()); // NOSONAR
  }

  private ComponentXO readComponent(final String componentString) {
    checkNotNull(componentString);
    try {
      return jsonMapper.readValue(componentString, ComponentXO.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
