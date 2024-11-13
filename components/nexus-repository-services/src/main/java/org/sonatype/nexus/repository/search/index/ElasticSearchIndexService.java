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
package org.sonatype.nexus.repository.search.index;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;

import com.google.common.annotations.VisibleForTesting;

/**
 * Search index service.
 *
 * @since 3.25
 */
public interface ElasticSearchIndexService
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
   * Rebuilds index for specific repository.
   */
  void rebuildIndex(Repository repository);

  /**
   * Check search index exists for specific repository
   */
  boolean indexExist(Repository repository);

  /**
   * Checks whether search index for the specified repository is empty.
   *
   * @param repository The repository to check.
   * @return True if the index is empty, false otherwise.
   */
  boolean indexEmpty(Repository repository);

  /**
   * Puts data with given identifier into index of given repository.
   */
  void put(Repository repository, String identifier, String json);

  /**
   * Operation used for bulk updating of component index.
   *
   * The update is done on a separate dedicated thread.
   *
   * @param repository the source repository
   * @param components an {@link Iterable} of components to index
   * @param identifierProducer a function producing an identifier for a component (never returning null)
   * @param jsonDocumentProducer a function producing a json document for the component (never returning null)
   *
   * @return A list of Future objects for tracking each update.
   */
  <T> List<Future<Void>> bulkPut(Repository repository,
                                 Iterable<T> components,
                                 Function<T, String> identifierProducer,
                                 Function<T, String> jsonDocumentProducer);

  /**
   * Removes data with given identifier from index of given repository.
   */
  void delete(Repository repository, String identifier);

  /**
   * Operation used for bulk removal of data from index of given repository.
   *
   * @param repository the source repository (if known)
   * @param identifiers the ids of documents to remove
   */
  void bulkDelete(@Nullable Repository repository, Iterable<String> identifiers);

  /**
   * Flush any pending bulk index requests.
   */
  void flush(boolean fsync);

  /**
   * Used by ITs to check the frequency of search updates.
   */
  @VisibleForTesting
  long getUpdateCount();

  /**
   * Used by UTs and ITs only to "wait for calm period" when all search indexing is finished.
   */
  @VisibleForTesting
  boolean isCalmPeriod();

  /**
   * Wait for a calm period where no search indexing is happening.
   */
  @VisibleForTesting
  void waitForCalm();

  /**
   * Wait for the elasticsearch index to be in a queryable state.
   */
  @VisibleForTesting
  void waitForReady();
}
