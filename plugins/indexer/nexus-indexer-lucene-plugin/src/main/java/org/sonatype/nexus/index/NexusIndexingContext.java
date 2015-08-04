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
import java.util.List;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;

/**
 * Nexus indexing context, derived from default implementation.
 *
 * @since 2.3
 */
public class NexusIndexingContext
    extends DefaultIndexingContext
{

  private final boolean receivingUpdates;

  public NexusIndexingContext(final String id, final String repositoryId, final File repository,
                              final Directory indexDirectory,
                              final String repositoryUrl, final String indexUpdateUrl,
                              final List<? extends IndexCreator> indexCreators,
                              final boolean reclaimIndex,
                              final boolean receivingUpdates)
      throws IOException, ExistingLuceneIndexMismatchException
  {
    super(id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl, indexCreators,
        reclaimIndex);
    this.receivingUpdates = receivingUpdates;
  }

  @Override
  protected IndexWriterConfig getWriterConfig() {
    final IndexWriterConfig writerConfig = super.getWriterConfig();

    // NEXUS-5380 force use of compound lucene index file to postpone "Too many open files"

    final TieredMergePolicy mergePolicy = new TieredMergePolicy();
    mergePolicy.setUseCompoundFile(true);
    mergePolicy.setNoCFSRatio(1.0);

    writerConfig.setMergePolicy(mergePolicy);

    return writerConfig;
  }

  @Override
  public void optimize() throws IOException {
    super.optimize();
    // NEXUS-6156: Make SearcherManager be notified about this
    // This will release the deleted files removed after. SearcherManager is not reachable (private, no getter)
    // so this is the only way to make superclass invoke SearcherManager#maybeRefresh()
    releaseIndexSearcher(acquireIndexSearcher());
  }

  @Override
  public boolean isReceivingUpdates() {
    return receivingUpdates;
  }
}
