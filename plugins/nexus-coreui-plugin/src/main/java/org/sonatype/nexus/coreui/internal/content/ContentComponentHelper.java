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
package org.sonatype.nexus.coreui.internal.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.coreui.ComponentXO;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.content.search.ComponentFinder;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorSqlBuilder;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.AuthorizationException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.repository.content.store.AssetDAO.FILTER_PARAMS;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Content-based {@link ComponentHelper}.
 *
 * @since 3.26
 */
@Named
@Singleton
public class ContentComponentHelper
    extends ComponentSupport
    implements ComponentHelper
{
  private final MaintenanceService maintenanceService;

  private final Map<String, ComponentFinder> componentFinders;

  private final ComponentFinder defaultComponentFinder;

  private final AssetPermissionChecker assetPermissionChecker;

  private final SelectorFactory selectorFactory;

  @Inject
  public ContentComponentHelper(
      final MaintenanceService maintenanceService,
      final Map<String, ComponentFinder> componentFinders,
      final AssetPermissionChecker assetPermissionChecker,
      final SelectorFactory selectorFactory)
  {
    this.maintenanceService = checkNotNull(maintenanceService);
    this.componentFinders = checkNotNull(componentFinders);
    this.assetPermissionChecker = checkNotNull(assetPermissionChecker);
    this.defaultComponentFinder = checkNotNull(componentFinders.get("default"));
    this.selectorFactory = checkNotNull(selectorFactory);
  }

  @Override
  public List<AssetXO> readComponentAssets(final Repository repository, final ComponentXO model) {
    Optional<FluentComponent> component = findComponentsByModel(repository, model).findFirst();
    if (!component.isPresent()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    Collection<FluentAsset> assets = component.get().assets();

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    return assetPermissionChecker.findPermittedAssets(assets, format, BROWSE)
        .map(entry -> toAssetXO(repositoryName, entry.getValue(), format, entry.getKey()))
        .collect(toList());
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public PageResult<AssetXO> previewAssets(
      final RepositorySelector repositorySelector,
      final List<Repository> selectedRepositories,
      final String jexlExpression,
      final QueryOptions queryOptions)
  {
    Repository repository = selectedRepositories.get(0);
    String format = repository.getFormat().getValue();

    Set<Repository> previewRepositories = new LinkedHashSet<>();
    selectedRepositories.forEach(r -> {
      if (r.getType() instanceof GroupType) {
        previewRepositories.addAll(r.facet(GroupFacet.class).leafMembers());
      }
      else {
        previewRepositories.add(r);
      }
    });

    SelectorSqlBuilder sqlBuilder = new SelectorSqlBuilder()
        .propertyAlias("path", "path")
        .propertyAlias("format", "'" + format + "'")
        .parameterPrefix("#{" + FILTER_PARAMS + ".")
        .parameterSuffix("}");

    Selector selector = selectorFactory.createSelector(CselSelector.TYPE, jexlExpression);
    selector.toSql(sqlBuilder);

    String filterString = sqlBuilder.getQueryString();
    Map<String, Object> filterParams = (Map) sqlBuilder.getQueryParameters();

    if (queryOptions.getFilter() != null) {
      filterString += " AND path LIKE #{" + FILTER_PARAMS + ".pathFilter}";
      filterParams.put("pathFilter", "%" + queryOptions.getFilter() + "%");
    }

    int numAssets = 0;
    List<AssetXO> assets = new ArrayList<>();

    for (Repository r : previewRepositories) {
      FluentQuery<FluentAsset> assetQuery = r.facet(ContentFacet.class).assets()
          .byFilter(filterString, filterParams);

      numAssets += assetQuery.count();

      int nextLimit = queryOptions.getLimit() - assets.size();
      if (nextLimit > 0) {
        assetQuery.browse(nextLimit, null).stream()
            .map(asset -> toAssetXO(r.getName(), r.getName(), format, (Asset) asset))
            .collect(Collectors.toCollection(() -> assets));
      }
    }

    return new PageResult<>(numAssets, assets);
  }

  @Override
  public ComponentXO readComponent(final Repository repository, final EntityId componentId) {
    Optional<FluentComponent> component = findComponentById(repository, componentId);
    if (!component.isPresent()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    Collection<FluentAsset> assets = component.get().assets();
    if (assets.isEmpty()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    // display component if at least one asset is permitted
    if (assetPermissionChecker.findPermittedAssets(assets, format, BROWSE).findFirst().isPresent()) {
      return toComponentXO(repositoryName, format, component.get());
    }

    throw new AuthorizationException();
  }

  @Override
  public boolean canDeleteComponent(final Repository repository, final ComponentXO model) {
    return findComponentsByModel(repository, model)
        .allMatch(component -> maintenanceService.canDeleteComponent(repository, component));
  }

  @Override
  public Set<String> deleteComponent(final Repository repository, final ComponentXO model) {
    return findComponentsByModel(repository, model)
        .flatMap(component -> maintenanceService.deleteComponent(repository, component).stream())
        .collect(toSet());
  }

  @Override
  public AssetXO readAsset(final Repository repository, final EntityId assetId) {
    Optional<FluentAsset> asset = findAssetById(repository, assetId);
    if (!asset.isPresent()) {
      throw new WebApplicationException(NOT_FOUND);
    }

    String repositoryName = repository.getName();
    String format = repository.getFormat().getValue();

    return assetPermissionChecker.isPermitted(asset.get(), format, BROWSE)
        .map(containingRepositoryName -> toAssetXO(repositoryName, containingRepositoryName, format, asset.get()))
        .orElseThrow(AuthorizationException::new);
  }

  @Override
  public boolean canDeleteAsset(final Repository repository, final EntityId assetId) {
    return findAssetById(repository, assetId)
        .filter(asset -> maintenanceService.canDeleteAsset(repository, asset))
        .isPresent();
  }

  @Override
  public Set<String> deleteAsset(final Repository repository, final EntityId assetId) {
    return findAssetById(repository, assetId)
        .map(asset -> maintenanceService.deleteAsset(repository, asset))
        .orElse(ImmutableSet.of());
  }

  @Override
  public boolean canDeleteFolder(final Repository repository, final String path) {
    return maintenanceService.canDeleteFolder(repository, path);
  }

  @Override
  public void deleteFolder(final Repository repository, final String path) {
    maintenanceService.deleteFolder(repository, path);
  }

  private Stream<FluentComponent> findComponentsByModel(final Repository repository, final ComponentXO model) {
    String format = repository.getFormat().getValue();
    ComponentFinder finder = componentFinders.getOrDefault(format, defaultComponentFinder);
    return finder.findComponentsByModel(repository,
        model.getId(),
        model.getGroup(),
        model.getName(),
        model.getVersion());
  }

  private Optional<FluentComponent> findComponentById(final Repository repository, final EntityId componentId) {
    return repository.facet(ContentFacet.class).components().find(componentId);
  }

  private Optional<FluentAsset> findAssetById(final Repository repository, final EntityId assetId) {
    return repository.facet(ContentFacet.class).assets().find(assetId);
  }

  private static ComponentXO toComponentXO(
      final String repositoryName,
      final String format,
      final Component component)
  {
    ComponentXO componentXO = new ComponentXO();
    componentXO.setRepositoryName(repositoryName);
    componentXO.setFormat(format);

    componentXO.setId(componentId(component));
    componentXO.setGroup(component.namespace());
    componentXO.setName(component.name());
    componentXO.setVersion(component.version());

    return componentXO;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static AssetXO toAssetXO(
      final String repositoryName,
      final String containingRepositoryName,
      final String format,
      final Asset asset)
  {
    AssetXO assetXO = new AssetXO();
    assetXO.setRepositoryName(repositoryName);
    assetXO.setContainingRepositoryName(containingRepositoryName);
    assetXO.setFormat(format);

    assetXO.setId(assetId(asset));
    assetXO.setName(asset.path());

    asset.component().ifPresent(component -> assetXO.setComponentId(componentId(component)));

    Map<String, Object> attributes = new HashMap<>(asset.attributes().backing());

    assetXO.setBlobCreated(Date.from(asset.created().toInstant()));
    asset.blob().ifPresent(blob -> {
      assetXO.setBlobRef(blob.blobRef().toString());
      assetXO.setSize(blob.blobSize());
      assetXO.setContentType(blob.contentType());
      assetXO.setBlobUpdated(Date.from(blob.blobCreated().toInstant()));
      attributes.put("checksum", blob.checksums());
      assetXO.setCreatedBy(blob.createdBy().orElse(null));
      assetXO.setCreatedByIp(blob.createdByIp().orElse(null));
    });

    assetXO.setAttributes((Map) attributes);

    asset.lastDownloaded().ifPresent(when -> assetXO.setLastDownloaded(Date.from(when.toInstant())));

    return assetXO;
  }

  private static String componentId(final Component component) {
    return toExternalId(internalComponentId(component)).getValue();
  }

  private static String assetId(final Asset asset) {
    return toExternalId(internalAssetId(asset)).getValue();
  }
}
