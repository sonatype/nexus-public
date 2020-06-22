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
package org.sonatype.nexus.coreui

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.ValidationException

import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.coreui.events.UiSearchEvent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.LimitedPagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.rapture.UiSettingsManager
import org.sonatype.nexus.repository.search.query.SearchFilter
import org.sonatype.nexus.repository.search.query.SearchQueryService
import org.sonatype.nexus.repository.search.query.SearchResultComponent
import org.sonatype.nexus.repository.search.query.SearchResultsGenerator
import org.sonatype.nexus.repository.search.query.SearchUtils

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.QueryBuilder

import static java.time.Duration.ofSeconds
import static org.sonatype.nexus.repository.search.query.RepositoryQueryBuilder.repositoryQuery

/**
 * Search {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'coreui_Search')
class SearchComponent
    extends DirectComponentSupport
{
  @Inject
  SearchQueryService searchQueryService

  @Inject
  SearchUtils searchUtils

  @Inject
  @Named('${nexus.searchResultsLimit:-1000}')
  long searchResultsLimit

  @Inject
  SearchResultsGenerator searchResultsGenerator

  @Inject
  UiSettingsManager uiSettingsManager

  @Inject
  EventManager eventManager;

  /**
   * Search based on configured filters.
   *
   * @param parameters store parameters
   * @return search results
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:search:read')
  LimitedPagedResponse<ComponentXO> read(StoreLoadParameters parameters) {
    eventManager.post(new UiSearchEvent())
    if (parameters.limit > searchResultsLimit) {
      parameters.limit = searchResultsLimit
    }

    Collection<SearchFilter> searchFilters = parameters.filters.collect {
      new SearchFilter(it.property, it.value)
    }
    QueryBuilder query = searchUtils.buildQuery(searchFilters)

    if (!query) {
      return null
    }
    else {
      log.debug("UI Search Query {}", query)
    }

    try {
      def sort = parameters?.sort?.get(0)
      def sortBuilders = searchUtils.getSortBuilders(sort?.property, sort?.direction)
      def timeout = uiSettingsManager.settings.searchRequestTimeout ?: uiSettingsManager.settings.requestTimeout - 5

      SearchResponse response = searchQueryService.search(
        repositoryQuery(query).sortBy(sortBuilders).timeout(ofSeconds(timeout)), parameters.start, parameters.limit)

      List<SearchResultComponent> searchResultComponents = searchResultsGenerator.getSearchResultList(response)

      return new LimitedPagedResponse<ComponentXO>(
          Math.min(parameters.limit, searchResultComponents.size()),
          (parameters.limit < response.hits.totalHits()) ? response.hits.totalHits() : searchResultComponents.size(),
          searchResultComponents.collect { searchResultComponent ->
            new ComponentXO(
                id: searchResultComponent.id,
                repositoryName: searchResultComponent.repositoryName,
                group: searchResultComponent.group,
                name: searchResultComponent.name,
                version: searchResultComponent.version,
                format: searchResultComponent.format
            )
          },
          response.isTimedOut()
      )
    }
    catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage())
    }
  }
}
