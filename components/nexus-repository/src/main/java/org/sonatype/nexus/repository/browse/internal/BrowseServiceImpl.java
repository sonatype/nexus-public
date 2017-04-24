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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.MetadataNode;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.StreamSupport.stream;

/**
 * Implementation of {@link BrowseService}.
 *
 * @since 3.1
 */
@Named
@Singleton
public class BrowseServiceImpl
    extends ComponentSupport
    implements BrowseService
{
  private final Type groupType;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final AssetEntityAdapter assetEntityAdapter;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final ContentPermissionChecker contentPermissionChecker;

  @Inject
  public BrowseServiceImpl(@Named(GroupType.NAME) final Type groupType,
                           final ComponentEntityAdapter componentEntityAdapter,
                           final VariableResolverAdapterManager variableResolverAdapterManager,
                           final ContentPermissionChecker contentPermissionChecker,
                           final AssetEntityAdapter assetEntityAdapter)
  {
    this.groupType = checkNotNull(groupType);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.assetEntityAdapter = assetEntityAdapter;
  }

  @Override
  public BrowseResult<Component> browseComponents(final Repository repository,
                                                  final QueryOptions queryOptions)
  {
    checkNotNull(repository);
    final List<Repository> repositories = getRepositories(repository);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      List<Bucket> buckets = getBuckets(storageTx, repositories);
      List<Component> components = Collections.emptyList();
      // ensure there are components before incurring contentAuth overhead
      if (hasComponents(storageTx, repository, buckets, queryOptions)) {
        BrowseComponentsSqlBuilder builder = new BrowseComponentsSqlBuilder(repository.getName(),
            buckets, queryOptions);
        components = getComponents(storageTx.browse(builder.buildBrowseSql(), builder.buildSqlParams()));
      }
      return new BrowseResult<>(estimateCount(queryOptions, components), components);
    }
  }

  private boolean hasComponents(final StorageTx storageTx, final Repository repository, final List<Bucket> buckets,
                                final QueryOptions queryOptions)
  {
    QueryOptions adjustedOptions = new QueryOptions(queryOptions.getFilter(), null, null, 0, 1, null, false);
    BrowseComponentsSqlBuilder builder = new BrowseComponentsSqlBuilder(repository.getName(),
        buckets, adjustedOptions);
    Iterable<ODocument> docs = storageTx.browse(builder.buildBrowseSql(), builder.buildSqlParams());
    return docs.iterator().hasNext();
  }

  private long estimateCount(final QueryOptions queryOptions, final List<?> items) {
    long count = items.size();
    if (queryOptions.getStart() != null && queryOptions.getLimit() != null) {
      count += queryOptions.getStart();
      // estimate an additional page if the number of items returned was limited
      if (items.size() == queryOptions.getLimit()) {
        count += queryOptions.getLimit();
      }
    }
    return count;
  }

  @Override
  public BrowseResult<Asset> browseComponentAssets(final Repository repository, final String componentId)
  {
    checkNotNull(repository);
    checkNotNull(componentId);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      Component component = storageTx.findComponent(new DetachedEntityId(componentId));
      if (component == null) {
        return new BrowseResult<>(0, Collections.emptyList());
      }
      VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(component.format());
      List<Asset> assets = StreamSupport.stream(storageTx.browseAssets(component).spliterator(), false)
          .filter(
              (Asset asset) -> contentPermissionChecker.isPermitted(
                  repository.getName(),
                  asset.format(),
                  BreadActions.BROWSE,
                  variableResolverAdapter.fromAsset(asset))
          ).collect(Collectors.toList());
      return new BrowseResult<>(assets.size(), assets);
    }
  }

  @Override
  public BrowseResult<Asset> browseAssets(final Repository repository,
                                          final QueryOptions queryOptions)
  {
    checkNotNull(repository);
    final List<Repository> repositories = getRepositories(repository);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      BrowseAssetsSqlBuilder builder = new BrowseAssetsSqlBuilder(repository.getName(), queryOptions);
      List<Asset> assets = Collections.emptyList();
      // ensure there are assets before incurring contentAuth overhead
      if (hasAssets(storageTx, repository, repositories, queryOptions)) {
        assets = Lists.newArrayList(storageTx.findAssets(builder.buildWhereClause(), builder.buildSqlParams(),
            repositories, builder.buildQuerySuffix()));
      }
      return new BrowseResult<>(estimateCount(queryOptions,assets), assets);
    }
  }

  private boolean hasAssets(final StorageTx storageTx, final Repository repository, final List<Repository> repositories,
                            final QueryOptions queryOptions)
  {
    QueryOptions adjustedOptions = new QueryOptions(queryOptions.getFilter(), null, null, 0, 1, null, false);
    BrowseAssetsSqlBuilder builder = new BrowseAssetsSqlBuilder(repository.getName(), adjustedOptions);
    Iterable<Asset> docs = storageTx.findAssets(builder.buildWhereClause(), builder.buildSqlParams(),
        repositories, builder.buildQuerySuffix());
    return docs.iterator().hasNext();
  }

  @Override
  public BrowseResult<Asset> previewAssets(final RepositorySelector repositorySelector,
                                           final List<Repository> repositories,
                                           final String jexlExpression,
                                           final QueryOptions queryOptions)
  {
    checkNotNull(repositories);
    checkNotNull(jexlExpression);
    final Repository repository = repositories.get(0);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      List<Repository> previewRepositories;
      if (repositories.size() == 1 && groupType.equals(repository.getType())) {
        previewRepositories = repository.facet(GroupFacet.class).leafMembers();
      }
      else {
        previewRepositories = repositories;
      }

      PreviewAssetsSqlBuilder builder = new PreviewAssetsSqlBuilder(
          repositorySelector,
          jexlExpression,
          queryOptions,
          getRepoToContainedGroupMap(repositories));
      return new BrowseResult<>(
          storageTx.countAssets(builder.buildWhereClause(), builder.buildSqlParams(), previewRepositories, null),
          Lists.newArrayList(storageTx.findAssets(builder.buildWhereClause(), builder.buildSqlParams(),
              previewRepositories, builder.buildQuerySuffix()))
      );
    }
  }

  @Override
  public Asset getAssetById(final ORID assetId, final Repository repository) {
    checkNotNull(repository);
    checkNotNull(assetId);

    return getById(assetId, repository, "asset", assetEntityAdapter);
  }

  @Override
  public Component getComponentById(final ORID componentId, final Repository repository) {
    checkNotNull(repository);
    checkNotNull(componentId);

    return getById(componentId, repository, "component", componentEntityAdapter);
  }

  private <T extends MetadataNode<?>> T getById(final ORID orid,
                                                final Repository repository,
                                                final String tableName,
                                                final MetadataNodeEntityAdapter<T> adapter)
  {
    String sql = format("SELECT * FROM %s WHERE contentAuth(@this, :browsedRepository) == true AND @RID == :rid",
        tableName);

    Map<String, Object> params = ImmutableMap
        .of("browsedRepository", repository.getName(), "rid", orid.toString());

    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      return stream(storageTx.browse(sql, params).spliterator(), false)
          .map(adapter::readEntity).findFirst().orElse(null);
    }
  }

  @VisibleForTesting
  Map<String, List<String>> getRepoToContainedGroupMap(List<Repository> repositories) {
    Map<String, List<String>> repoToContainedGroupMap = new HashMap<>();
    for (Repository repository : repositories) {
      List<String> groupNames = new ArrayList<>();
      groupNames.add(repository.getName());
      groupNames.addAll(repositories.stream().filter(groupRepository -> {
        Optional<GroupFacet> groupFacet = groupRepository.optionalFacet(GroupFacet.class);
        return groupFacet.isPresent() && groupFacet.get().leafMembers().stream()
            .anyMatch(leafMember -> repository.getName().equals(leafMember.getName()));
      }).map(groupRepository -> groupRepository.getName()).collect(Collectors.toSet()));
      repoToContainedGroupMap.put(repository.getName(), groupNames);
    }
    return repoToContainedGroupMap;
  }

  private List<Repository> getRepositories(final Repository repository) {
    checkNotNull(repository);
    if (groupType.equals(repository.getType())) {
      return repository.facet(GroupFacet.class).leafMembers();
    }
    return ImmutableList.of(repository);
  }

  private List<Bucket> getBuckets(final StorageTx storageTx, final Iterable<Repository> repositories) {
    checkNotNull(storageTx);
    checkNotNull(repositories);
    Iterable<Bucket> buckets = storageTx.findBuckets(repositories);
    if (buckets == null) {
      return Collections.emptyList();
    }
    return Lists.newArrayList(buckets);
  }

  private List<Component> getComponents(final Iterable<ODocument> results) {
    checkNotNull(results);
    return Lists.newArrayList(Iterables.transform(results,
        (ODocument doc) -> componentEntityAdapter.readEntity(doc)));
  }
}
