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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchResultData;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.transaction.Transactional;

/**
 * Store for the single table search implementation.
 */
@Named
@Singleton
public class SearchTableStore
    extends ConfigStoreSupport<SearchTableDAO>
{
  @Inject
  public SearchTableStore(
      final DataSessionSupplier sessionSupplier)
  {
    super(sessionSupplier, SearchTableDAO.class);
  }

  /**
   * Search for components using the given {@link SqlSearchQueryCondition}
   */
  @Transactional
  public Collection<SearchResult> searchComponents(
      final int limit,
      final int offset,
      @Nullable final SqlSearchQueryCondition filterQuery,
      final SearchViewColumns sortColumnName,
      final SortDirection sortDirection)
  {
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
    }

    String direction = Optional.ofNullable(sortDirection).orElse(SortDirection.ASC).name();

    SqlSearchRequest request = SqlSearchRequest
        .builder()
        .limit(limit)
        .offset(offset)
        .searchFilter(filterFormat)
        .searchFilterValues(formatValues)
        .sortColumnName(sortColumnName.name())
        .sortDirection(direction)
        .defaultSortColumnName(SearchViewColumns.COMPONENT_ID.name())
        .build();

    return dao().searchComponents(request);
  }

  /**
   * Count all {@link SearchResultData} in the given format.
   *
   * @return count of all {@link SearchResultData} in the given format
   */
  @Transactional
  public int count(@Nullable final SqlSearchQueryCondition filterQuery)
  {
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
    }
    return dao().count(filterFormat, formatValues);
  }
}
