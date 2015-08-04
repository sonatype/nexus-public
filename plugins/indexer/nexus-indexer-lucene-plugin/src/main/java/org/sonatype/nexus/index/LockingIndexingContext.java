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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;

/**
 * Read-only indexing context wrapper with locking of IndexSearcher acquire/release operations.
 */
class LockingIndexingContext
    implements IndexingContext
{

  private final IndexingContext context;

  private final Lock lock;

  private boolean closed;

  private static final IndexSearcher EMPTY_SEARCHER;

  static {
    EMPTY_SEARCHER = new MemoryIndex().createSearcher();
  }

  public LockingIndexingContext(IndexingContext context, Lock lock) {
    this.context = context;
    this.lock = lock;
  }

  @Override
  public String getId() {
    return context.getId();
  }

  @Override
  public String getRepositoryId() {
    return context.getRepositoryId();
  }

  @Override
  public File getRepository() {
    return context.getRepository();
  }

  @Override
  public String getRepositoryUrl() {
    return context.getRepositoryUrl();
  }

  @Override
  public String getIndexUpdateUrl() {
    return context.getIndexUpdateUrl();
  }

  @Override
  public boolean isSearchable() {
    return context.isSearchable();
  }

  @Override
  public void setSearchable(boolean searchable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getTimestamp() {
    return context.getTimestamp();
  }

  @Override
  public void updateTimestamp()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateTimestamp(boolean save)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateTimestamp(boolean save, Date date)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getSize()
      throws IOException
  {
    lock.lock();
    try {
      return !isClosed() ? context.getSize() : 0;
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public IndexSearcher acquireIndexSearcher()
      throws IOException
  {
    lock.lock();
    return !isClosed() ? context.acquireIndexSearcher() : EMPTY_SEARCHER;
  }

  @Override
  public void releaseIndexSearcher(IndexSearcher s)
      throws IOException
  {
    try {
      if (!isClosed()) {
        context.releaseIndexSearcher(s);
      }
    }
    finally {
      lock.unlock();
    }
  }

  private synchronized boolean isClosed() {
    // It is theoretically possible to "reopen" closed indexing context by context.replace(Directory)
    // To guarantee balance of acquireIndexSearcher/releaseIndexSearcher calls, closed context must stay close
    if (!closed) {
      closed = context.getIndexDirectory() == null;
    }
    return closed;
  }

  @Override
  public IndexWriter getIndexWriter()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<IndexCreator> getIndexCreators() {
    return context.getIndexCreators();
  }

  @Override
  public Analyzer getAnalyzer() {
    return context.getAnalyzer();
  }

  @Override
  public void commit()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rollback()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void optimize()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close(boolean deleteFiles)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void purge()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void merge(Directory directory)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void merge(Directory directory, DocumentFilter filter)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void replace(Directory directory)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Directory getIndexDirectory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public File getIndexDirectoryFile() {
    return context.getIndexDirectoryFile();
  }

  @Override
  public GavCalculator getGavCalculator() {
    return context.getGavCalculator();
  }

  @Override
  public void setAllGroups(Collection<String> groups)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getAllGroups()
      throws IOException
  {
    lock.lock();
    try {
      return !isClosed() ? context.getAllGroups() : Collections.<String>emptySet();
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void setRootGroups(Collection<String> groups)
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getRootGroups()
      throws IOException
  {
    lock.lock();
    try {
      return !isClosed() ? context.getRootGroups() : Collections.<String>emptySet();
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void rebuildGroups()
      throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isReceivingUpdates() {
    return context.isReceivingUpdates();
  }

  public IndexingContext getContext() {
    IndexingContext result = context;
    while (result instanceof LockingIndexingContext) {
      result = ((LockingIndexingContext) result).getContext();
    }
    return result;
  }
}
