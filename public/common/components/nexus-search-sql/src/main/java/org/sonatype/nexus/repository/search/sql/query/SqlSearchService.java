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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.ws.rs.BadRequestException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.security.AssetPermissionChecker;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SearchResult;
import org.sonatype.nexus.repository.search.sql.SqlSearchResultDecorator;
import org.sonatype.nexus.repository.search.sql.index.SqlSearchEventHandler;
import org.sonatype.nexus.repository.search.sql.query.security.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.query.security.UnknownRepositoriesException;
import org.sonatype.nexus.repository.search.sql.store.SearchStore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.CHECKSUM;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * {@link SearchService} implementation that uses a single search table.
 */
@Component
@Singleton
public class SqlSearchService
    extends ComponentSupport
    implements SearchService
{
  private final SearchStore searchStore;

  private final ExpressionBuilder expressionBuilder;

  private final SearchRequestModifier requestModifier;

  private final SearchConditionFactory queryFactory;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final SqlSearchSortUtil sqlSearchSortUtil;

  private final Set<SqlSearchResultDecorator> decorators;

  private final SqlSearchEventHandler sqlSearchEventHandler;

  private final AssetPermissionChecker assetPermissionChecker;

  private static final long CALM_TIMEOUT_MS = 30000;

  private static final long CALM_POLL_INTERVAL_MS = 100;

  @Inject
  public SqlSearchService(
      final SearchStore searchStore,
      final SqlSearchSortUtil sqlSearchSortUtil,
      final List<FormatStoreManager> formatStoreManagersByFormatList,
      final ExpressionBuilder expressionBuilder,
      final SearchRequestModifier requestModifier,
      final SearchConditionFactory queryFactory,
      final Set<SqlSearchResultDecorator> decorators,
      final SqlSearchEventHandler sqlSearchEventHandler,
      final AssetPermissionChecker assetPermissionChecker)
  {
    this.searchStore = checkNotNull(searchStore);
    this.expressionBuilder = checkNotNull(expressionBuilder);
    this.requestModifier = checkNotNull(requestModifier);
    this.queryFactory = checkNotNull(queryFactory);
    this.formatStoreManagersByFormat =
        QualifierUtil.buildQualifierBeanMap(checkNotNull(formatStoreManagersByFormatList));
    this.sqlSearchSortUtil = checkNotNull(sqlSearchSortUtil);
    this.decorators = checkNotNull(decorators);
    this.sqlSearchEventHandler = checkNotNull(sqlSearchEventHandler);
    this.assetPermissionChecker = checkNotNull(assetPermissionChecker);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    SqlSearchService.ComponentSearchResultPage searchResultPage = searchComponents(searchRequest);
    SearchResponse response = new SearchResponse();
    response.setSearchResults(searchResultPage.componentSearchResults);

    response.setTotalHits(count(searchRequest));

    searchResultPage.nextOffset
        .map(String::valueOf)
        .ifPresent(response::setContinuationToken);

    return response;
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    return () -> new ComponentSearchResultIterator(searchRequest);
  }

  private class ComponentSearchResultIterator
      implements Iterator<ComponentSearchResult>
  {
    private final SearchRequest searchRequest;

    private Iterator<ComponentSearchResult> currentBatchIterator;

    private int currentOffset;

    private boolean hasMoreResults = true;

    ComponentSearchResultIterator(final SearchRequest searchRequest) {
      this.searchRequest = searchRequest;
      this.currentOffset = searchRequest.getOffset() != null ? searchRequest.getOffset() : 0;
    }

    @Override
    public boolean hasNext() {
      if (!hasMoreResults) {
        return false;
      }

      if (currentBatchIterator == null || !currentBatchIterator.hasNext()) {
        fetchNextBatch();
      }

      return currentBatchIterator.hasNext();
    }

    private void fetchNextBatch() {
      SearchRequest batchRequest = SearchRequest.builder()
          .from(searchRequest)
          .offset(currentOffset)
          .build();

      SqlSearchService.ComponentSearchResultPage resultPage = searchComponents(batchRequest);
      List<ComponentSearchResult> results = resultPage.componentSearchResults;

      currentBatchIterator = results.iterator();
      currentOffset += results.size();
      hasMoreResults = results.iterator().hasNext();
    }

    @Override
    public ComponentSearchResult next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return currentBatchIterator.next();
    }
  }

  @Override
  public void waitForCalm() {
    log.debug("Waiting for calm period to allow SQL search indexing to complete");
    try {
      long startTime = System.currentTimeMillis();
      long endTime = startTime + CALM_TIMEOUT_MS;

      while (System.currentTimeMillis() < endTime) {
        if (sqlSearchEventHandler.isCalmPeriod()) {
          long elapsed = System.currentTimeMillis() - startTime;
          log.debug("Search indexing completed after {}ms", elapsed);
          return;
        }
        Thread.sleep(CALM_POLL_INTERVAL_MS);
      }

      log.warn("Timed out waiting for search indexing to complete after {}ms", CALM_TIMEOUT_MS);
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Waiting for calm period has been interrupted", e);
    }
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    try {
      SqlSearchQueryConditionGroup queryCondition = getSqlSearchQueryCondition(searchRequest);
      return searchStore.count(queryCondition);
    }
    catch (SqlSearchPermissionException | UnknownRepositoriesException e) {
      log.error(e.getMessage());
    }
    return 0L;
  }

  private SqlSearchService.ComponentSearchResultPage searchComponents(final SearchRequest searchRequest) {
    try {
      SqlSearchQueryConditionGroup queryCondition = getSqlSearchQueryCondition(searchRequest);
      SearchRequest modifiedSearchRequest = getModifiedSearchRequest(searchRequest);
      log.debug("Query: {}", queryCondition);
      return doSearch(modifiedSearchRequest, queryCondition);
    }
    catch (SqlSearchPermissionException | UnknownRepositoriesException e) {
      log.error(e.getMessage());
    }
    return SqlSearchService.ComponentSearchResultPage.empty();
  }

  @Nullable
  private SqlSearchQueryConditionGroup getSqlSearchQueryCondition(final SearchRequest searchRequest) {
    return expressionBuilder.from(searchRequest)
        .map(queryFactory::build)
        .orElse(null);
  }

  private SearchRequest getModifiedSearchRequest(final SearchRequest searchRequest) {
    return requestModifier.modify(searchRequest);
  }

  private AssetStore<?> getAssetStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.assetStore(DEFAULT_DATASTORE_NAME);
  }

  private Integer offsetFromSearchRequest(final SearchRequest searchRequest) {
    Integer offset = offsetFromToken(searchRequest.getContinuationToken())
        .orElseGet(searchRequest::getOffset);

    if (offset == null) {
      offset = 0;
    }
    else if (offset < 0) {
      throw new BadRequestException("Continuation token must be positive");
    }
    return offset;
  }

  private SqlSearchService.ComponentSearchResultPage doSearch(
      final SearchRequest searchRequest,
      @Nullable final SqlSearchQueryConditionGroup queryCondition)
  {
    Optional<Integer> nextOffset = Optional.empty();
    Integer offset = offsetFromSearchRequest(searchRequest);
    Boolean distinct = searchRequest.isDistinctNameAndNamespace();

    OrderBy orderBy = getOrderBy(searchRequest);
    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        orderBy.columnName,
        orderBy.direction,
        distinct);

    if (searchResults.isEmpty()) {
      return SqlSearchService.ComponentSearchResultPage.empty();
    }
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

    Map<String, List<Asset>> componentIdToAsset =
        maybeGetAssetsForComponents(searchResults, searchRequest.isIncludeAssets(), queryCondition);

    List<ComponentSearchResult> componentSearchResults = new ArrayList<>(searchResults.size());
    for (SearchResult component : searchResults) {
      String repositoryName = component.repositoryName();
      ComponentSearchResult componentSearchResult = buildComponentSearchResult(component);

      List<Asset> assets = componentIdToAsset.get(getFormatComponentKey(component.format(), component.componentId()));

      if (assets != null) {
        assetPermissionChecker.findPermittedAssets(assets, component.format(), BROWSE)
            .map(Entry::getKey)
            .map(asset -> buildAssetSearch(asset, repositoryName, component))
            .forEach(componentSearchResult::addAsset);
      }
      componentSearchResults.add(componentSearchResult);
    }

    return new SqlSearchService.ComponentSearchResultPage(nextOffset, componentSearchResults);
  }

  private OrderBy getOrderBy(final SearchRequest searchRequest) {
    String sortField = searchRequest.getSortField();

    if (sortField == null) {
      return new OrderBy(null, null);
    }

    Optional<String> sortColumnName = sqlSearchSortUtil.getSortExpression(sortField);
    if (sortColumnName.isEmpty()) {
      throw new ValidationErrorsException("sort",
          String.format("Invalid sort field: '%s'. Please use a valid sort field.", sortField));
    }

    SortDirection sortDirection = searchRequest.getSortDirection();
    if (sortDirection == null) {
      sortDirection = sqlSearchSortUtil.getSortDirection(sortField).orElse(null);
    }

    if (searchRequest.isDistinctNameAndNamespace()) {
      String column = sortColumnName.orElse(null);
      if ("cs.namespace".equals(column) || "cs.search_component_name".equals(column)) {
        // Ordering is already applied via DISTINCT ON columns, no need to duplicate in ORDER BY.
        // isDistinctNameAndNamespace is only used on npm search which not accept custom sortDirection
        return new OrderBy(null, null);
      }
    }

    return new OrderBy(sortColumnName.orElse(null), sortDirection);
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

  private Map<String, List<Asset>> maybeGetAssetsForComponents(
      final Collection<SearchResult> searchResults,
      final boolean includeAssets,
      final SqlSearchQueryConditionGroup queryCondition)
  {
    if (!includeAssets) {
      return Map.of();
    }

    // <Format+Component ID, List<Asset>>
    Map<String, List<Asset>> componentIdToAsset = new HashMap<>();
    // <Format, List<component id>>
    Map<String, List<Integer>> componentIdsByFormat = new HashMap<>();

    // sort the components into their respective format buckets
    for (SearchResult searchResult : searchResults) {
      if (componentIdsByFormat.containsKey(searchResult.format())) {
        componentIdsByFormat.get(searchResult.format()).add(searchResult.componentId());
      }
      else {
        List<Integer> componentIds = Lists.newArrayList(searchResult.componentId());
        componentIdsByFormat.put(searchResult.format(), componentIds);
      }
    }
    // for each format, get the asset store and fetch all the assets for the components
    for (Entry<String, List<Integer>> formatComponentIds : componentIdsByFormat.entrySet()) {
      AssetStore<?> assetStore = getAssetStore(formatComponentIds.getKey());
      Set<Integer> componentIds = new HashSet<>(formatComponentIds.getValue());
      final Optional<SqlSearchQueryCondition> assetCondition = Optional.ofNullable(queryCondition)
          .flatMap(SqlSearchQueryConditionGroup::getAssetCondition);
      componentIdToAsset.putAll(assetStore.findByComponentIds(componentIds,
          assetCondition.map(SqlSearchQueryCondition::getSqlFilter).orElse(null),
          assetCondition.map(SqlSearchQueryCondition::getParameters).orElse(null))
          .stream()
          .collect(
              groupingBy(asset -> getFormatComponentKey(formatComponentIds.getKey(),
                  InternalIds.internalComponentId(asset).getAsInt()))));
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

  @VisibleForTesting
  static AssetSearchResult buildAssetSearch(
      final Asset asset,
      final String repositoryName,
      final SearchResult componentInfo)
  {
    AssetSearchResult searchResult = new AssetSearchResult();

    searchResult.setId(InternalIds.toExternalId(InternalIds.internalAssetId(asset)).getValue());
    searchResult.setPath(asset.path());
    searchResult.setRepository(repositoryName);
    searchResult.setFormat(componentInfo.format());
    searchResult.setAttributes(asset.attributes().backing());
    searchResult.setBlobCreated(DateHelper.toDate(asset.created()));

    asset.blob().ifPresent(blob -> {
      searchResult.setLastModified(DateHelper.toDate(blob.blobCreated()));
      searchResult.getAttributes().put(CHECKSUM, blob.checksums());
      searchResult.setContentType(blob.contentType());
      searchResult.setChecksum(blob.checksums());
      searchResult.setFileSize(blob.blobSize());
      blob.createdBy().ifPresent(searchResult::setUploader);
      blob.createdByIp().ifPresent(searchResult::setUploaderIp);
    });
    asset.lastDownloaded()
        .map(DateHelper::toDate)
        .ifPresent(searchResult::setLastDownloaded);

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

    private static SqlSearchService.ComponentSearchResultPage empty() {
      return new SqlSearchService.ComponentSearchResultPage(Optional.empty(), Collections.emptyList());
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
