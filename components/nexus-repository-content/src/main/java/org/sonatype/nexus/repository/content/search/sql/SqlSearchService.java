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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchStore;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchUtils;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.SecurityHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

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

  private final SecurityHelper securityHelper;

  @Inject
  public SqlSearchService(
      final SqlSearchUtils searchUtils,
      final RepositoryManager repositoryManager,
      final Map<String, FormatStoreManager> formatStoreManagersByFormat,
      final SecurityHelper securityHelper)
  {
    this.searchUtils = checkNotNull(searchUtils);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.formatStoreManagersByFormat = checkNotNull(formatStoreManagersByFormat);
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  public SearchResponse search(final SearchRequest searchRequest) {
    ComponentSearchResultPage searchResultPage = searchComponents(searchRequest);
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
    Optional<String> searchFormatOpt = getSearchFormat(searchRequest);
    if (searchFormatOpt.isPresent()) {
      SearchStore<?> searchStore = getSearchStore(searchFormatOpt.get());
      return searchStore.count();
    }

    return 0L;
  }

  private ComponentSearchResultPage searchComponents(final SearchRequest searchRequest) {
    SqlSearchQueryBuilder queryBuilder = searchUtils.buildQuery(searchRequest.getSearchFilters());
    SqlSearchQueryCondition queryCondition = queryBuilder.buildQuery().orElse(null);

    Optional<String> searchFormatOpt = getSearchFormat(searchRequest);
    if (searchFormatOpt.isPresent()) {
      String searchFormat = searchFormatOpt.get();
      SearchStore<?> searchStore = getSearchStore(searchFormat);
      return searchByFormat(searchStore, searchRequest, queryCondition, searchFormat);
    }

    return ComponentSearchResultPage.empty();
  }

  private SearchStore<?> getSearchStore(final String format) {
    FormatStoreManager formatStoreManager = formatStoreManagersByFormat.get(format);
    return formatStoreManager.searchStore(DEFAULT_DATASTORE_NAME);
  }

  private ComponentSearchResultPage searchByFormat(
      final SearchStore<?> searchStore,
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

    Collection<SearchResult> searchResults = searchStore.searchComponents(
        searchRequest.getLimit(),
        offset,
        queryCondition,
        SearchViewColumns.COMPONENT_ID, //TODO will be changed in the scope of NEXUS-29476
        searchRequest.getSortDirection());

    // we have to save ordering here
    Map<Integer, ComponentSearchResult> componentById = new LinkedHashMap<>();
    Map<String, Boolean> repositoryNameByAccess = getPermittedRepositories(searchResults);
    for (SearchResult component : searchResults) {
      if (TRUE.equals(repositoryNameByAccess.get(component.repositoryName()))) {
        ComponentSearchResult componentSearchResult = componentById.get(component.componentId());
        if (componentSearchResult == null) {
          componentSearchResult = buildComponentSearchResult(component, format);
          componentById.put(component.componentId(), componentSearchResult);
        }
        AssetSearchResult assetSearchResult = buildAssetSearch(component, format);
        componentSearchResult.addAsset(assetSearchResult);
      }
    }

    return new ComponentSearchResultPage(searchResults.size(), newArrayList(componentById.values()));
  }

  private Map<String, Boolean> getPermittedRepositories(final Collection<SearchResult> components) {
    Map<String, Boolean> repositoryNameByAccess = new HashMap<>();
    for (SearchResult componentSearch : components) {
      String repositoryName = componentSearch.repositoryName();
      Boolean permitted = repositoryNameByAccess.get(repositoryName);
      if (permitted == null) {
        Repository repository = repositoryManager.get(repositoryName);
        // we must pre-filter repositories we can access
        if (repository != null && securityHelper.allPermitted(new RepositoryViewPermission(repository, BROWSE))) {
          repositoryNameByAccess.put(repositoryName, TRUE);
        }
        else {
          repositoryNameByAccess.put(repositoryName, FALSE);
        }
      }
    }

    return repositoryNameByAccess;
  }

  private ComponentSearchResult buildComponentSearchResult(final SearchResult assetSearch, final String format) {
    ComponentSearchResult searchResult = new ComponentSearchResult();
    searchResult.setId(String.valueOf(assetSearch.componentId()));
    searchResult.setFormat(format);
    searchResult.setRepositoryName(assetSearch.repositoryName());
    searchResult.setName(assetSearch.componentName());
    searchResult.setGroup(assetSearch.namespace());
    searchResult.setVersion(assetSearch.version());

    return searchResult;
  }

  private AssetSearchResult buildAssetSearch(final SearchResult assetSearch, final String format) {
    AssetSearchResult searchResult = new AssetSearchResult();
    searchResult.setId(String.valueOf(assetSearch.assetId()));
    searchResult.setPath(assetSearch.path());
    searchResult.setRepository(assetSearch.repositoryName());
    searchResult.setFormat(format);
    searchResult.setLastModified(Date.from(assetSearch.lastUpdated().toInstant()));
    searchResult.setAttributes(assetSearch.attributes().backing());
    searchResult.setContentType(assetSearch.contentType());
    searchResult.setChecksum(assetSearch.checksums());

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

    // in case of search formats are different - we can't recognize where to search and returns an empty result.
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

  private static class ComponentSearchResultPage {
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
