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
package org.sonatype.nexus.repository.content.search.table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.scheduling.PeriodicJobService.PeriodicJob;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.codahale.metrics.annotation.Gauge;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.MIN_PRIORITY;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class SearchTableEventProcessor
    extends LifecycleSupport
{
  public enum EventType
  {
    ASSET_CREATED, ASSET_DELETED, REPOSITORY_DELETED, COMPONENT_KIND_UPDATED, ASSET_ATTRIBUTES_UPDATED
  }

  private static class InternalEvent
  {
    private final EventType type;

    private final SearchTableData data;

    private InternalEvent(final EventType type, final SearchTableData data) {
      this.type = checkNotNull(type);
      this.data = checkNotNull(data);
    }
  }

  private final SearchTableStore store;

  private final PeriodicJobService periodicJobService;

  private final int flushPoolSize;

  private final int flushOnCount;

  private final int flushOnSeconds;

  private final Lock lock = new ReentrantLock(true);

  private final AtomicInteger pendingEventsCount = new AtomicInteger();

  private final ConcurrentLinkedQueue<InternalEvent> eventsQueue = new ConcurrentLinkedQueue<>();

  private PeriodicJob flushTask;

  private ThreadPoolExecutor threadPoolExecutor;

  private boolean processEvents = true;

  @Inject
  public SearchTableEventProcessor(
      final SearchTableStore store,
      final PeriodicJobService periodicJobService,
      @Named("${nexus.search.event.handler.flushPoolSize:-128}") final int flushPoolSize,
      @Named("${nexus.search.event.handler.flushOnCount:-100}") final int flushOnCount,
      @Named("${nexus.search.event.handler.flushOnSeconds:-2}") final int flushOnSeconds)
  {
    this.store = checkNotNull(store);
    this.periodicJobService = checkNotNull(periodicJobService);
    checkArgument(flushPoolSize > 0, "nexus.search.event.handler.flushPoolSize must be greater than zero");
    this.flushPoolSize = flushPoolSize;
    checkArgument(flushOnCount > 0, "nexus.search.event.handler.flushOnCount must be positive");
    this.flushOnCount = flushOnCount;
    checkArgument(flushOnSeconds > 0, "nexus.search.event.handler.flushOnSeconds must be positive");
    this.flushOnSeconds = flushOnSeconds;
  }

  @Gauge(name = "nexus.search.eventHandler.executor.queueSize")
  public int searchEventQueue() {
    return threadPoolExecutor.getQueue().size();
  }

  /**
   * Adding new event
   *
   * @param eventType event type
   * @param data      date to flush
   */
  public void addEvent(EventType eventType, SearchTableData data) {
    if (eventsQueue.add(new InternalEvent(eventType, data))) {
      pendingEventsCount.incrementAndGet();
    }
  }

  @VisibleForTesting
  public boolean isCalmPeriod() {
    return threadPoolExecutor.getQueue().isEmpty() && threadPoolExecutor.getActiveCount() == 0;
  }

  @Override
  protected void doStart() throws Exception {
    if (flushOnSeconds > 1) {
      periodicJobService.startUsing();
      flushTask = periodicJobService.schedule(this::checkAndFlushJob, flushOnSeconds);
    }

    this.threadPoolExecutor = new ThreadPoolExecutor(
        flushPoolSize, // core-size
        flushPoolSize, // max-size
        0L, // keep-alive
        TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), // allow queueing up of requests
        new NexusThreadFactory("searchTableSubscriber", "flushAndPurge", MIN_PRIORITY),
        new AbortPolicy());
  }

  @Override
  protected void doStop() throws Exception {
    if (flushOnSeconds > 1) {
      flushTask.cancel();
      periodicJobService.stopUsing();
    }

    this.threadPoolExecutor.shutdownNow();
    int unFlushedEvents = eventsQueue.size();
    if (unFlushedEvents > 0) {
      flush(unFlushedEvents);
    }
  }

  /**
   * Allow event processing to be disabled.
   */
  public void setProcessEvents(final boolean processEvents) {
    this.processEvents = processEvents;
  }

  /**
   * Used by all other methods to poll for work.
   */
  public void checkAndFlush() {
    checkAndFlush(false);
  }

  /**
   * Used by scheduled flush task to poll for work.
   */
  private void checkAndFlushJob() {
    checkAndFlush(true);
  }

  private void checkAndFlush(final boolean jobCalled) {
    if (processEvents) {
      int pendingEvents = pendingEventsCount.get();
      if (jobCalled && pendingEvents > 0) {
        threadPoolExecutor.execute(() -> flush(Math.min(pendingEvents, flushOnCount)));
      }
      else if (pendingEvents >= flushOnCount) {
        threadPoolExecutor.execute(() -> flush(flushOnCount));
      }
    }
  }

  /**
   * Grabs a bunch of events and flush them to a data store.
   *
   * @param eventsToFlushQty events to flush quantity
   */
  private void flush(final int eventsToFlushQty) {
    try {
      lock.lock();

      List<InternalEvent> eventsToFlush = new ArrayList<>(eventsToFlushQty);
      for (int i = 0; i < eventsToFlushQty; i++) {
        InternalEvent event = eventsQueue.poll();
        if (event != null) {
          eventsToFlush.add(event);
          pendingEventsCount.decrementAndGet();
        }
      }

      for (InternalEvent e : eventsToFlush) {
        SearchTableData data = e.data;
        Integer repositoryId = data.getRepositoryId();
        Integer componentId = data.getComponentId();
        Integer assetId = data.getAssetId();
        String format = data.getFormat();

        switch (e.type) {
          case ASSET_CREATED: {
            log.trace("Creating a new record into component_search table: {}", data);
            store.create(data);
            break;
          }
          case ASSET_ATTRIBUTES_UPDATED: {
            String formatField1 = data.getFormatField1();
            String formatField2 = data.getFormatField2();
            String formatField3 = data.getFormatField3();
            log.trace("Updating format fields in component_search table for repositoryId: {}, componentId: {}, " +
                    "assetId: {}, format: {}, formatField1: {}, formatField2: {}, formatField3: {}",
                repositoryId, componentId, assetId, format, formatField1, formatField2, formatField3);
            store.updateFormatFields(repositoryId, componentId, assetId, format, formatField1, formatField2,
                formatField3);
            break;
          }
          case ASSET_DELETED: {
            log.trace("Deleting a record from component_search table for repositoryId: {}, componentId: {}, " +
                "assetId: {}, format: {}", repositoryId, componentId, assetId, format);
            store.delete(repositoryId, componentId, assetId, format);
            break;
          }
          case REPOSITORY_DELETED: {
            log.trace("Deleting all records from component_search table for repository id: {}, format: {}",
                repositoryId, format);
            store.deleteAllForRepository(repositoryId, format);
            break;
          }
          case COMPONENT_KIND_UPDATED: {
            String componentKind = data.getComponentKind();
            log.trace("Updating a component kind in component_search table for repositoryId: {}, componentId: {}, " +
                "format: {}, componentKind: {}", repositoryId, componentId, format, componentKind);
            store.updateKind(repositoryId, componentId, format, componentKind);
            break;
          }
          default:
            log.error("Unsupported event type: {}", e.type);
        }
      }
    }
    finally {
      lock.unlock();
    }
  }
}
