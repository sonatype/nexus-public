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
package org.sonatype.nexus.repository.assetdownloadcount.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.assetdownloadcount.DateType;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Will periodically remove old download count records
 *
 * @since 3.4
 */
@Named
@Singleton
public class AssetDownloadHistoricDataCleaner
  extends ComponentSupport
    implements Runnable
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AssetDownloadCountEntityAdapter assetDownloadCountEntityAdapter;

  private final long interval;

  private final ExecutorService executorService;

  private final AtomicBoolean running = new AtomicBoolean();

  private static final String ERROR_MSG = "will restart process on next download count increment request";

  @Inject
  public AssetDownloadHistoricDataCleaner(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstance,
                                          final AssetDownloadCountEntityAdapter assetDownloadCountEntityAdapter,
                                          @Named("${nexus.assetdownloads.historicdata.cleaner.interval:-86400}") final long interval)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.assetDownloadCountEntityAdapter = checkNotNull(assetDownloadCountEntityAdapter);
    this.interval = interval;
    executorService = NexusExecutorService.forCurrentSubject(Executors.newSingleThreadExecutor(
        new NexusThreadFactory("assetdownloads-cleaner", "Asset Downloads Historic Data Cleaner")
    ));
  }

  public void start() {
    if (!running.getAndSet(true)) {
      executorService.submit(this);
    }
  }

  public void stop() {
    running.set(false);
    executorService.shutdownNow();
  }

  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void run() {
    do {
      try {
        doDelete();

        Thread.sleep(interval * 1000);
      }
      catch (InterruptedException e) { // NOSONAR
        log.debug("Periodic checks interrupted, {}", ERROR_MSG);
        running.set(false);
      }
      catch (Exception e) {
        log.debug("Periodic checks failed, {}", ERROR_MSG, e);
        running.set(false);
      }
    }
    while (running.get());
  }

  private void doDelete() throws Exception {
    int removedCount;
    for (DateType dateType : DateType.values()) {
      do {
        removedCount = inTxRetry(databaseInstance)
            .call(db -> assetDownloadCountEntityAdapter.removeOldRecords(db, dateType));
        log.debug("Removed {} old records of type {}", removedCount, dateType.name());
      }
      //keep repeating the delete until there are none left
      while (removedCount > 0 && removedCount == assetDownloadCountEntityAdapter.getMaxDeleteSize());
    }
  }
}
