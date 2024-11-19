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
package org.sonatype.nexus.repository.search.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.search.BlankValueSearchQueryFilter;
import org.sonatype.nexus.repository.search.SearchRequest;

import com.google.common.annotations.VisibleForTesting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.NORMALIZED_VERSION;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.search.index.SearchConstants.VERSION;

/**
 * @since 3.7
 */
@Named
@Singleton
public class ElasticSearchUtils
    extends ComponentSupport
{
  public static final String CONTINUATION_TOKEN = "continuationToken";

  public static final String SORT_FIELD = "sort";

  public static final String SORT_DIRECTION = "direction";

  private static final String ASSET_PREFIX = "assets.";

  private static final String CI_SUFFIX = ".case_insensitive";

  private final RepositoryManagerRESTAdapter repoAdapter;

  private final Map<String, String> searchParams = new HashMap<>();

  private final Map<String, String> assetSearchParams;

  private final Map<String, ElasticSearchContribution> searchContributions;

  private final Map<String, BlankValueSearchQueryFilter> filterAttributes;

  private final ElasticSearchContribution defaultElasticSearchContribution;

  private final ElasticSearchContribution blankValueElasticSearchContribution;

  @Inject
  public ElasticSearchUtils(
      final RepositoryManagerRESTAdapter repoAdapter,
      final Map<String, SearchMappings> searchMappings,
      final Map<String, ElasticSearchContribution> searchContributions,
      final Map<String, BlankValueSearchQueryFilter> filterAttributes)
  {
    this.repoAdapter = checkNotNull(repoAdapter);
    checkNotNull(searchMappings).entrySet()
        .stream()
        .flatMap(e -> stream(e.getValue().get().spliterator(), true))
        .forEach(mapping -> {
          searchParams.put(mapping.getAlias(), mapping.getAttribute());
          searchParams.put(mapping.getAttribute(), mapping.getAttribute());
        });
    this.assetSearchParams = searchParams.entrySet()
        .stream()
        .filter(e -> e.getValue().startsWith(ASSET_PREFIX))
        .collect(toMap(Entry::getKey, Entry::getValue));
    this.searchContributions = checkNotNull(searchContributions);
    this.filterAttributes = checkNotNull(filterAttributes);
    this.defaultElasticSearchContribution = checkNotNull(searchContributions.get(
        DefaultElasticSearchContribution.NAME));
    this.blankValueElasticSearchContribution = checkNotNull(searchContributions.get(
        BlankValueElasticSearchContribution.NAME));
  }

  private Map<String, String> getAssetSearchParameters() {
    return assetSearchParams;
  }

  public Repository getRepository(final String repository) {
    return repoAdapter.getRepository(repository);
  }

  public Repository getReadableRepository(final String repository) {
    return repoAdapter.getReadableRepository(repository);
  }

  /**
   * Constructs a query from the provided search filters using conjunction (AND)
   */
  public QueryBuilder buildQuery(final Collection<SearchFilter> searchFilters) {
    return buildQuery(convertFilters(searchFilters), true);
  }

  /**
   * @since 3.15
   * @param searchFilters the filters for the query
   * @param conjunction indicates the query should use conjunection (AND) rather than disjunction (OR)
   * @return a {@link QueryBuilder} for the provided search filters, the query will be generated by the
   *         {@link ElasticSearchContribution}s
   */
  public QueryBuilder buildQuery(final Collection<SearchFilter> searchFilters, final boolean conjunction) {
    BoolQueryBuilder query = QueryBuilders.boolQuery();
    Consumer<QueryBuilder> contribute = conjunction ? query::must : query::should;
    searchFilters.stream()
        .filter(searchFilter -> !isBlank(searchFilter.getValue()))
        .sorted(comparing(SearchFilter::getProperty).thenComparing(SearchFilter::getValue))
        .forEach(searchFilter -> {
          ElasticSearchContribution elasticSearchContribution = searchContributions
              .getOrDefault(searchFilter.getProperty(), defaultElasticSearchContribution);
          elasticSearchContribution.contribute(contribute, searchFilter.getProperty(), searchFilter.getValue());
        });

    handleBlankValueFilters(contribute, searchFilters);

    log.debug("Query: {}", query);

    return query;
  }

  private void handleBlankValueFilters(
      final Consumer<QueryBuilder> contribute,
      final Collection<SearchFilter> searchFilters)
  {
    searchFilters.stream()
        .filter(this::filter)
        .forEach(filter -> blankValueElasticSearchContribution.contribute(contribute, filter.getProperty(),
            filter.getValue()));
  }

  private boolean filter(final SearchFilter filter) {
    BlankValueSearchQueryFilter queryFilter = filterAttributes.get(filter.getProperty());
    return queryFilter != null ? queryFilter.shouldHandleBlankValue() : isBlank(filter.getValue());
  }

  /**
   * Builds a {@link QueryBuilder} based on configured search parameters.
   *
   * @param uriInfo {@link UriInfo} to extract query parameters from
   */
  public QueryBuilder buildQuery(final UriInfo uriInfo) {
    Collection<SearchFilter> searchFilters = convertParameters(uriInfo,
        Arrays.asList(CONTINUATION_TOKEN, SORT_FIELD, SORT_DIRECTION));
    return buildQuery(searchFilters);
  }

  public QueryBuilder buildQuery(final UriInfo uriInfo, final List<String> parameters) {
    ArrayList<String> filterParameters = new ArrayList<>();
    filterParameters.add(CONTINUATION_TOKEN);
    filterParameters.add(SORT_FIELD);
    filterParameters.add(SORT_DIRECTION);
    filterParameters.addAll(parameters);
    return buildQuery(convertParameters(uriInfo, filterParameters));
  }

  private Collection<SearchFilter> convertParameters(final UriInfo uriInfo, final List<String> keys) {
    return uriInfo.getQueryParameters()
        .entrySet()
        .stream()
        .filter(entry -> !keys.contains(entry.getKey()))
        .map(entry -> entry.getValue().stream().map(value -> {
          String key = searchParams.getOrDefault(entry.getKey(), entry.getKey());
          return new SearchFilter(key, value);
        }).collect(toSet()))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private Collection<SearchFilter> convertFilters(final Collection<SearchFilter> rawFilters) {
    return rawFilters.stream().map(filter -> {
      String key = searchParams.getOrDefault(filter.getProperty(), filter.getProperty());
      return new SearchFilter(key, filter.getValue());
    }).collect(toSet());
  }

  @VisibleForTesting
  boolean isFullAssetAttributeName(final String assetSearchParam) {
    return assetSearchParam.startsWith(ASSET_PREFIX);
  }

  public String getFullAssetAttributeName(final String key) {
    return isFullAssetAttributeName(key) ? key : getAssetSearchParameters().get(key);
  }

  public List<SortBuilder> getSortBuilders(final String sort, final String direction) {
    return getSortBuilders(sort, direction, true);
  }

  private List<SortBuilder> getSortBuilders(final String sort, final String direction, final boolean allowAnySort) {
    if (sort == null) {
      return emptyList();
    }

    switch (sort) {
      case GROUP:
        return handleGroupSort(direction);
      case NAME:
        return handleNameSort(direction);
      case VERSION:
        return handleVersionSort(direction);
      case "repository":
      case "repositoryName":
        return handleRepositoryNameSort(direction);
      default:
        return handleOtherSort(sort, direction, allowAnySort);
    }
  }

  private List<SortBuilder> handleGroupSort(final String direction) {
    return Arrays.asList(fieldSort(GROUP + CI_SUFFIX).order(getValidSortOrder(direction, SortOrder.ASC)),
        fieldSort(NAME + CI_SUFFIX).order(SortOrder.ASC), fieldSort(VERSION).order(SortOrder.ASC));
  }

  private List<SortBuilder> handleNameSort(final String direction) {
    return Arrays.asList(fieldSort(NAME + CI_SUFFIX).order(getValidSortOrder(direction, SortOrder.ASC)),
        fieldSort(VERSION).order(SortOrder.ASC), fieldSort(GROUP + CI_SUFFIX).order(SortOrder.ASC));
  }

  private List<SortBuilder> handleRepositoryNameSort(final String direction) {
    return singletonList(fieldSort(REPOSITORY_NAME).order(getValidSortOrder(direction, SortOrder.ASC)));
  }

  private List<SortBuilder> handleVersionSort(final String direction) {
    return singletonList(fieldSort(NORMALIZED_VERSION).order(getValidSortOrder(direction, SortOrder.DESC)));
  }

  private List<SortBuilder> handleOtherSort(final String sort, final String direction, final boolean allowed) {
    if (!allowed) {
      return emptyList();
    }
    return singletonList(fieldSort(sort).order(getValidSortOrder(direction, SortOrder.ASC)));
  }

  private SortOrder getValidSortOrder(final String direction, final SortOrder defaultValue) {
    if (direction != null) {
      switch (direction.toLowerCase()) {
        case "asc":
        case "ascending":
          return SortOrder.ASC;
        case "desc":
        case "descending":
          return SortOrder.DESC;
        default:
          break;
      }
    }

    return defaultValue;
  }

  public QueryBuilder buildQuery(final SearchRequest searchRequest) {
    QueryBuilder queryBuilder =
        buildQuery(convertFilters(searchRequest.getSearchFilters()), searchRequest.isConjunction());

    List<SortBuilder> sortBuilders = getSortBuilders(
        searchRequest.getSortField(),
        searchRequest.getSortDirection() != null ? searchRequest.getSortDirection().name() : null);

    RepositoryQueryBuilder repositoryQueryBuilder = RepositoryQueryBuilder
        .repositoryQuery(queryBuilder)
        .sortBy(sortBuilders);

    if (searchRequest.getRepositories() != null && !searchRequest.getRepositories().isEmpty()) {
      repositoryQueryBuilder.inRepositories(searchRequest.getRepositories());
    }

    if (!searchRequest.isCheckAuthorization()) {
      repositoryQueryBuilder.unrestricted();
    }

    return repositoryQueryBuilder;
  }
}
