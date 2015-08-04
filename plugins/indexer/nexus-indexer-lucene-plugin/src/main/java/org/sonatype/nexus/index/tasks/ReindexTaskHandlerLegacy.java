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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.index.IndexerManager;

/**
 * Reindex task.
 *
 * @author cstamas
 * @author Alin Dreghiciu
 */
@Named("legacy")
@Singleton
public class ReindexTaskHandlerLegacy
    implements ReindexTaskHandler
{
  private final IndexerManager indexerManager;

  @Inject
  public ReindexTaskHandlerLegacy(final IndexerManager indexerManager) {
    this.indexerManager = indexerManager;
  }

  /**
   * Delegates to indexer manager.
   *
   * {@inheritDoc}
   */
  public void reindexAllRepositories(final String path,
                                     final boolean fullReindex)
      throws Exception
  {
    indexerManager.reindexAllRepositories(path, fullReindex);
  }

  /**
   * Delegates to indexer manager.
   *
   * {@inheritDoc}
   */
  public void reindexRepository(final String repositoryId,
                                final String path,
                                final boolean fullReindex)
      throws Exception
  {
    indexerManager.reindexRepository(path, repositoryId, fullReindex);
  }
}
