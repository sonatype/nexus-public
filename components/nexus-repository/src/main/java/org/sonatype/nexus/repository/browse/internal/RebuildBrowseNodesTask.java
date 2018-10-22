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
package org.sonatype.nexus.repository.browse.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.logging.task.ProgressLogIntervalHelper;
import org.sonatype.nexus.orient.entity.AttachedEntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.AssetStore;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexCursor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Browse nodes rebuild task.
 *
 * @since 3.6
 */
@Named
public class RebuildBrowseNodesTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  private static final int BUCKET_KEY_ID = 0;

  private final AssetStore assetStore;

  private final BucketStore bucketStore;

  private final BrowseNodeManager browseNodeManager;

  private final int rebuildPageSize;

  @Inject
  public RebuildBrowseNodesTask(final AssetStore assetStore,
                                final BucketStore bucketStore,
                                final BrowseNodeManager browseNodeManager,
                                final BrowseNodeConfiguration configuration)
  {
    this.assetStore = checkNotNull(assetStore);
    this.bucketStore = checkNotNull(bucketStore);
    this.browseNodeManager = checkNotNull(browseNodeManager);
    this.rebuildPageSize = checkNotNull(configuration).getRebuildPageSize();
  }

  @Override
  public String getMessage() {
    return "Rebuilding browse tree for " + getRepositoryField();
  }

  @Override
  protected void execute(final Repository repo) {

    log.info("Deleting browse nodes for repository {}", repo.getName());

    browseNodeManager.deleteByRepository(repo.getName());

    log.info("Rebuilding browse nodes for repository {}", repo.getName());

    Bucket bucket = bucketStore.read(repo.getName());
    ORID bucketId = AttachedEntityHelper.id(bucket);

    try {
      long processed = 0;
      long total = assetStore.countAssets(ImmutableList.of(bucket));

      if (total > 0) {
        ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
        Stopwatch sw = Stopwatch.createStarted();

        OIndexCursor cursor = assetStore.getIndex(AssetEntityAdapter.I_BUCKET_COMPONENT_NAME).cursor();
        List<Entry<OCompositeKey, EntityId>> nextPage = assetStore.getNextPage(cursor, rebuildPageSize);
        while (!Iterables.isEmpty(nextPage)) {
          checkContinuation(repo);

          List<Asset> assets = new ArrayList<>(rebuildPageSize);
          for (Entry<OCompositeKey, EntityId> indexEntry : nextPage) {
            if (bucketId.equals(indexEntry.getKey().getKeys().get(BUCKET_KEY_ID))) {
              assets.add(assetStore.getById(indexEntry.getValue()));
            }
          }

          int assetsSize = Iterables.size(assets);

          browseNodeManager.createFromAssets(repo, assets);

          processed += assetsSize;

          long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
          progressLogger.info("Rebuilt {} / {} browse nodes in {} ms", processed, total, elapsed);

          nextPage = assetStore.getNextPage(cursor, rebuildPageSize);
        }
        progressLogger.flush(); // ensure final rebuild message is flushed
      }
    }
    catch (Exception e) {
      log.error("Could not re-create browse nodes for repository: {}", repo, e);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository != null;
  }

  private void checkContinuation(final Repository repo) {
    if (isCanceled()) {
      throw new TaskInterruptedException(String.format("Rebuilding browse nodes was cancelled for %s", repo.getName()),
          true);
    }
  }
}
