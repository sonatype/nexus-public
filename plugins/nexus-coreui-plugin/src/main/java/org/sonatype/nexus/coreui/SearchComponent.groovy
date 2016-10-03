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

import org.sonatype.nexus.coreui.search.SearchContribution
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.LimitedPagedResponse
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.search.SearchService

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder

import static org.elasticsearch.search.sort.SortBuilders.fieldSort
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.FORMAT
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.VERSION

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
  Map<String, SearchContribution> searchContributions

  @Inject
  @Named('${nexus.searchResultsLimit:-1000}')
  long searchResultsLimit

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
    QueryBuilder query = buildQuery(parameters)
    if (!query) {
      return null
    }

    try {
      def sort = parameters?.sort?.get(0)
      def sortBuilders = []
      if (sort) {
        switch (sort.property) {
          case GROUP:
            sortBuilders << fieldSort("${GROUP}.case_insensitive").order(SortOrder.valueOf(sort.direction))
            sortBuilders << fieldSort("${NAME}.case_insensitive").order(SortOrder.ASC)
            sortBuilders << fieldSort(VERSION).order(SortOrder.ASC)
            break
          case NAME:
            sortBuilders << fieldSort("${NAME}.case_insensitive").order(SortOrder.valueOf(sort.direction))
            sortBuilders << fieldSort(VERSION).order(SortOrder.ASC)
            sortBuilders << fieldSort("${GROUP}.case_insensitive").order(SortOrder.ASC)
            break
          case 'repositoryName':
            sortBuilders = [fieldSort(REPOSITORY_NAME).order(SortOrder.valueOf(sort.direction))]
            break
          default:
            sortBuilders = [fieldSort(sort.property).order(SortOrder.valueOf(sort.direction))]
        }
      }
      SearchResponse response = searchService.search(query, sortBuilders, parameters.start, parameters.limit)
      return new LimitedPagedResponse<ComponentXO>(
          searchResultsLimit,
          response.hits.totalHits,
          response.hits.hits?.collect { hit ->
            return new ComponentXO(
                id: hit.id,
                repositoryName: hit.source[REPOSITORY_NAME],
                group: hit.source[GROUP],
                name: hit.source[NAME],
                version: hit.source[VERSION],
                format: hit.source[FORMAT]
            )
          }
      )
    }
    catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage())
    }
  }

  /**
   * Builds a QueryBuilder based on configured filters.
   *
   * @param parameters store parameters
   */
  private QueryBuilder buildQuery(final StoreLoadParameters parameters) {
    BoolQueryBuilder query = QueryBuilders.boolQuery()
    parameters.filters?.each { filter ->
      SearchContribution contribution = searchContributions[filter.property]
      if (!contribution) {
        contribution = searchContributions['default']
      }
      contribution.contribute(query, filter.property, filter.value)
    }
    if (!query.hasClauses()) {
      return null;
    }
    log.debug('Query: {}', query)
    return query
  }
}
