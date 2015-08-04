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
package org.sonatype.nexus.index;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.Field;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.treeview.TreeNode;
import org.apache.maven.index.treeview.TreeNodeFactory;

/**
 * Nexus facade for NexusIndexer operations.
 *
 * @author cstamas
 */
public interface IndexerManager
{
  // ----------------------------------------------------------------------------
  // Config management et al
  // ----------------------------------------------------------------------------

  /**
   * Shutdown of Indexer, with cleanup. It remove index files if needed.
   *
   * @param deleteFile set to true if you want to completely remove index files.
   */
  void shutdown(boolean deleteFiles)
      throws IOException;

  /**
   * Resets the configuration of Indexer, forcing it to renew it's values.
   */
  void resetConfiguration();

  // ----------------------------------------------------------------------------
  // Context management et al
  // ----------------------------------------------------------------------------

  /**
   * Adds a new IndexContext for repository.
   */
  void addRepositoryIndexContext(String repositoryId)
      throws IOException, NoSuchRepositoryException;

  /**
   * Removes an IndexContext for repository.
   */
  void removeRepositoryIndexContext(String repositoryId, boolean deleteFiles)
      throws IOException, NoSuchRepositoryException;

  /**
   * Updates an IndexContext for repository.
   */
  void updateRepositoryIndexContext(String repositoryId)
      throws IOException, NoSuchRepositoryException;

  // ----------------------------------------------------------------------------
  // adding/removing on the fly
  // ----------------------------------------------------------------------------

  /**
   * Adds an item to index.
   */
  void addItemToIndex(Repository repository, StorageItem item)
      throws IOException;

  /**
   * Removes single item from index.
   */
  void removeItemFromIndex(Repository repository, StorageItem item)
      throws IOException;

  // ----------------------------------------------------------------------------
  // Reindexing related (will do local-scan, remote-download, merge, publish)
  // ----------------------------------------------------------------------------

  void reindexAllRepositories(String path, boolean fullReindex)
      throws IOException;

  void reindexRepository(String path, String repositoryId, boolean fullReindex)
      throws NoSuchRepositoryException, IOException;

  // ----------------------------------------------------------------------------
  // Downloading remote indexes (will do remote-download, merge only)
  // ----------------------------------------------------------------------------

  void downloadAllIndex()
      throws IOException;

  void downloadRepositoryIndex(String repositoryId)
      throws IOException, NoSuchRepositoryException;

  // ----------------------------------------------------------------------------
  // Publishing index (will do publish only)
  // ----------------------------------------------------------------------------

  void publishAllIndex()
      throws IOException;

  void publishRepositoryIndex(String repositoryId)
      throws IOException, NoSuchRepositoryException;

  // ----------------------------------------------------------------------------
  // Optimizing index
  // ----------------------------------------------------------------------------

  void optimizeAllRepositoriesIndex()
      throws IOException;

  void optimizeRepositoryIndex(String repositoryId)
      throws NoSuchRepositoryException, IOException;

  // ----------------------------------------------------------------------------
  // Identify
  // ----------------------------------------------------------------------------

  Collection<ArtifactInfo> identifyArtifact(Field field, String data)
      throws IOException;

  // ----------------------------------------------------------------------------
  // Combined searching
  // ----------------------------------------------------------------------------

  @Deprecated
  FlatSearchResponse searchArtifactFlat(String term, String repositoryId, Integer from, Integer count,
                                        Integer hitLimit)
      throws NoSuchRepositoryException;

  @Deprecated
  FlatSearchResponse searchArtifactClassFlat(String term, String repositoryId, Integer from, Integer count,
                                             Integer hitLimit)
      throws NoSuchRepositoryException;

  @Deprecated
  FlatSearchResponse searchArtifactFlat(String gTerm, String aTerm, String vTerm, String pTerm, String cTerm,
                                        String repositoryId, Integer from, Integer count, Integer hitLimit)
      throws NoSuchRepositoryException;

  IteratorSearchResponse searchQueryIterator(Query query, String repositoryId, Integer from, Integer count,
                                             Integer hitLimit, boolean uniqueRGA, List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;

  IteratorSearchResponse searchArtifactIterator(String term, String repositoryId, Integer from, Integer count,
                                                Integer hitLimit, boolean uniqueRGA, SearchType searchType,
                                                List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;

  IteratorSearchResponse searchArtifactClassIterator(String term, String repositoryId, Integer from, Integer count,
                                                     Integer hitLimit, SearchType searchType,
                                                     List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;

  IteratorSearchResponse searchArtifactIterator(String gTerm, String aTerm, String vTerm, String pTerm,
                                                String cTerm, String repositoryId, Integer from, Integer count,
                                                Integer hitLimit, boolean uniqueRGA, SearchType searchType,
                                                List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;

  IteratorSearchResponse searchArtifactSha1ChecksumIterator(String sha1Checksum, String repositoryId, Integer from,
                                                            Integer count, Integer hitLimit,
                                                            List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException;

  // ----------------------------------------------------------------------------
  // Query construction
  // ----------------------------------------------------------------------------

  @Deprecated
  Query constructQuery(Field field, String query, SearchType type)
      throws IllegalArgumentException;

  Query constructQuery(Field field, SearchExpression expression)
      throws IllegalArgumentException;

  // ----------------------------------------------------------------------------
  // Tree nodes
  // ----------------------------------------------------------------------------

  TreeNode listNodes(final TreeNodeFactory factory, final String path, final String repositoryId)
      throws NoSuchRepositoryException, IOException;

  TreeNode listNodes(final TreeNodeFactory factory, final String path, final Map<Field, String> hints,
                     final ArtifactInfoFilter artifactInfoFilter, final String repositoryId)
      throws NoSuchRepositoryException, IOException;
}
