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

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.ValidationException

import org.sonatype.nexus.coreui.search.SearchContribution
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.repository.search.SearchService

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolFilterBuilder
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.FilterBuilders
import org.elasticsearch.index.query.FilteredQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder

import static org.elasticsearch.search.sort.SortBuilders.fieldSort
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT
import static org.sonatype.nexus.repository.storage.StorageFacet.P_GROUP
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME
import static org.sonatype.nexus.repository.storage.StorageFacet.P_REPOSITORY_NAME
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION

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

  /**
   * Search based on configured filters.
   *
   * @param parameters store parameters
   * @return search results
   */
  @DirectMethod
  @RequiresPermissions('nexus:search:read')
  PagedResponse<ComponentXO> read(final @Nullable StoreLoadParameters parameters) {
    QueryBuilder query = buildQuery(parameters)
    if (!query) {
      return null
    }

    try {
      def sort = parameters?.sort?.get(0)
      def sortBuilders = []
      if (sort) {
        switch (sort.property) {
          case P_GROUP:
            sortBuilders << fieldSort("${P_GROUP}.case_insensitive").order(SortOrder.valueOf(sort.direction))
            sortBuilders << fieldSort("${P_NAME}.case_insensitive").order(SortOrder.ASC)
            sortBuilders << fieldSort(P_VERSION).order(SortOrder.ASC)
            break
          case P_NAME:
            sortBuilders << fieldSort("${P_NAME}.case_insensitive").order(SortOrder.valueOf(sort.direction))
            sortBuilders << fieldSort(P_VERSION).order(SortOrder.ASC)
            sortBuilders << fieldSort("${P_GROUP}.case_insensitive").order(SortOrder.ASC)
            break
          case 'repositoryName':
            sortBuilders = [fieldSort(P_REPOSITORY_NAME).order(SortOrder.valueOf(sort.direction))]
            break
          default:
            sortBuilders = [fieldSort(sort.property).order(SortOrder.valueOf(sort.direction))]
        }
      }
      SearchResponse response = searchService.search(query, sortBuilders, parameters.start, parameters.limit)
      return new PagedResponse<ComponentXO>(
          response.hits.totalHits,
          response.hits.hits?.collect { hit ->
            return new ComponentXO(
                id: hit.id,
                repositoryName: hit.source[P_REPOSITORY_NAME],
                group: hit.source[P_GROUP],
                name: hit.source[P_NAME],
                version: hit.source[P_VERSION],
                format: hit.source[P_FORMAT]
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
    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
    BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter()
    parameters.filters?.each { filter ->
      SearchContribution contribution = searchContributions[filter.property]
      if (!contribution) {
        contribution = searchContributions['default']
      }
      contribution.contribute(queryBuilder, filter.property, filter.value)
      contribution.contribute(filterBuilder, filter.property, filter.value)
    }

    if (!queryBuilder.hasClauses() && !filterBuilder.hasClauses()) {
      return null
    }
    FilteredQueryBuilder query = QueryBuilders.filteredQuery(
        queryBuilder.hasClauses() ? queryBuilder : null,
        filterBuilder.hasClauses() ? filterBuilder : null
    )
    log.debug('Query: {}', query)

    return query
  }

}
