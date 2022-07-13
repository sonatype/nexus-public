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
  int count(@Nullable @Param("filter") String filter, @Nullable @Param("filterParams") Map<String, String> values);

  /**
   * Search components in the scope of one format.
   *
   * @param request DTO containing all required params for search
   * @return collection of {@link SearchResultData} representing search results for a given format.
   */
  Collection<SearchResult> searchComponents(SqlSearchRequest request);

  /**
   * Creates the given search entry in the content data store.
   *
   * @param data the search row to create
   */
  void create(SearchTableData data);

  /**
   * Update a component kind for the search entry in the content data store.
   *
   * @param repositoryId  the content repository identification
   * @param componentId   the component identification
   * @param format        the repository format
   * @param componentKind the new component kind
   */
  void updateKind(
      @Param("repositoryId") Integer repositoryId,
      @Param("componentId") Integer componentId,
      @Param("format") String format,
      @Param("componentKind") String componentKind);

  /**
   * Update custom format fields for a specific record
   *
   * @param repositoryId the content repository identification
   * @param componentId  the component identification
   * @param assetId      the asset identification
   * @param format       the repository format
   * @param formatField1 a format specific field 1
   * @param formatField2 a format specific field 2
   * @param formatField3 a format specific field 3
   * @param formatField4 a format specific field 4
   * @param formatField5 a format specific field 5
   */
  void updateFormatFields(
      @Param("repositoryId") final Integer repositoryId,
      @Param("componentId") final Integer componentId,
      @Param("assetId") final Integer assetId,
      @Param("format") final String format,
      @Nullable @Param("formatField1") final String formatField1,
      @Nullable @Param("formatField2") final String formatField2,
      @Nullable @Param("formatField3") final String formatField3,
      @Nullable @Param("formatField4") final String formatField4,
      @Nullable @Param("formatField5") final String formatField5);

  /**
   * Delete the given search entry in the content data store.
   *
   * @param repositoryId the content repository identification
   * @param componentId  the component identification
   * @param assetId      the asset identification
   * @param format       the repository format
   */
  void delete(
      @Param("repositoryId") Integer repositoryId,
      @Param("componentId") Integer componentId,
      @Param("assetId") Integer assetId,
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
}
