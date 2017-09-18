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
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndexCursor;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Browse nodes rebuild task.
 *
 * @since 3.6
 */
@Named
public class RebuildBrowseNodesTask
    extends RepositoryTaskSupport
{
  private static final int BUCKET_KEY_ID = 0;

  private final AssetStore assetStore;

  private final BucketStore bucketStore;

  private final BrowseNodeWrapper browseNodeWrapper;

  private final BrowseNodeStore browseNodeStore;

  private final int limit;

  @Inject
  public RebuildBrowseNodesTask(final AssetStore assetStore,
                                final BucketStore bucketStore,
                                final BrowseNodeWrapper browseNodeWrapper,
                                final BrowseNodeStore browseNodeStore,
                                final BrowseNodeConfiguration configuration)
  {
    this.assetStore = checkNotNull(assetStore);
    this.bucketStore = checkNotNull(bucketStore);
    this.browseNodeWrapper = checkNotNull(browseNodeWrapper);
    this.browseNodeStore = checkNotNull(browseNodeStore);
    this.limit = checkNotNull(configuration).getRebuildPageSize();
  }

  @Override
  public String getMessage() {
    return "Rebuilding browse tree for " + getRepositoryField();
  }

  @Override
  protected void execute(final Repository repo) {
    browseNodeStore.truncateRepository(repo.getName());
    Iterable<Bucket> buckets = singletonList(bucketStore.read(repo.getName()));
    List<ORID> bucketIds = stream(buckets.spliterator(), false).map(AttachedEntityHelper::id).collect(toList());

    try {
      long processed = 0;
      long total = assetStore.countAssets(buckets);

      if (total > 0) {
        ProgressLogIntervalHelper progressLogger = new ProgressLogIntervalHelper(log, 60);
        Stopwatch sw = Stopwatch.createStarted();
        long lastTime = sw.elapsed(TimeUnit.MILLISECONDS);

        OIndexCursor cursor = assetStore.getIndex(AssetEntityAdapter.I_BUCKET_COMPONENT_NAME).descCursor();
        List<Entry<OCompositeKey, EntityId>> nextPage = assetStore.getNextPage(cursor, limit);
        while (!Iterables.isEmpty(nextPage)) {
          checkContinuation(repo);

          List<Asset> assets = new ArrayList<>(limit);
          for (Entry<OCompositeKey, EntityId> indexEntry : nextPage) {
            ORID bucketId = (ORID) indexEntry.getKey().getKeys().get(BUCKET_KEY_ID);
            if (bucketIds.contains(bucketId)) {
              assets.add(assetStore.getById(indexEntry.getValue()));
            }
          }

          int assetsSize = Iterables.size(assets);

          browseNodeWrapper.createFromAssets(repo, assets);

          processed += assetsSize;

          long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
          progressLogger.info("rebuilding tree for {} assets took {} ms of {} ms, {} / {} assets processed", assetsSize,
              elapsed - lastTime, elapsed, processed, total);

          lastTime = sw.elapsed(TimeUnit.MILLISECONDS);

          nextPage = assetStore.getNextPage(cursor, limit);
        }
        progressLogger.flush(); // ensure final rebuild message is flushed

        progressLogger.info("updating children references for {} assets", total);
        progressLogger.flush();
        browseNodeStore.updateChildNodes(repo.getName());
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
    // Treat repositories put offline during walking as cancellation
    if (!repo.getConfiguration().isOnline()) {
      throw new TaskInterruptedException(String.format(
          "Repository %s is offline, rebuilding browse nodes was cancelled", repo.getName()), false);
    }
  }
}
