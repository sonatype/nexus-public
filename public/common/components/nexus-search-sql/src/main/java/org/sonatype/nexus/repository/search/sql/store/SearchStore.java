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
package org.sonatype.nexus.repository.search.sql.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SearchAssetRecord;
import org.sonatype.nexus.repository.search.sql.SearchResult;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryConditionGroup;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchRequest;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Store for the single table search implementation.
 */
@Component
public class SearchStore
    extends ConfigStoreSupport<SearchTableDAO>
{
  private final int deleteBatchSize;

  private final int assetSaveBatchSize;

  @Autowired
  public SearchStore(
      final DataSessionSupplier sessionSupplier,
      @Value("${nexus.content.deleteBatchSize:1000}") final int deleteBatchSize,
      @Value("${nexus.rebuild.search.assetBatchSize:1500}") final int assetSaveBatchSize)
  {
    super(sessionSupplier, SearchTableDAO.class);
    this.deleteBatchSize = deleteBatchSize;
    this.assetSaveBatchSize = assetSaveBatchSize;
  }

  /**
   * Saves the given search entry in the content data store.
   *
   * @param data the search row to save.
   */
  @Transactional
  public void save(final SearchRecordData data) {
    log.debug("Saving {}", data);
    dao().save(data);
  }

  /**
   * Delete the given search entry in the content data store.
   *
   * @param repositoryId the content repository identification
   * @param componentId the component identification
   * @param format the repository format
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
   *
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
   * Delete the asset records for the specified repository, format and component ids.
   *
   * @param repositoryId the content repository id
   * @param componentIds the component ids to delete
   * @param format the format
   */
  @Transactional
  public void deleteSearchAssets(
      @Nonnull @Param("repositoryId") final Integer repositoryId,
      @Nonnull @Param("componentIds") final Set<Integer> componentIds,
      @Nonnull @Param("format") final String format)
  {
    dao().deleteSearchAssets(repositoryId, componentIds, format);
  }

  /**
   * Delete all search entries for given repository.
   *
   * @param repositoryId the content repository identification
   * @param format the repository format
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
   * Delete all asset search entries for given repository.
   *
   * @param repositoryId the content repository identification
   * @param format the repository format
   * @return {@code true} if all records were deleted
   */
  @Transactional
  public boolean deleteAllSearchAssets(final Integer repositoryId, final String format) {
    boolean deleted = false;
    while (dao().deleteAllSearchAssets(repositoryId, format, deleteBatchSize)) {
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
      @Nullable final SqlSearchQueryConditionGroup queryConditionGroup,
      @Nullable final String sortColumnName,
      final SortDirection sortDirection,
      final Boolean distinct)
  {
    SqlSearchRequest request = prepareSearchRequest(limit, offset, queryConditionGroup, sortColumnName, sortDirection,
        distinct);
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
  public long count(@Nullable final SqlSearchQueryConditionGroup queryConditionGroup) {
    SqlSearchRequest request = prepareSearchRequest(queryConditionGroup);
    log.debug("Search request - filters: {}, filter values: {}", request.filter, request.filterParams);

    return dao().count(request);
  }

  /**
   * Batch Insert data.
   *
   * @param searchData data to be saved.
   */
  @Transactional
  public void saveBatch(final List<SearchRecordData> searchData) {
    log.trace("Saving {} records into the search table", searchData.size());
    dao().saveBatch(searchData);
  }

  /**
   * Batch Insert assets data.
   * <p>
   * Splits large collections to avoid PostgreSQL parameter limit.
   * Configurable via {@code nexus.rebuild.search.assetBatchSize} property.
   * Uses a lazy view over the original collection to avoid memory overhead.
   *
   * @param assetRecords data to be saved.
   */
  @Transactional
  public void saveAssets(final Collection<SearchAssetRecord> assetRecords) {
    log.trace("Saving {} records into the search asset records table", assetRecords.size());

    if (assetRecords.size() <= assetSaveBatchSize) {
      dao().saveAssets(assetRecords);
    }
    else {
      int totalBatches = (assetRecords.size() + assetSaveBatchSize - 1) / assetSaveBatchSize;
      log.info("Splitting {} assets into {} batches of {}",
          assetRecords.size(), totalBatches, assetSaveBatchSize);

      int batchNum = 0;
      for (List<SearchAssetRecord> batch : Iterables.partition(assetRecords, assetSaveBatchSize)) {
        batchNum++;
        log.debug("Saving asset batch {}/{}: {} records", batchNum, totalBatches, batch.size());
        dao().saveAssets(batch);
      }
    }
  }

  /**
   * Saves the asset records if that asset still exists.
   *
   * @param assetRecord data to be saved.
   */
  @Transactional
  public void saveAsset(final SearchAssetRecord assetRecord) {
    log.trace("Saving {} records into the search asset records table", assetRecord);
    dao().saveAsset(assetRecord);
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
   * Get distinct repositories from search tables
   *
   * @return a list of unique repositories wrapped in {@link SearchRepositoryData}
   */
  @Transactional
  public List<SearchRepositoryData> getSearchRepositories() {
    return dao().getSearchRepositories();
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

  private SqlSearchQueryCondition getComponentFilterQuery(SqlSearchQueryConditionGroup sqlSearchQueryConditionGroup) {
    return Objects.nonNull(sqlSearchQueryConditionGroup) ? sqlSearchQueryConditionGroup.getComponentCondition() : null;
  }

  private Optional<SqlSearchQueryCondition> getAssetFilterQuery(
      SqlSearchQueryConditionGroup sqlSearchQueryConditionGroup)
  {
    return Objects.nonNull(
        sqlSearchQueryConditionGroup) ? sqlSearchQueryConditionGroup.getAssetCondition() : Optional.empty();
  }

  private String getFilterFormat(SqlSearchQueryCondition componentFilterQuery) {
    return Objects.nonNull(componentFilterQuery) ? componentFilterQuery.getSqlFilter() : null;
  }

  private Map<String, Object> getFormatValues(SqlSearchQueryCondition componentFilterQuery) {
    return Objects.nonNull(componentFilterQuery) ? componentFilterQuery.getParameters() : null;
  }

  private String getSortDirection(SortDirection sortDirectionEnum) {
    return Optional.ofNullable(sortDirectionEnum).orElse(SortDirection.ASC).name();
  }

  private SqlSearchRequest buildSearchRequest(
      int limit,
      int offset,
      String filterFormat,
      Map<String, Object> formatValues,
      Optional<SqlSearchQueryCondition> assetFilterQuery,
      @Nullable final String sortColumnName,
      @Nullable final String sortDirection,
      final Boolean distinct)
  {
    SqlSearchRequest.Builder builder = SqlSearchRequest
        .builder()
        .limit(limit)
        .offset(offset)
        .searchFilter(filterFormat)
        .searchFilterValues(formatValues)
        .searchAssetFilter(assetFilterQuery.map(SqlSearchQueryCondition::getSqlFilter).orElse(null))
        .searchAssetFilterValues(assetFilterQuery.map(SqlSearchQueryCondition::getParameters).orElse(null))
        .distinctNameAndNamespace(distinct);

    if (Objects.nonNull(sortColumnName)) {
      builder = builder
          .sortColumnName(sortColumnName)
          .sortDirection(sortDirection);
    }

    return builder.build();
  }

  private SqlSearchRequest prepareSearchRequest(
      final int limit,
      final int offset,
      final SqlSearchQueryConditionGroup sqlSearchQueryConditionGroup,
      @Nullable final String sortColumnName,
      @Nullable final SortDirection sortDirectionEnum,
      final Boolean distinct)
  {
    SqlSearchQueryCondition componentFilterQuery = getComponentFilterQuery(sqlSearchQueryConditionGroup);
    Optional<SqlSearchQueryCondition> assetFilterQuery = getAssetFilterQuery(sqlSearchQueryConditionGroup);

    String filterFormat = getFilterFormat(componentFilterQuery);
    Map<String, Object> formatValues = getFormatValues(componentFilterQuery);

    String sortDirection = getSortDirection(sortDirectionEnum);

    return buildSearchRequest(limit, offset, filterFormat, formatValues, assetFilterQuery, sortColumnName,
        sortDirection, distinct);
  }

  private SqlSearchRequest prepareSearchRequest(final SqlSearchQueryConditionGroup sqlSearchQueryConditionGroup) {
    SqlSearchQueryCondition componentFilterQuery = getComponentFilterQuery(sqlSearchQueryConditionGroup);
    Optional<SqlSearchQueryCondition> assetFilterQuery = getAssetFilterQuery(sqlSearchQueryConditionGroup);

    String filterFormat = getFilterFormat(componentFilterQuery);
    Map<String, Object> formatValues = getFormatValues(componentFilterQuery);

    return buildSearchRequest(filterFormat, formatValues, assetFilterQuery);
  }

  private SqlSearchRequest buildSearchRequest(
      String filterFormat,
      Map<String, Object> formatValues,
      Optional<SqlSearchQueryCondition> assetFilterQuery)
  {
    SqlSearchRequest.Builder builder = SqlSearchRequest
        .builder()
        .searchFilter(filterFormat)
        .searchFilterValues(formatValues)
        .searchAssetFilter(assetFilterQuery.map(SqlSearchQueryCondition::getSqlFilter).orElse(null))
        .searchAssetFilterValues(assetFilterQuery.map(SqlSearchQueryCondition::getParameters).orElse(null));

    return builder.build();
  }
}
