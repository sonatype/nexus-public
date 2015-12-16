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
package org.sonatype.timeline.internal;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.timeline.Timeline;
import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineFilter;
import org.sonatype.timeline.TimelineRecord;

@Singleton
@Named("default")
public class DefaultTimeline
    extends ComponentSupport
    implements Timeline
{
  private volatile boolean started;

  private final DefaultTimelinePersistor persistor;

  private final DefaultTimelineIndexer indexer;

  private final ReentrantReadWriteLock timelineLock;

  /**
   * Constructor. See {@link DefaultTimelineIndexer} constructor for meaning of {@code luceneFSDirectoryType}. Note:
   * The {@code luceneFSDirectoryType} is copied from nexus-indexer-lucene-plugin's DefaultIndexerManager as part of
   * fix for NEXUS-5658, hence, the key used here must be kept in sync with the one used in DefaultIndexerManager!
   */
  @Inject
  public DefaultTimeline(@Nullable @Named("${lucene.fsdirectory.type}") final String luceneFSDirectoryType) {
    this.started = false;
    this.persistor = new DefaultTimelinePersistor();
    this.indexer = new DefaultTimelineIndexer(luceneFSDirectoryType);
    this.timelineLock = new ReentrantReadWriteLock();
  }

  /**
   * Visible for UT
   */
  protected DefaultTimelineIndexer getIndexer() {
    return indexer;
  }

  /**
   * Visible for UT
   */
  protected DefaultTimelinePersistor getPersistor() {
    return persistor;
  }

  // ==
  // Public API

  public void start(final TimelineConfiguration configuration)
      throws IOException
  {
    log.debug("Starting Timeline...");
    timelineLock.writeLock().lock();
    try {
      if (!started) {
        // if persistor fails, it's a total failure, we
        // cannot work without persistor
        persistor.setConfiguration(configuration);

        // indexer start, that might need repair
        // and might end up in falied repair
        try {
          indexer.start(configuration);
        }
        catch (IOException e) {
          // we are starting, so repair must be tried
          log.info("Timeline index got corrupted, trying to repair it.", e);
          // stopping it cleanly
          indexer.stop();
          // deleting index files
          DirSupport.empty(configuration.getIndexDirectory().toPath());
          try {
            // creating new index from scratch
            indexer.start(configuration);
            // pouring over records from persisted into indexer
            final RepairBatch rb = new RepairBatch(indexer);
            persistor.readAllSinceDays(configuration.getRepairDaysCountRestored(), rb);
            rb.finish();

            log.info(
                "Timeline index is succesfully repaired, the last "
                    + configuration.getRepairDaysCountRestored() + " days were restored.");
          }
          catch (Exception ex) {
            // do not propagate the exception for indexer
            // we have persistor started, and that's enough
            markIndexerDead(ex);
          }
        }
        DefaultTimeline.this.started = true;
        log.info("Started Timeline...");
      }
    }
    finally {
      timelineLock.writeLock().unlock();
    }
  }

  public void stop()
      throws IOException
  {
    log.debug("Stopping Timeline...");
    timelineLock.writeLock().lock();
    try {
      if (started) {
        DefaultTimeline.this.started = false;
        indexer.stop();
        log.info("Stopped Timeline...");
      }
    }
    finally {
      timelineLock.writeLock().unlock();
    }
  }

  public boolean isStarted() {
    return started;
  }

  @Override
  public void add(final TimelineRecord... records) {
    if (!started) {
      return;
    }
    try {
      persistor.persist(records);
      addToIndexer(records);
    }
    catch (IOException e) {
      log.warn("Failed to add a timeline record", e);
    }
  }

  @Override
  public int purgeOlderThan(final int days) {
    if (started) {
      return doShared(new Work<Integer>()
      {
        @Override
        public Integer doIt()
            throws IOException
        {
          try {
            persistor.purge(days);
          }
          catch (IOException e) {
            // we don't want to make indexer dead in here
            // FIXME: but do we want to abort the purge?
            log.warn("Failed to purge a timeline persisted records", e);
          }
          return indexer.purge(0l, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days), null, null);
        }
      });
    }
    return 0;
  }

  @Override
  public void retrieve(int fromItem, int count, Set<String> types, Set<String> subTypes, TimelineFilter filter,
                       TimelineCallback callback)
  {
    if (!started) {
      return;
    }
    retrieveFromIndexer(0L, System.currentTimeMillis(), fromItem, count, types, subTypes, filter, callback);
  }

  // ==

  protected void addToIndexer(final TimelineRecord... records) {
    doShared(new Work<Void>()
    {
      @Override
      public Void doIt()
          throws IOException
      {
        indexer.addAll(records);
        return null;
      }
    });
  }

  protected void retrieveFromIndexer(final long fromTime, final long toTime, final int from, final int count,
                                     final Set<String> types, final Set<String> subTypes,
                                     final TimelineFilter filter, final TimelineCallback callback)
  {
    doShared(new Work<Void>()
    {
      @Override
      public Void doIt()
          throws IOException
      {
        indexer.retrieve(fromTime, toTime, types, subTypes, from, count, filter, callback);
        return null;
      }
    });
  }

  // ==

  protected static interface Work<E>
  {

    E doIt()
        throws IOException;

  }

  protected <E> E doShared(final Work<E> work) {
    if (timelineLock.readLock().tryLock()) {
      try {
        if (started && !indexerIsDead) {
          boolean isInterrupted = Thread.interrupted();
          try {
            return work.doIt();
          }
          catch (IOException e) {
            markIndexerDead(e);
          }
          finally {
            if (isInterrupted) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
      finally {
        timelineLock.readLock().unlock();
      }
    }
    return null;
  }

  // ==

  private volatile boolean indexerIsDead = false;

  protected void markIndexerDead(final Exception e) {
    if (!indexerIsDead) {
      log.warn("Timeline index got corrupted and is disabled. Repair will be tried on next boot.", e);
      // we need to stop it and signal to not try any other thread
      indexerIsDead = true;
      try {
        indexer.stop();
      }
      catch (IOException ex) {
        log.warn("Timeline index can't be stopped cleanly after it's corruption.", ex);
      }
    }
  }
}
