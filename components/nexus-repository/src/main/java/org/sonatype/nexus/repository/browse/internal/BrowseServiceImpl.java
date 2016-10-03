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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

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

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final ContentPermissionChecker contentPermissionChecker;

  @Inject
  public BrowseServiceImpl(@Named(GroupType.NAME) final Type groupType,
                           final ComponentEntityAdapter componentEntityAdapter,
                           final VariableResolverAdapterManager variableResolverAdapterManager,
                           final ContentPermissionChecker contentPermissionChecker) {
    this.groupType = checkNotNull(groupType);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
  }

  @Override
  public BrowseResult<Component> browseComponents(final Repository repository,
                                                  @Nullable final String filter,
                                                  @Nullable final String sortProperty,
                                                  @Nullable final String sortDirection,
                                                  @Nullable final Integer start,
                                                  @Nullable final Integer limit)
  {
    checkNotNull(repository);
    final List<Repository> repositories = getRepositories(repository);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      List<Bucket> buckets = getBuckets(storageTx, repositories);
      QueryOptions options = new QueryOptions(filter, sortProperty, sortDirection, start, limit);
      BrowseComponentsSqlBuilder builder = new BrowseComponentsSqlBuilder(groupType.equals(repository.getType()),
          buckets, options);
      return new BrowseResult<>(
          getCount(storageTx.browse(builder.buildCountSql(), builder.buildSqlParams())),
          getComponents(storageTx.browse(builder.buildBrowseSql(), builder.buildSqlParams())));
    }
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
                                          @Nullable final String filter,
                                          @Nullable final String sortProperty,
                                          @Nullable final String sortDirection,
                                          @Nullable final Integer start,
                                          @Nullable final Integer limit)
  {
    checkNotNull(repository);
    final List<Repository> repositories = getRepositories(repository);
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();
      QueryOptions options = new QueryOptions(filter, sortProperty, sortDirection, start, limit);
      BrowseAssetsSqlBuilder builder = new BrowseAssetsSqlBuilder(options);
      return new BrowseResult<>(
          storageTx.countAssets(builder.buildWhereClause(), builder.buildSqlParams(), repositories, null),
          Lists.newArrayList(storageTx.findAssets(builder.buildWhereClause(), builder.buildSqlParams(), repositories,
              builder.buildQuerySuffix())));
    }
  }

  @Override
  public BrowseResult<Asset> previewAssets(final List<Repository> repositories,
                                           final String jexlExpression,
                                           @Nullable final String filter,
                                           @Nullable final String sortProperty,
                                           @Nullable final String sortDirection,
                                           @Nullable final Integer start,
                                           @Nullable final Integer limit)
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
      QueryOptions options = new QueryOptions(filter, sortProperty, sortDirection, start, limit);
      PreviewAssetsSqlBuilder builder = new PreviewAssetsSqlBuilder(
          repository.getName(),
          jexlExpression,
          previewRepositories.stream().map(Repository::getName).collect(Collectors.toList()),
          options);
      return new BrowseResult<>(
          storageTx.countAssets(builder.buildWhereClause(), builder.buildSqlParams(), repositories, null),
          Lists.newArrayList(storageTx.findAssets(builder.buildWhereClause(), builder.buildSqlParams(),
              previewRepositories, builder.buildQuerySuffix()))
      );
    }
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

  private long getCount(final Iterable<ODocument> results) {
    checkNotNull(results);
    return Iterables.getOnlyElement(results).field("COUNT");
  }

  private List<Component> getComponents(final Iterable<ODocument> results) {
    checkNotNull(results);
    return Lists.newArrayList(Iterables.transform(results,
        (ODocument doc) -> componentEntityAdapter.readEntity(doc.field(AssetEntityAdapter.P_COMPONENT))));
  }
}
