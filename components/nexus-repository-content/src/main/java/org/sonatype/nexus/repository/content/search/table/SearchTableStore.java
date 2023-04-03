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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchResultData;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.ibatis.annotations.Param;

import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Store for the single table search implementation.
 */
@Named
@Singleton
public class SearchTableStore
    extends ConfigStoreSupport<SearchTableDAO>
{
  private final int deleteBatchSize;

  @Inject
  public SearchTableStore(
      final DataSessionSupplier sessionSupplier,
      @Named("${nexus.content.deleteBatchSize:-1000}") final int deleteBatchSize)
  {
    super(sessionSupplier, SearchTableDAO.class);
    this.deleteBatchSize = deleteBatchSize;
  }

  /**
   * Saves the given search entry in the content data store.
   *
   * @param data the search row to save.
   */
  @Transactional
  public void save(final SearchTableData data) {
    log.debug("Saving {}", data);
    dao().save(data);
  }

  /**
   * Delete the given search entry in the content data store.
   *
   * @param repositoryId the content repository identification
   * @param componentId  the component identification
   * @param format       the repository format
   */
  @Transactional
  public void delete(
      @Nonnull @Param("repositoryId") final Integer repositoryId,
      @Nonnull @Param("componentId") final Integer componentId,
      @Nonnull @Param("format") final String format)
  {
    dao().delete(repositoryId, componentId, format);
  }

  /**
   * Delete records for the specified repository, format and component ids.
   * @param repositoryId the content repository id
   * @param componentIds the component ids to delete
   * @param format the format
   */
  @Transactional
  public void deleteComponentIds(
      @Nonnull @Param("repositoryId") final Integer repositoryId,
      @Nonnull @Param("componentIds") final Set<Integer> componentIds,
      @Nonnull @Param("format") final String format)
  {
    dao().deleteComponentIds(repositoryId, componentIds, format);
  }

  /**
   * Delete all search entries for given repository.
   *
   * @param repositoryId the content repository identification
   * @param format       the repository format
   * @return {@code true} if all records were deleted
   */
  @Transactional
  public boolean deleteAllForRepository(final Integer repositoryId, final String format) {
    boolean deleted = false;
    while (dao().deleteAllForRepository(repositoryId, format, deleteBatchSize)) {
      commitChangesSoFar();
      deleted = true;
    }
    return deleted;
  }

  /**
   * Search for components using the given {@link SqlSearchQueryCondition}
   */
  @Transactional
  public Collection<SearchResult> searchComponents(
      final int limit,
      final int offset,
      @Nullable final SqlSearchQueryCondition filterQuery,
      @Nullable final String sortColumnName,
      final SortDirection sortDirection)
  {
    SqlSearchRequest request = prepareSearchRequest(limit, offset, filterQuery, sortColumnName, sortDirection);
    log.debug("Search request - filters: {}, filter values: {}, limit: {}, offset: {}, sort column: {}, " +
            "sort direction: {}", request.filter, request.filterParams, request.limit, request.offset,
        request.sortColumnName, request.sortDirection);
    return dao().searchComponents(request);
  }

  /**
   * Count all {@link SearchResultData} in the given format.
   *
   * @return count of all {@link SearchResultData} in the given format
   */
  @Transactional
  public long count(@Nullable final SqlSearchQueryCondition filterQuery)
  {
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
    }
    log.debug("Search request - filters: {}, filter values: {}", filterFormat, formatValues);
    return dao().count(filterFormat, formatValues);
  }

  /**
   * Batch Insert data.
   *
   * @param searchData data to be saved.
   */
  @Transactional
  public void saveBatch(final List<SearchTableData> searchData) {
    log.trace("Saving {} records into the search table", searchData.size());
    dao().saveBatch(searchData);
  }

  /**
   * Check if repository search index should be re-indexed.
   *
   * @param repositoryName repository id
   * @return check result
   */
  @Transactional
  public boolean repositoryNeedsReindex(final String repositoryName) {
    return !dao().hasRepositoryEntries(repositoryName);
  }

  /**
   * Commits any batched changes so far.
   * <p>
   * Also checks to see if the current (potentially long-running) operation has been cancelled.
   */
  private void commitChangesSoFar() {
    Transaction tx = UnitOfWork.currentTx();
    tx.commit();
    tx.begin();
    checkCancellation();
  }

  private SqlSearchRequest prepareSearchRequest(
      final int limit,
      final int offset,
      final SqlSearchQueryCondition filterQuery,
      @Nullable final String sortColumnName,
      final SortDirection sortDirectionEnum)
  {
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
    }
    final String sortDirection = Optional.ofNullable(sortDirectionEnum).orElse(SortDirection.ASC).name();

    //Regular search request
    return SqlSearchRequest
        .builder()
        .limit(limit)
        .offset(offset)
        .searchFilter(filterFormat)
        .searchFilterValues(formatValues)
        .sortColumnName(sortColumnName)
        .sortDirection(sortDirection)
        .build();
  }
}
