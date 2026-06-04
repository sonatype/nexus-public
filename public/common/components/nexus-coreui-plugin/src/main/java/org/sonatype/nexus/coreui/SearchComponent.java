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
package org.sonatype.nexus.coreui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ValidationException;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.extdirect.model.LimitedPagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.SearchResponse;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.event.SearchEvent;
import org.sonatype.nexus.repository.search.event.SearchEventSource;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.query.SearchResultsGenerator;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;

/**
 * Search {@link DirectComponent}.
 *
 * @since 3.0
 */
@Component
@DirectAction(action = "coreui_Search")
public class SearchComponent
    extends DirectComponentSupport
{
  private static final String REPOSITORY_NAME_FILTER = "repository_name";

  private final SearchService searchService;

  private final EventManager eventManager;

  private final SearchResultsGenerator searchResultsGenerator;

  private final RepositoryManager repositoryManager;

  private int searchResultsLimit;

  @Autowired
  public SearchComponent(
      final SearchService searchService,
      @Value("${nexus.searchResultsLimit:1000}") final int searchResultsLimit,
      final SearchResultsGenerator searchResultsGenerator,
      final EventManager eventManager,
      final RepositoryManager repositoryManager)
  {
    this.searchService = checkNotNull(searchService);
    this.searchResultsLimit = searchResultsLimit;
    this.searchResultsGenerator = checkNotNull(searchResultsGenerator);
    this.eventManager = checkNotNull(eventManager);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  /**
   * Search based on configured filters.
   *
   * @param parameters store parameters
   * @return search results
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions("nexus:search:read")
  public LimitedPagedResponse<ComponentXO> read(final StoreLoadParameters parameters) {
    List<SearchFilter> searchFilters = createFilters(parameters.getFilters());

    boolean formatCriteriaOnly = searchFilters.size() == 1 && FORMAT.equals(searchFilters.get(0).getProperty());
    if (searchFilters.size() == 0 || parameters.isFormatSearch() && formatCriteriaOnly) {
      /*
       * Format specific and All format search UI should not perform a search query
       * unless a user specifies a search criteria.
       */
      return new LimitedPagedResponse<>(parameters.getLimit(), 0L, emptyList());
    }
    else if (formatCriteriaOnly) {
      // All format search UI should not perform a search query with only format search criteria
      throw new ValidationErrorsException("Specify at least one more search criteria with the format");
    }

    fireSearchEvent(searchFilters);

    try {
      return componentSearch(parameters.getLimit(), parameters.getPage(), orEmpty(parameters.getSort()),
          searchFilters);
    }
    catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }
  }

  private static List<SearchFilter> createFilters(final List<Filter> filters) {
    return Optional.ofNullable(filters)
        .map(List::stream)
        .orElseGet(Stream::empty)
        .map(filter -> new SearchFilter(filter.getProperty(),
            stripSurroundingQuotes(filter.getProperty(), filter.getValue())))
        .collect(Collectors.toList());
  }

  private static String stripSurroundingQuotes(final String property, final String value) {
    if (property.equalsIgnoreCase("keyword") && value != null && value.length() >= 2 && value.startsWith("\"")
        && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  private LimitedPagedResponse<ComponentXO> componentSearch(
      final Integer limit,
      final Integer page,
      final List<Sort> sort,
      final List<SearchFilter> filters)
  {
    String sortField = sort.stream().findFirst().map(Sort::getProperty).orElse(null);
    String sortDirection = sort.stream().findFirst().map(Sort::getDirection).orElse(null);

    int queryLimit = Math.min(limit, searchResultsLimit);
    int offset = Optional.ofNullable(page)
        .map(p -> p - 1) // the UI is 1 indexed, not 0 indexed
        .map(p -> p * queryLimit)
        .orElse(0);

    SearchRequest request = SearchRequest.builder()
        .searchFilters(filters)
        .sortField(sortField)
        .limit(queryLimit)
        .offset(offset)
        .sortDirection(Optional.ofNullable(sortDirection)
            .map(String::toUpperCase)
            .map(SortDirection::valueOf)
            .orElse(null))
        .build();

    log.debug("UI Search Query {}", request);

    SearchResponse response = searchService.search(request);

    if (filters.stream().noneMatch(filter -> filter.getProperty().equals(REPOSITORY_NAME_FILTER))) {
      // No repository_name filter - skip repository-specific mapping
      List<ComponentXO> componentXOs = searchResultsGenerator.getSearchResultList(response)
          .stream()
          .map(componentHit -> toComponent(componentHit, identityMapper()))
          .toList();

      return new LimitedPagedResponse<>(limit, response.getTotalHits(), componentXOs, false);
    }

    UnaryOperator<String> repositoryNameMapper = tryExtractRepositoryFromSearch(filters);

    List<ComponentXO> componentXOs = searchResultsGenerator.getSearchResultList(response)
        .stream()
        .map(componentHit -> toComponent(componentHit, repositoryNameMapper))
        .toList();

    return new LimitedPagedResponse<>(limit, response.getTotalHits(), componentXOs, false);
  }

  @VisibleForTesting
  public int getSearchResultsLimit() {
    return searchResultsLimit;
  }

  @VisibleForTesting
  public void setSearchResultsLimit(final int searchResultsLimit) {
    this.searchResultsLimit = searchResultsLimit;
  }

  private static ComponentXO toComponent(
      final ComponentSearchResult componentHit,
      final UnaryOperator<String> repositoryNameMapper)
  {
    ComponentXO componentXO = new ComponentXO();

    componentXO.setGroup(componentHit.getGroup());
    componentXO.setName(componentHit.getName());
    componentXO.setVersion(componentHit.getVersion());
    componentXO.setId(componentHit.getId());
    componentXO.setRepositoryName(repositoryNameMapper.apply(componentHit.getRepositoryName()));
    componentXO.setFormat(componentHit.getFormat());

    if (componentHit.getLastModified() != null) {
      componentXO.setLastBlobUpdated(componentHit.getLastModified().toString());
    }

    return componentXO;
  }

  private UnaryOperator<String> identityMapper() {
    return repositoryName -> repositoryName;
  }

  private void fireSearchEvent(final Collection<SearchFilter> searchFilters) {
    eventManager.post(new SearchEvent(searchFilters, SearchEventSource.UI));
  }

  private static List<Sort> orEmpty(final List<Sort> sort) {
    return sort != null ? sort : emptyList();
  }

  private UnaryOperator<String> createRepositoryNameMapper(final List<SearchFilter> searchFilters) {
    List<Repository> repositories = searchFilters
        .stream()
        .filter(filter -> filter.getProperty().equals(REPOSITORY_NAME_FILTER))
        .findFirst()
        .map(SearchFilter::getValue)
        .stream()
        .flatMap(filter -> Stream.of(filter.split(" ")))
        .map(String::trim)
        .filter(repositoryName -> !repositoryName.isEmpty())
        .map(repositoryManager::get)
        .filter(Objects::nonNull)
        .toList();

    Map<String, String> nameMap = repositories.stream()
        .collect(Collectors.toMap(
            Repository::getName,
            Repository::getName,
            (a, b) -> a,
            HashMap::new));

    // If any of the specified repositories are groups then add leaf members missing from the map referencing the group
    for (Repository repository : repositories) {
      repository.optionalFacet(GroupFacet.class)
          .ifPresent(groupFacet -> groupFacet.leafMembers()
              .forEach(member -> nameMap.putIfAbsent(member.getName(), repository.getName())));
    }
    return repositoryName -> nameMap.getOrDefault(repositoryName, repositoryName);
  }

  @VisibleForTesting
  UnaryOperator<String> tryExtractRepositoryFromSearch(final List<SearchFilter> searchFilters) {
    return createRepositoryNameMapper(searchFilters);
  }
}
