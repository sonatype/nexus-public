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
package org.sonatype.nexus.repository.content.search.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetInfo;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchStore;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionException;
import org.sonatype.nexus.repository.search.sql.SqlSearchPermissionManager;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;

/**
 * Implementation of {@link SearchService} to be used in sql search query.
 *
 * @since 3.next
 */
@Named
@Singleton
public class SqlSearchService
    extends ComponentSupport
    implements SearchService
{
  private static final String DELIMITER = "\\s+";

  private final SqlSearchUtils searchUtils;

  private final RepositoryManager repositoryManager;

  private final Map<String, FormatStoreManager> formatStoreManagersByFormat;

  private final SqlSearchPermissionManager sqlSearchPermissionManager;

  @Inject
  public SqlSearchService(
      final SqlSearchUtils searchUtils,
      final RepositoryManager repositoryManager,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final SqlSearchPermissionManager sqlSearchPermissionManager)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
    this.sqlSearchPermissionManager = checkNotNull(sqlSearchPermissionManager);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    ComponentSearchResultPage searchResultPage = executeSearch(() -> searchComponents(searchRequest));
    SearchResponse response = new SearchResponse();
    response.setSearchResults(searchResultPage.componentSearchResults);
    response.setTotalHits((long) searchResultPage.componentSearchResults.size());
    response.setContinuationToken(String.valueOf(searchResultPage.offset));
    return response;
  }

  @Override
  public Iterable<ComponentSearchResult> browse(final SearchRequest searchRequest) {
    return executeSearch( () -> searchComponents(searchRequest)).componentSearchResults;
  }

  @Override
  public long count(final SearchRequest searchRequest) {
    Optional<String> searchFormatOpt = getSearchFormat(searchRequest);
    if (searchFormatOpt.isPresent()) {
      SearchStore<?> searchStore = getSearchStore(searchFormatOpt.get());
      return executeCount(() -> {
        SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest, searchFormatOpt.get());
        return searchStore.count(queryCondition);
      });
    }
    return 0L;
  }

  private ComponentSearchResultPage searchComponents(final SearchRequest searchRequest) {
    Optional<String> searchFormatOpt = getSearchFormat(searchRequest);
    if (searchFormatOpt.isPresent()) {
      String searchFormat = searchFormatOpt.get();
      SqlSearchQueryCondition queryCondition = getSqlSearchQueryCondition(searchRequest, searchFormat);
      return searchByFormat(searchRequest, queryCondition, searchFormat);
    }

    return ComponentSearchResultPage.empty();
  }

  private SqlSearchQueryCondition getSqlSearchQueryCondition(final SearchRequest searchRequest, final String format) {
    SqlSearchQueryBuilder queryBuilder = searchUtils.buildQuery(searchRequest.getSearchFilters());
    addPermissionFilters(queryBuilder, searchRequest.getSearchFilters(), format);
    return queryBuilder.buildQuery().orElse(null);
  }

  private void addPermissionFilters(
      final SqlSearchQueryBuilder queryBuilder,
      final List<SearchFilter> searchFilters,
      final String format)
  {
    sqlSearchPermissionManager.addPermissionFilters(
        queryBuilder,
        format,
        searchUtils.getRepositoryFilter(searchFilters).map(SearchFilter::getValue).orElse(null)
    );
  }

  private ComponentSearchResultPage executeSearch(final Supplier<ComponentSearchResultPage> searchRequest) {
    try {
      return searchRequest.get();
    }
    catch (SqlSearchPermissionException ex) {
      log.debug(ex.getMessage());
    }
    return ComponentSearchResultPage.empty();
  }

  private int executeCount(final IntSupplier searchRequest) {
    try {
      return searchRequest.getAsInt();
    }
    catch (SqlSearchPermissionException ex) {
      log.debug(ex.getMessage());
    }
    return 0;
  }

  private SearchStore<?> getSearchStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.searchStore(DEFAULT_DATASTORE_NAME);
  }

  private AssetStore<?> getAssetStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.assetStore(DEFAULT_DATASTORE_NAME);
  }

  private ComponentSearchResultPage searchByFormat(
      final SearchRequest searchRequest,
      final SqlSearchQueryCondition queryCondition,
      final String format)
  {
    int offset = 0;
    String continuationToken = searchRequest.getContinuationToken();
    try {
      if (continuationToken != null) {
        offset = Integer.parseInt(continuationToken);
        if (offset < 0) {
          log.error("Continuation token [{}] should be a positive number", continuationToken);
          return ComponentSearchResultPage.empty();
        }
      }
    }
    catch (NumberFormatException e) {
      log.error("Continuation token [{}] should be a number", continuationToken);
      return ComponentSearchResultPage.empty();
    }

    SearchStore<?> searchStore = getSearchStore(format);
    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        SearchViewColumns.fromSortFieldName(searchRequest.getSortField()),
        searchRequest.getSortDirection());

    Map<Integer, List<AssetInfo>> componentIdToAsset = new HashMap<>();
    if (searchRequest.isIncludeAssets()) {
      AssetStore<?> assetStore = getAssetStore(format);
      Set<Integer> componentIds = searchResults.stream()
          .map(SearchResult::componentId)
          .collect(Collectors.toSet());
      componentIdToAsset = assetStore.findByComponentIds(componentIds).stream()
          .collect(groupingBy(AssetInfo::componentId));
    }

    List<ComponentSearchResult> componentSearchResults = new ArrayList<>(searchResults.size());
    for (SearchResult component : searchResults) {
      String repositoryName = component.repositoryName();
      ComponentSearchResult componentSearchResult = buildComponentSearchResult(component, format);
      if (searchRequest.isIncludeAssets()) {
        List<AssetInfo> assets = componentIdToAsset.get(component.componentId());
        for (AssetInfo asset : assets) {
          AssetSearchResult assetSearchResult = buildAssetSearch(asset, format, repositoryName);
          componentSearchResult.addAsset(assetSearchResult);
        }
      }
      componentSearchResults.add(componentSearchResult);
    }

    return new ComponentSearchResultPage(searchResults.size(), componentSearchResults);
  }

  private ComponentSearchResult buildComponentSearchResult(final SearchResult searchResult, final String format) {
    ComponentSearchResult componentSearchResult = new ComponentSearchResult();
    componentSearchResult.setId(String.valueOf(searchResult.componentId()));
    componentSearchResult.setFormat(format);
    componentSearchResult.setRepositoryName(searchResult.repositoryName());
    componentSearchResult.setName(searchResult.componentName());
    componentSearchResult.setGroup(searchResult.namespace());
    componentSearchResult.setVersion(searchResult.version());

    return componentSearchResult;
  }

  private AssetSearchResult buildAssetSearch(final AssetInfo asset, final String format, final String repositoryName) {
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

  private Optional<String> getSearchFormat(final SearchRequest searchRequest) {
    Set<String> formats = new HashSet<>();
    searchRequest.getRepositories().forEach(repositoryName -> addRepositoryFormat(repositoryName, formats));

    // We have to parse the SearchRequest to get a search format.
    for (SearchFilter searchFilter : searchRequest.getSearchFilters()) {
      if (REPOSITORY_NAME.equals(searchFilter.getProperty())) {
        String value = searchFilter.getValue();
        if (!Strings2.isEmpty(value)) {
          Stream.of(value.split(DELIMITER)).forEach(repositoryName -> addRepositoryFormat(repositoryName, formats));
        }
      }
      else if (FORMAT.equals(searchFilter.getProperty())) {
        String format = searchFilter.getValue();
        if (!Strings2.isEmpty(format)) {
          formats.add(format);
        }
      }
    }

    // Searching across formats is not supported
    return formats.size() == 1 ? Optional.of(formats.iterator().next()) : Optional.empty();
  }

  /**
   * Get format from the repository name and add to the {@code formats} set.
   *
   * @param repositoryName repository name to get format.
   * @param formats        add format from {@code repositoryName}.
   */
  private void addRepositoryFormat(final String repositoryName, final Set<String> formats) {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      // we shouldn't throw any exceptions here
      log.error("Can't find repository: {}. Search results will not be available for this repository", repositoryName);
    }
    else {
      formats.add(repository.getFormat().getValue());
    }
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

    private static ComponentSearchResultPage empty() {
      return new ComponentSearchResultPage(0, Collections.emptyList());
    }
  }
}
