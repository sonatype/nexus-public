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
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchResultData;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;

import org.apache.ibatis.annotations.Param;

/**
 * DAO for access search table entries
 */
public interface SearchTableDAO
    extends ContentDataAccess
{
  /**
   * Count components in the given format.
   *
   * @param filter optional filter to apply
   * @param values optional values map for filter (required if filter is not null)
   * @return number of found components.
   */
  long count(@Nullable @Param("filter") String filter, @Nullable @Param("filterParams") Map<String, String> values);

  /**
   * Search components in the scope of one format.
   *
   * @param request DTO containing all required params for search
   * @return collection of {@link SearchResultData} representing search results for a given format.
   */
  Collection<SearchResult> searchComponents(SqlSearchRequest request);

  /**
   * Saves the given search entry in the content data store by performing an upsert.
   *
   * @param searchTableData the search row to create
   */
  void save(SearchTableData searchTableData);

  /**
   * Delete the given search entry in the content data store.
   *
   * @param repositoryId the content repository identification
   * @param componentId  the component identification
   * @param format       the repository format
   */
  void delete(
      @Param("repositoryId") Integer repositoryId,
      @Param("componentId") Integer componentId,
      @Param("format") String format);

  /**
   * Delete records for the specified repository, format and component ids.
   *
   * @param repositoryId the content repository id
   * @param componentIds the component ids to delete
   * @param format       the format
   */
  void deleteComponentIds(
      @Param("repositoryId") Integer repositoryId,
      @Param("componentIds") Set<Integer> componentIds,
      @Param("format") String format);

  /**
   * Delete all search entries for given repository.
   *
   * @param repositoryId the content repository identification
   * @param format       the repository format
   * @param limit        when positive limits the number of entries deleted per-call
   * @return {@code true} if any record was deleted
   */
  boolean deleteAllForRepository(
      @Param("repositoryId") Integer repositoryId,
      @Param("format") String format,
      @Param("limit") int limit);

  /**
   * Batch Insert data.
   *
   * @param searchData data to be saved.
   */
  void saveBatch(@Param("searchData") List<SearchTableData> searchData);

  /**
   * Check repository has search entries.
   *
   * @param repositoryName repository
   * @return {@code true} if any records exists
   */
  boolean hasRepositoryEntries(@Param("repositoryName") final String repositoryName);
}
