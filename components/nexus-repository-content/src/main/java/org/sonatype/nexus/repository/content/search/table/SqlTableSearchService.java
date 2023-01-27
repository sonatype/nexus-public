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
package org.sonatype.nexus.repository.content.search.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.table.TableSearchPermissionManager;
import org.sonatype.nexus.repository.search.table.TableSearchUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CHECKSUM;

/**
 * {@link SearchService} implementation that uses a single search table.
 */
@Named
@Singleton
public class SqlTableSearchService
    extends ComponentSupport
    implements SearchService
{
  private final TableSearchUtils searchUtils;

  private final SearchTableStore searchStore;

  private final TableSearchPermissionManager sqlSearchPermissionManager;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final SqlSearchSortUtil sqlSearchSortUtil;

  private final Set<TableSearchResultDecorator> decorators;

  @Inject
  public SqlTableSearchService(
      final TableSearchUtils searchUtils,
      final SearchTableStore searchStore,
      final SqlSearchSortUtil sqlSearchSortUtil,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final TableSearchPermissionManager sqlSearchPermissionManager,
      final Set<TableSearchResultDecorator> decorators)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.searchStore = checkNotNull(searchStore);
    this.sqlSearchPermissionManager = checkNotNull(sqlSearchPermissionManager);
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
    this.sqlSearchSortUtil = checkNotNull(sqlSearchSortUtil);
    this.decorators = checkNotNull(decorators);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    SqlTableSearchService.ComponentSearchResultPage searchResultPage = searchComponents(searchRequest);
    SearchResponse response = new SearchResponse();
    response.setSearchResults(searchResultPage.componentSearchResults);
    response.setTotalHits((long) searchResultPage.componentSearchResults.size());

    searchResultPage.nextOffset
        .map(String::valueOf)
        .ifPresent(response::setContinuationToken);

    return response;
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    return searchComponents(searchRequest).componentSearchResults;
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    try {
      SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest);
      return searchStore.count(queryCondition);
    }
    catch (SqlSearchPermissionException e) {
      log.error(e.getMessage());
    }
    return 0L;
  }

  private SqlTableSearchService.ComponentSearchResultPage searchComponents(final SearchRequest searchRequest) {
    try {
      SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest);
      log.debug("Query: {}", queryCondition);
      return doSearch(searchRequest, queryCondition);
    }
    catch (SqlSearchPermissionException e) {
      throw new ForbiddenException(e.getMessage());
    }
  }

  private SqlSearchQueryCondition getSqlSearchQueryCondition(final SearchRequest searchRequest) {
    SqlSearchQueryBuilder queryBuilder = searchUtils.buildQuery(searchRequest);
    addPermissionFilters(queryBuilder, searchRequest.getSearchFilters());
    return queryBuilder.buildQuery().orElse(null);
  }

  private void addPermissionFilters(
      final SqlSearchQueryBuilder queryBuilder,
      final List<SearchFilter> searchFilters)
  {
    sqlSearchPermissionManager.addPermissionFilters(
        queryBuilder,
        searchUtils.getRepositoryFilter(searchFilters).map(SearchFilter::getValue).orElse(null)
    );
  }

  private AssetStore<?> getAssetStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.assetStore(DEFAULT_DATASTORE_NAME);
  }

  private SqlTableSearchService.ComponentSearchResultPage doSearch(
      final SearchRequest searchRequest,
      final SqlSearchQueryCondition queryCondition)
  {
    Integer offset = offsetFromToken(searchRequest.getContinuationToken())
        .orElseGet(searchRequest::getOffset);

    if (offset == null) {
      offset = 0;
    }
    else if (offset < 0) {
      throw new BadRequestException("Continuation token must be positive");
    }

    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        sqlSearchSortUtil.getSortExpression(searchRequest.getSortField()).orElse(null),
        searchRequest.getSortDirection());

    if (searchResults.isEmpty()) {
      return SqlTableSearchService.ComponentSearchResultPage.empty();
    }

    Map<String, List<AssetInfo>> componentIdToAsset =
        getAssetsForComponents(searchResults, searchRequest.isIncludeAssets());

    List<ComponentSearchResult> componentSearchResults = new ArrayList<>(searchResults.size());
    for (SearchResult component : searchResults) {
      String repositoryName = component.repositoryName();
      ComponentSearchResult componentSearchResult = buildComponentSearchResult(component);
      if (searchRequest.isIncludeAssets()) {
        List<AssetInfo> assets =
            componentIdToAsset.get(getFormatComponentKey(component.format(), component.componentId()));
        for (AssetInfo asset : assets) {
          AssetSearchResult assetSearchResult =
              buildAssetSearch(asset, repositoryName, component);
          componentSearchResult.addAsset(assetSearchResult);
        }
      }
      componentSearchResults.add(componentSearchResult);
    }

    Optional<Integer> nextOffset = Optional.empty();
    if (searchResults.size() == searchRequest.getLimit()) {
      // Only provide a reference for the next page if this one matched the provided limit.
      nextOffset = Optional.of(offset + searchRequest.getLimit());
    }

    return new SqlTableSearchService.ComponentSearchResultPage(nextOffset, componentSearchResults);
  }

  private static Optional<Integer> offsetFromToken(@Nullable final String continuationToken) {
    try {
      return Optional.ofNullable(continuationToken)
           .map(Integer::parseInt);
    }
    catch (NumberFormatException e) {
      throw new BadRequestException("Continuation token should be a number");
    }
  }

  private String getFormatComponentKey(final String format, final Integer componentId) {
    return format + componentId;
  }

  private Map<String, List<AssetInfo>> getAssetsForComponents(
      final Collection<SearchResult> searchResults,
      final boolean includeAssets)
  {
    if (!includeAssets) {
      return Collections.emptyMap();
    }

    // <Format+Component ID, List<Asset>>
    Map<String, List<AssetInfo>> componentIdToAsset = new HashMap<>();
    // <Format, List<component id>>
    Map<String, List<Integer>> componentIdsByFormat = new HashMap<>();

    //sort the components into their respective format buckets
    for (SearchResult searchResult : searchResults) {
      if (componentIdsByFormat.containsKey(searchResult.format())) {
        componentIdsByFormat.get(searchResult.format()).add(searchResult.componentId());
      }
      else {
        List<Integer> componentIds = new ArrayList<>();
        componentIds.add(searchResult.componentId());
        componentIdsByFormat.put(searchResult.format(), componentIds);
      }
    }
    //for each format, get the asset store and fetch all the assets for the components
    for (Entry<String, List<Integer>> formatComponentIds : componentIdsByFormat.entrySet()) {
      AssetStore<?> assetStore = getAssetStore(formatComponentIds.getKey());
      Set<Integer> componentIds = new HashSet<>(formatComponentIds.getValue());
      componentIdToAsset.putAll(assetStore.findByComponentIds(componentIds).stream()
          .collect(groupingBy(assetInfo ->
              getFormatComponentKey(formatComponentIds.getKey(), assetInfo.componentId()))));
    }

    return componentIdToAsset;
  }

  private ComponentSearchResult buildComponentSearchResult(final SearchResult searchResult) {
    ComponentSearchResult componentSearchResult = new ComponentSearchResult();
    componentSearchResult.setId(InternalIds.toExternalId(searchResult.componentId()).getValue());
    componentSearchResult.setFormat(searchResult.format());
    componentSearchResult.setRepositoryName(searchResult.repositoryName());
    componentSearchResult.setName(searchResult.componentName());
    componentSearchResult.setGroup(searchResult.namespace());
    componentSearchResult.setVersion(searchResult.version());
    componentSearchResult.setLastModified(searchResult.created());

    decorators.forEach(decorator -> decorator.updateComponent(componentSearchResult, searchResult));
    return componentSearchResult;
  }

  private AssetSearchResult buildAssetSearch(final AssetInfo asset,
                                             final String repositoryName,
                                             final SearchResult componentInfo) {
    AssetSearchResult searchResult = new AssetSearchResult();

    searchResult.setId(String.valueOf(asset.assetId()));
    searchResult.setPath(asset.path());
    searchResult.setRepository(repositoryName);
    searchResult.setFormat(componentInfo.format());
    searchResult.setLastModified(Date.from(asset.lastUpdated().toInstant()));
    searchResult.setAttributes(asset.attributes().backing());
    searchResult.getAttributes().put(CHECKSUM, asset.checksums());
    searchResult.setContentType(asset.contentType());
    searchResult.setChecksum(asset.checksums());
    searchResult.setUploader(asset.createdBy());
    searchResult.setUploaderIp(asset.createdByIp());

    return searchResult;
  }

  private static class ComponentSearchResultPage
  {
    private final Optional<Integer> nextOffset;

    private final List<ComponentSearchResult> componentSearchResults;

    public ComponentSearchResultPage(
        final Optional<Integer> nextOffset,
        final List<ComponentSearchResult> componentSearchResults)
    {
      this.nextOffset = nextOffset;
      this.componentSearchResults = checkNotNull(componentSearchResults);
    }

    private static SqlTableSearchService.ComponentSearchResultPage empty() {
      return new SqlTableSearchService.ComponentSearchResultPage(Optional.empty(), Collections.emptyList());
    }
  }
}
