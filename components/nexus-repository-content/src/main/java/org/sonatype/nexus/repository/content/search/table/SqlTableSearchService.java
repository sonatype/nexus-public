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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

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
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.table.SqlSearchPermissionBuilder;
import org.sonatype.nexus.repository.search.table.TableSearchUtils;
import org.sonatype.nexus.repository.search.table.UnknownRepositoriesException;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.groupingBy;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CHECKSUM;
import static org.sonatype.nexus.repository.search.table.TableSearchUtils.isRemoveLastWords;

/**
 * {@link SearchService} implementation that uses a single search table.
 */
@Named
@Singleton
public class SqlTableSearchService
    extends ComponentSupport
    implements SearchService
{
  private static final String SEARCH_ANY_SYMBOLS = "*";

  private final TableSearchUtils searchUtils;

  private final SearchTableStore searchStore;

  private final SqlSearchPermissionBuilder sqlSearchPermissionBuilder;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final SqlSearchSortUtil sqlSearchSortUtil;

  private final Set<TableSearchResultDecorator> decorators;

  @Inject
  public SqlTableSearchService(
      final TableSearchUtils searchUtils,
      final SearchTableStore searchStore,
      final SqlSearchSortUtil sqlSearchSortUtil,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final SqlSearchPermissionBuilder sqlSearchPermissionBuilder,
      final Set<TableSearchResultDecorator> decorators)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.searchStore = checkNotNull(searchStore);
    this.sqlSearchPermissionBuilder = checkNotNull(sqlSearchPermissionBuilder);
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
    catch (SqlSearchPermissionException | UnknownRepositoriesException e) {
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
    catch (SqlSearchPermissionException | UnknownRepositoriesException e) {
      log.error(e.getMessage());
    }
    return SqlTableSearchService.ComponentSearchResultPage.empty();
  }

  private SqlSearchQueryCondition getSqlSearchQueryCondition(final SearchRequest searchRequest) {
    SqlSearchQueryBuilder queryBuilder = searchUtils.buildQuery(searchRequest);

    return sqlSearchPermissionBuilder.build(queryBuilder, searchRequest).buildQuery()
        .orElse(null);
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
    Optional<Integer> nextOffset = Optional.empty();

    if (offset == null) {
      offset = 0;
    }
    else if (offset < 0) {
      throw new BadRequestException("Continuation token must be positive");
    }

    OrderBy orderBy = getOrderBy(searchRequest);
    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        orderBy.columnName,
        orderBy.direction);

    if (searchResults.isEmpty()) {
      return SqlTableSearchService.ComponentSearchResultPage.empty();
    }
    searchResults = maybeFilterSearchResults(searchResults, offset, searchRequest, queryCondition);
    // Cut search results to satisfy the page size in the search request.
    if (searchResults.size() > searchRequest.getLimit()) {
      nextOffset = Optional.of(offset + searchResults.size());
      searchResults = searchResults.stream()
          .limit(searchRequest.getLimit())
          .collect(Collectors.toList());
    }
    else if (searchResults.size() == searchRequest.getLimit()) {
      // Only provide a reference for the next page if this one matched the provided limit.
      nextOffset = Optional.of(offset + searchRequest.getLimit());
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
        assets.stream()
            .map(asset -> buildAssetSearch(asset, repositoryName, component))
            .forEach(componentSearchResult::addAsset);
      }
      componentSearchResults.add(componentSearchResult);
    }

    return new SqlTableSearchService.ComponentSearchResultPage(nextOffset, componentSearchResults);
  }

  private OrderBy getOrderBy(final SearchRequest searchRequest) {
    String sortColumnName = sqlSearchSortUtil.getSortExpression(searchRequest.getSortField()).orElse(null);
    SortDirection sortDirection = searchRequest.getSortDirection() != null
        ? searchRequest.getSortDirection()
        : sqlSearchSortUtil.getSortDirection(searchRequest.getSortField()).orElse(null);
    return new OrderBy(sortColumnName, sortDirection);
  }

  /**
   * Filter out search results for the Keyword search criteria only in case of wildcard search request.
   */
  private Collection<SearchResult> maybeFilterSearchResults(
      final Collection<SearchResult> searchResults,
      final int offset,
      final SearchRequest searchRequest,
      final SqlSearchQueryCondition queryCondition)
  {
    Optional<SearchFilter> keywordRegexFilter = searchRequest.getSearchFilters().stream()
        .filter(filter -> "keyword".equals(filter.getProperty()) && filter.getValue().contains(SEARCH_ANY_SYMBOLS))
        .findFirst();
    if (keywordRegexFilter.isPresent()) {
      // replace a user's search request to use regex
      String regex = keywordRegexFilter.get().getValue().replace(SEARCH_ANY_SYMBOLS, ".*");
      Pattern searchPattern = Pattern.compile(regex, CASE_INSENSITIVE);
      Collection<SearchResult> filteredResults = filterSearchResults(searchResults, searchPattern);

      int nextOffset = offset + searchResults.size();
      fetchMoreResults(nextOffset, searchRequest, queryCondition, searchPattern, filteredResults);

      return filteredResults;
    }

    keywordRegexFilter = searchRequest.getSearchFilters().stream()
        .filter(filter -> "keyword".equals(filter.getProperty()) && isRemoveLastWords(filter.getValue()))
        .findFirst();
    if (keywordRegexFilter.isPresent()) {
      // use the original user's search request to filter out results
      String regex = keywordRegexFilter.get().getValue();
      Pattern searchPattern = Pattern.compile(regex, CASE_INSENSITIVE);
      Collection<SearchResult> filteredResults = filterSearchResults(searchResults, searchPattern);

      int nextOffset = offset + searchResults.size();
      fetchMoreResults(nextOffset, searchRequest, queryCondition, searchPattern, filteredResults);

      return filteredResults;
    }

    return searchResults;
  }

  /**
   * Filter search results for wildcard matching in Format, Group, Name, Version
   * and all other component metadata values.
   *
   * @param searchResults the collection with components to filter out.
   * @param searchPattern the wildcard search pattern.
   * @return the new collection with filtered search results.
   */
  private static Collection<SearchResult> filterSearchResults(
      final Collection<SearchResult> searchResults,
      final Pattern searchPattern)
  {
    Collection<SearchResult> results = new ArrayList<>();
    for (SearchResult searchResult : searchResults) {
      boolean matched = searchPattern.matcher(searchResult.componentName()).find();
      matched |= searchPattern.matcher(searchResult.format()).find();
      matched |= searchPattern.matcher(searchResult.version()).find();
      matched |= searchPattern.matcher(searchResult.normalisedVersion()).find();
      matched |= searchPattern.matcher(searchResult.repositoryName()).find();
      matched |= searchPattern.matcher(searchResult.namespace()).find();
      matched |= searchPattern.matcher(searchResult.attributes().toString()).find();
      if (matched) {
        results.add(searchResult);
      }
    }

    return results;
  }

  /**
   * Return enough results to satisfy the page size in the search request.
   */
  private void fetchMoreResults(
      final int offset,
      final SearchRequest searchRequest,
      final SqlSearchQueryCondition queryCondition,
      final Pattern searchPattern,
      final Collection<SearchResult> filteredSearchResults)
  {
    int nextOffset = offset;
    while (filteredSearchResults.size() < searchRequest.getLimit()) {
      OrderBy orderBy = getOrderBy(searchRequest);
      Collection<SearchResult> results = searchStore.searchComponents(
          searchRequest.getLimit(),
          nextOffset,
          queryCondition,
          orderBy.columnName,
          orderBy.direction);
      if (results.isEmpty()) {
        break;
      }
      nextOffset += results.size();
      filteredSearchResults.addAll(filterSearchResults(results, searchPattern));
    }
  }

  private static Optional<Integer> offsetFromToken(@Nullable final String continuationToken) {
    try {
      return ofNullable(continuationToken)
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
        List<Integer> componentIds = Lists.newArrayList(searchResult.componentId());
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
    componentSearchResult.setLastModified(searchResult.lastModified());

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
    searchResult.setFileSize(asset.blobSize());
    ofNullable(asset.lastDownloaded()).ifPresent(when -> searchResult.setLastDownloaded(Date.from(when.toInstant())));

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

  private static class OrderBy
  {
    public final String columnName;

    public final SortDirection direction;

    public OrderBy(@Nullable final String columnName, @Nullable final SortDirection direction) {
      this.columnName = columnName;
      this.direction = direction;
    }
  }
}
