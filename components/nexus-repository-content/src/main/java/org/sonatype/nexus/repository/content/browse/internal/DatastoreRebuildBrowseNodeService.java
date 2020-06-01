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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodeFailedException;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodeService;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.base.Stopwatch;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Rebuild browse node service for the Orient based browse node implementation.
 *
 * @since 3.24
 */
@Singleton
@Named("mybatis")
public class DatastoreRebuildBrowseNodeService
    extends ComponentSupport
    implements RebuildBrowseNodeService
{
  private final DatastoreBrowseNodeManager browseNodeManager;

  private final int browseLimit;

  @Inject
  public DatastoreRebuildBrowseNodeService(
      final DatastoreBrowseNodeManager browseNodeManager,
      final BrowseNodeConfiguration configuration)
  {
    this.browseNodeManager = checkNotNull(browseNodeManager);
    this.browseLimit = checkNotNull(configuration).getRebuildPageSize();
  }

  @Override
  public void rebuild(final Repository repo, final BooleanSupplier isCancelled)
      throws RebuildBrowseNodeFailedException
  {
    browseNodeManager.deleteByRepository(repo);

    log.info("Rebuilding browse nodes for repository {}", repo.getName());

    ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);

    try {
      Stopwatch sw = Stopwatch.createStarted();
      long processed = 0;

      ContentFacet contentFacet = repo.facet(ContentFacet.class);
      Continuation<FluentAsset> assetContinuation = contentFacet.assets().browse(browseLimit, null);

      while (!assetContinuation.isEmpty()) {
        checkContinuation(isCancelled, repo);

        browseNodeManager.createFromAssets(repo, assetContinuation);

        processed += assetContinuation.size();
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
        progressLogger.info("Rebuilt browse nodes for {} assets in {} ms", processed, elapsed);

        assetContinuation = contentFacet.assets().browse(browseLimit, assetContinuation.nextContinuationToken());
      }
    }
    catch (Exception ex) {
      throw new RebuildBrowseNodeFailedException("Could not re-create browse nodes", ex);
    }
    finally {
      progressLogger.flush(); // ensure final rebuild message is flushed
      log.info("Browse Node Rebuild ended for {}", repo.getName());
    }
  }

  private void checkContinuation(final BooleanSupplier isCancelled, final Repository repo) {
    if (isCancelled.getAsBoolean()) {
      throw new TaskInterruptedException(format("Rebuilding browse nodes was cancelled for %s", repo.getName()), true);
    }
  }
}
