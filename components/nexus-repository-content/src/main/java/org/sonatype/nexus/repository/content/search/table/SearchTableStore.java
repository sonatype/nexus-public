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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchResultData;
import org.sonatype.nexus.repository.content.search.SearchViewColumns;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;
import org.sonatype.nexus.repository.search.SortDirection;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.ibatis.annotations.Param;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * Store for the single table search implementation.
 */
@Named
@Singleton
public class SearchTableStore
    extends ConfigStoreSupport<SearchTableDAO>
{
  private final List<Format> formats;

  private final int deleteBatchSize;

  @Inject
  public SearchTableStore(
      final List<Format> formats,
      final DataSessionSupplier sessionSupplier,
      @Named("${nexus.content.deleteBatchSize:-1000}") final int deleteBatchSize)
  {
    super(sessionSupplier, SearchTableDAO.class);
    this.formats = checkNotNull(formats);
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
      final SearchViewColumns sortColumnName,
      final SortDirection sortDirection)
  {
    try {
      SqlSearchRequest request = prepareSearchRequest(limit, offset, filterQuery, sortColumnName, sortDirection);
      return dao().searchComponents(request);
    }
    catch (NoTaggedComponentsException e) {
      return new ArrayList<>();
    }
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
      final SearchViewColumns sortViewColumnName,
      final SortDirection sortDirectionEnum) throws NoTaggedComponentsException
  {
    boolean crossFormatSearch = false;
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
      crossFormatSearch = formatValues.containsKey("name") && !formatValues.containsKey("format");
    }

    final String sortColumnName = sortViewColumnName.name();
    final String sortDirection = Optional.ofNullable(sortDirectionEnum).orElse(SortDirection.ASC).name();
    SqlSearchRequest request;
    if (crossFormatSearch) {
      //Cross format search request
      final String tagName = formatValues.get("name");
      final List<String> formats = this.formats.stream().map(Format::getValue).collect(Collectors.toList());
      Collection<SearchResult> taggedComponents = dao().findComponentIdsByTag(tagName, formats);
      if (taggedComponents.size() == 0) {
        throw new NoTaggedComponentsException("There are no components marked with specified tag name");
      }
      Map<String, List<Integer>> taggedComponentIds = new HashMap<>();
      for (String format : formats) {
        List<Integer> componentIds = taggedComponents.stream()
            .filter(taggedComponent -> Objects.equals(format, taggedComponent.format()))
            .map(SearchResult::componentId)
            .collect(Collectors.toList());
        if (componentIds.size() > 0) {
          taggedComponentIds.put(format, componentIds);
        }
      }

      request = SqlSearchRequest
          .builder()
          .limit(limit)
          .offset(offset)
          .tagToComponentIds(taggedComponentIds)
          .sortColumnName(sortColumnName)
          .sortDirection(sortDirection)
          .build();
    }
    else {
      //Regular search request
      request = SqlSearchRequest
          .builder()
          .limit(limit)
          .offset(offset)
          .searchFilter(filterFormat)
          .searchFilterValues(formatValues)
          .sortColumnName(sortColumnName)
          .sortDirection(sortDirection)
          .build();
    }
    return request;
  }
}
