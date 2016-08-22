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
package org.sonatype.nexus.repository.search;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilder;

/**
 * Search service: maintains indexes for repositories and performs indexing/deindexing of data, along with search.
 *
 * @since 3.0
 */
public interface SearchService
{

  /**
   * Create index for specified repository, if does not already exits.
   */
  void createIndex(Repository repository);

  /**
   * Deletes index for specified repository.
   */
  void deleteIndex(Repository repository);

  /**
   * Rebuilds index for specific reppsitory.
   */
  void rebuildIndex(Repository repository);

  /**
   * Puts data with given identifier into index of given repository.
   */
  void put(Repository repository, String identifier, String json);

  /**
   * Removes data with given identifier from index of given repository.
   */
  void delete(Repository repository, String identifier);

  /**
   * Search component metadata and browse results, without the effect of content selectors.
   *
   * @since 3.1
   */
  Iterable<SearchHit> browseUnrestricted(QueryBuilder query);

  /**
   * Search component metadata and browse results (paged), without the effect of content selectors.
   *
   * @since 3.1
   */
  Iterable<SearchHit> browseUnrestricted(QueryBuilder query, int from, int size);

  /**
   * Search component metadata and browse results (paged), without the effect of content selectors.
   *
   * @since 3.1
   */
  SearchResponse searchUnrestricted(QueryBuilder query, @Nullable List<SortBuilder> sort, int from, int size);

  /**
   * Search component metadata and browse results (paged) with content selectors applied.
   *
   * @since 3.1
   */
  SearchResponse search(QueryBuilder query, @Nullable List<SortBuilder> sort, int from, int size);

  /**
   * Count the number of results for a given query, without the effect of content selectors.
   *
   * @since 3.1
   */
  long countUnrestricted(QueryBuilder query);

}
