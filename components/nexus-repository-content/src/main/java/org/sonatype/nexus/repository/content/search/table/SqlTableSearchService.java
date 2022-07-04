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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.table.TableSearchPermissionManager;
import org.sonatype.nexus.repository.search.table.TableSearchUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

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

  @Inject
  public SqlTableSearchService(
      final TableSearchUtils searchUtils,
      final SearchTableStore searchStore,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final TableSearchPermissionManager sqlSearchPermissionManager)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.searchStore = checkNotNull(searchStore);
    this.sqlSearchPermissionManager = checkNotNull(sqlSearchPermissionManager);
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    SqlTableSearchService.ComponentSearchResultPage searchResultPage = searchComponents(searchRequest);
    SearchResponse response = new SearchResponse();
    response.setSearchResults(searchResultPage.componentSearchResults);
    response.setTotalHits((long) searchResultPage.componentSearchResults.size());
    response.setContinuationToken(String.valueOf(searchResultPage.offset));
    return response;
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    return searchComponents(searchRequest).componentSearchResults;
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest);
    return searchStore.count(queryCondition);
  }

  private SqlTableSearchService.ComponentSearchResultPage searchComponents(final SearchRequest searchRequest) {
    SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest);
    return doSearch(searchRequest, queryCondition);
  }

  private SqlSearchQueryCondition getSqlSearchQueryCondition(final SearchRequest searchRequest) {
    SqlSearchQueryBuilder queryBuilder = searchUtils.buildQuery(searchRequest.getSearchFilters());
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
    int offset = 0;
    String continuationToken = searchRequest.getContinuationToken();
    try {
      if (continuationToken != null) {
        offset = Integer.parseInt(continuationToken);
        if (offset < 0) {
          log.error("Continuation token [{}] should be a positive number", continuationToken);
          return SqlTableSearchService.ComponentSearchResultPage.empty();
        }
      }
    }
    catch (NumberFormatException e) {
      log.error("Continuation token [{}] should be a number", continuationToken);
      return SqlTableSearchService.ComponentSearchResultPage.empty();
    }
    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        SearchViewColumns.fromSortFieldName(searchRequest.getSortField()),
        searchRequest.getSortDirection());

    if (searchResults.isEmpty()) {
      return new SqlTableSearchService.ComponentSearchResultPage(0, Collections.emptyList());
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
              buildAssetSearch(asset, repositoryName, componentSearchResult.getFormat());
          componentSearchResult.addAsset(assetSearchResult);
        }
      }
      componentSearchResults.add(componentSearchResult);
    }

    return new SqlTableSearchService.ComponentSearchResultPage(searchResults.size(), componentSearchResults);
  }

  private String getFormatComponentKey(String format, Integer componentId) {
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
    Map<String, List<Integer>> formatComponentIdMap = new HashMap<>();

    //sort the components into their respective format buckets
    for (SearchResult searchResult : searchResults) {
      if (formatComponentIdMap.containsKey(searchResult.format())) {
        formatComponentIdMap.get(searchResult.format()).add(searchResult.componentId());
      }
      else {
        List<Integer> componentIds = new ArrayList<>();
        componentIds.add(searchResult.componentId());
        formatComponentIdMap.put(searchResult.format(), componentIds);
      }
    }
    //for each format, get the asset store and fetch all the assets for the components
    for (Entry<String, List<Integer>> stringListEntry : formatComponentIdMap.entrySet()) {
      AssetStore<?> assetStore = getAssetStore(stringListEntry.getKey());
      Set<Integer> componentIds = searchResults.stream()
          .map(SearchResult::componentId)
          .collect(Collectors.toSet());
      componentIdToAsset = assetStore.findByComponentIds(componentIds).stream()
          .collect(groupingBy(assetInfo -> getFormatComponentKey(stringListEntry.getKey(), assetInfo.componentId())));
    }

    return componentIdToAsset;
  }

  private ComponentSearchResult buildComponentSearchResult(final SearchResult searchResult) {
    ComponentSearchResult componentSearchResult = new ComponentSearchResult();
    componentSearchResult.setId(String.valueOf(searchResult.componentId()));
    componentSearchResult.setFormat(searchResult.format());
    componentSearchResult.setRepositoryName(searchResult.repositoryName());
    componentSearchResult.setName(searchResult.componentName());
    componentSearchResult.setGroup(searchResult.namespace());
    componentSearchResult.setVersion(searchResult.version());
    componentSearchResult.setLastModified(searchResult.created());

    return componentSearchResult;
  }

  private AssetSearchResult buildAssetSearch(final AssetInfo asset, final String repositoryName, final String format) {
    AssetSearchResult searchResult = new AssetSearchResult();

    searchResult.setId(String.valueOf(asset.assetId()));
    searchResult.setPath(asset.path());
    searchResult.setRepository(repositoryName);
    searchResult.setFormat(format);
    searchResult.setLastModified(Date.from(asset.lastUpdated().toInstant()));
    searchResult.setAttributes(asset.attributes().backing());
    searchResult.setContentType(asset.contentType());
    searchResult.setChecksum(asset.checksums());

    return searchResult;
  }

  private static class ComponentSearchResultPage
  {
    private final int offset;

    private final List<ComponentSearchResult> componentSearchResults;

    public ComponentSearchResultPage(
        final int offset,
        final List<ComponentSearchResult> componentSearchResults)
    {
      this.offset = offset;
      this.componentSearchResults = checkNotNull(componentSearchResults);
    }

    private static SqlTableSearchService.ComponentSearchResultPage empty() {
      return new SqlTableSearchService.ComponentSearchResultPage(0, Collections.emptyList());
    }
  }
}
