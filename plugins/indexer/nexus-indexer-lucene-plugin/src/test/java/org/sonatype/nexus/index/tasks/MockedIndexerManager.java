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
package org.sonatype.nexus.index.tasks;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.index.IndexerManager;
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

public class MockedIndexerManager
    implements IndexerManager
{

  public static boolean returnError = false;

  public static boolean mockInvoked = false;

  @Override
  public void shutdown(boolean deleteFiles)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void resetConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addRepositoryIndexContext(String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeRepositoryIndexContext(String repositoryId, boolean deleteFiles)
      throws IOException, NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateRepositoryIndexContext(String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void addItemToIndex(Repository repository, StorageItem item)
      throws IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void removeItemFromIndex(Repository repository, StorageItem item)
      throws IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void reindexAllRepositories(String path, boolean fullReindex)
      throws IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void reindexRepository(String path, String repositoryId, boolean fullReindex)
      throws NoSuchRepositoryException, IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void downloadAllIndex()
      throws IOException
  {
    mockInvoked = true;
    if (returnError) {
      throw new IOException("Mocked Index Error");
    }
  }

  @Override
  public void downloadRepositoryIndex(String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    mockInvoked = true;
    if (returnError) {
      throw new IOException("Mocked Index Error");
    }
  }

  @Override
  public void publishAllIndex()
      throws IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void publishRepositoryIndex(String repositoryId)
      throws IOException, NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void optimizeAllRepositoriesIndex()
      throws IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public void optimizeRepositoryIndex(String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    throw new UnsupportedOperationException();

  }

  @Override
  public Collection<ArtifactInfo> identifyArtifact(Field field, String data)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlatSearchResponse searchArtifactFlat(String term, String repositoryId, Integer from, Integer count,
                                               Integer hitLimit)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlatSearchResponse searchArtifactClassFlat(String term, String repositoryId, Integer from, Integer count,
                                                    Integer hitLimit)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlatSearchResponse searchArtifactFlat(String gTerm, String aTerm, String vTerm, String pTerm, String cTerm,
                                               String repositoryId, Integer from, Integer count, Integer hitLimit)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorSearchResponse searchQueryIterator(Query query, String repositoryId, Integer from, Integer count,
                                                    Integer hitLimit, boolean uniqueRGA,
                                                    List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorSearchResponse searchArtifactIterator(String term, String repositoryId, Integer from,
                                                       Integer count, Integer hitLimit, boolean uniqueRGA,
                                                       SearchType searchType, List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorSearchResponse searchArtifactClassIterator(String term, String repositoryId, Integer from,
                                                            Integer count, Integer hitLimit, SearchType searchType,
                                                            List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorSearchResponse searchArtifactIterator(String gTerm, String aTerm, String vTerm, String pTerm,
                                                       String cTerm, String repositoryId, Integer from,
                                                       Integer count, Integer hitLimit, boolean uniqueRGA,
                                                       SearchType searchType, List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public IteratorSearchResponse searchArtifactSha1ChecksumIterator(String sha1Checksum, String repositoryId,
                                                                   Integer from, Integer count, Integer hitLimit,
                                                                   List<ArtifactInfoFilter> filters)
      throws NoSuchRepositoryException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query constructQuery(Field field, String query, SearchType type)
      throws IllegalArgumentException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Query constructQuery(Field field, SearchExpression expression)
      throws IllegalArgumentException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public TreeNode listNodes(TreeNodeFactory factory, String path, String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public TreeNode listNodes(TreeNodeFactory factory, String path, Map<Field, String> hints,
                            ArtifactInfoFilter artifactInfoFilter, String repositoryId)
      throws NoSuchRepositoryException, IOException
  {
    throw new UnsupportedOperationException();
  }

}
