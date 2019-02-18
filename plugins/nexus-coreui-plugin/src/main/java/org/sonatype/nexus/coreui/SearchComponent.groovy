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

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.LimitedPagedResponse
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.rest.SearchUtils
import org.sonatype.nexus.repository.search.SearchFilter
import org.sonatype.nexus.repository.search.SearchResultComponent
import org.sonatype.nexus.repository.search.SearchResultsGenerator
import org.sonatype.nexus.repository.search.SearchService

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.QueryBuilder

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
  SearchService searchService

  @Inject
  SearchUtils searchUtils

  @Inject
  @Named('${nexus.searchResultsLimit:-1000}')
  long searchResultsLimit

  @Inject
  SearchResultsGenerator searchResultsGenerator

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
  PagedResponse<ComponentXO> read(StoreLoadParameters parameters) {
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
      log.debug("UI Search Query {}", query);
    }

    try {
      def sort = parameters?.sort?.get(0)

      SearchResponse response = searchService.search(query,
          searchUtils.getSortBuilders(sort?.property, sort?.direction), parameters.start, parameters.limit)
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
          }
      )
    }
    catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage())
    }
  }
}
