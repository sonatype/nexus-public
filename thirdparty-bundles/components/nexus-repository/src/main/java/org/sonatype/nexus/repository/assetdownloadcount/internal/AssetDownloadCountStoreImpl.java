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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.assetdownloadcount.AssetDownloadCountStore;
import org.sonatype.nexus.repository.assetdownloadcount.DateType;
import org.sonatype.nexus.repository.storage.ComponentDatabase;
import org.sonatype.nexus.thread.NexusExecutorService;
import org.sonatype.nexus.thread.NexusThreadFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListeners;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.joda.time.DateTime;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTxRetry;

/**
 * Manages shuffling counts data to from the database, as well as a cache of counts to not overwhelm the request
 * handling with these db updates
 *
 * @since 3.4
 */
@Named
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
public class AssetDownloadCountStoreImpl
    extends StateGuardLifecycleSupport
    implements AssetDownloadCountStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final AssetDownloadCountEntityAdapter entityAdapter;

  private final boolean enabled;

  private final AssetDownloadHistoricDataCleaner historicDataCleaner;

  private final LoadingCache<CacheEntryKey,AtomicLong> cache;

  @Inject
  public AssetDownloadCountStoreImpl(@Named(ComponentDatabase.NAME) final Provider<DatabaseInstance> databaseInstance,
                                     @Named("${nexus.assetdownloads.enabled:-true}") final boolean enabled,
                                     @Named("${nexus.assetdownloads.cache.size:-10000}") final int cacheSize,
                                     @Named("${nexus.assetdownloads.cache.duration:-3600}") final int cacheDuration,
                                     final AssetDownloadCountEntityAdapter entityAdapter,
                                     final AssetDownloadHistoricDataCleaner historicDataCleaner,
                                     final CacheRemovalListener cacheRemovalListener)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
    this.historicDataCleaner = checkNotNull(historicDataCleaner);
    this.enabled = enabled;

    cache = CacheBuilder.newBuilder()
        .maximumSize(cacheSize)
        .expireAfterWrite(cacheDuration, TimeUnit.SECONDS)
        .removalListener(RemovalListeners.asynchronous(cacheRemovalListener, NexusExecutorService
            .forCurrentSubject(Executors.newSingleThreadExecutor(
                new NexusThreadFactory("assetdownloads-count", "Asset Downloads Count")))))
        .build(new CacheLoader<CacheEntryKey, AtomicLong>()
        {
          @Override
          public AtomicLong load(final CacheEntryKey cacheEntryKey) throws Exception {
            return new AtomicLong(0);
          }
        });
  }

  @Override
  protected void doStart() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      entityAdapter.register(db);
    }
  }

  @Override
  protected void doStop() throws Exception {
    historicDataCleaner.stop();
  }

  @Override
  @Guarded(by = STARTED)
  public long getDailyCount(final String repositoryName,
                            final String assetName,
                            final DateTime date)
  {
    long count = inTx(databaseInstance)
        .call(db -> entityAdapter.getCount(db, repositoryName, assetName, DateType.DAY, date));
    log.debug("Get daily count for {} {} {} return {}", repositoryName, assetName, date, count);
    return count;
  }

  @Override
  @Guarded(by = STARTED)
  public long getMonthlyCount(final String repositoryName,
                              final String assetName,
                              final DateTime date)
  {
    long count = inTx(databaseInstance)
        .call(db -> entityAdapter.getCount(db, repositoryName, assetName, DateType.MONTH, date));
    log.debug("Get monthly count for {} {} {} return {}", repositoryName, assetName, date, count);
    return count;
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getDailyCounts(final String repositoryName,
                               final String assetName)
  {
    long[] counts = inTx(databaseInstance)
        .call(db -> entityAdapter.getCounts(db, repositoryName, assetName, DateType.DAY));
    log.debug("Get daily counts for {} {} {}", repositoryName, assetName, counts);
    return counts;
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyCounts(final String repositoryName,
                                 final String assetName)
  {
    long[] counts = inTx(databaseInstance)
        .call(db -> entityAdapter.getCounts(db, repositoryName, assetName, DateType.MONTH));
    log.debug("Get monthly counts for {} {} {}", repositoryName, assetName, counts);
    return counts;
  }

  @Override
  @Guarded(by = STARTED)
  public void incrementCount(final String repositoryName, final String assetName) {
    log.debug("Incremented count(CACHE) {} {} by {}", repositoryName, assetName, 1);

    //with the dead simple impl of the cache loader, i dont have concerns doing the getUnchecked over get method
    cache.getUnchecked(new CacheEntryKey(repositoryName, assetName)).incrementAndGet();

    historicDataCleaner.start();
  }

  @Override
  @Guarded(by = STARTED)
  public void setMonthlyVulnerableCount(final String repositoryName,
                                        final DateTime date,
                                        final long count)
  {
    inTxRetry(databaseInstance).run(db -> {
      entityAdapter.setCount(db, repositoryName, DateType.MONTH_WHOLE_REPO_VULNERABLE, date, count);
      log.debug("Setting monthly vulnerability count {} {} {}", repositoryName, date, count);
    });
  }

  @Override
  @Guarded(by = STARTED)
  public void setMonthlyCount(final String repositoryName,
                              final DateTime date,
                              final long count)
  {
    inTxRetry(databaseInstance).run(db -> {
      entityAdapter.setCount(db, repositoryName, DateType.MONTH_WHOLE_REPO, date, count);
      log.debug("Setting monthly count {} {} {}", repositoryName, date, count);
    });
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyCounts(final String repositoryName) {
    return inTx(databaseInstance).call(db -> {
      long[] counts = entityAdapter.getCounts(db, repositoryName, DateType.MONTH_WHOLE_REPO);
      log.debug("Get monthly counts for  {} {}", repositoryName, counts);
      return counts;
    });
  }

  @Override
  @Guarded(by = STARTED)
  public long[] getMonthlyVulnerableCounts(final String repositoryName) {
    return inTx(databaseInstance).call(db -> {
      long[] counts = entityAdapter.getCounts(db, repositoryName, DateType.MONTH_WHOLE_REPO_VULNERABLE);
      log.debug("Get monthly vulnerable counts for  {} {}", repositoryName, counts);
      return counts;
    });
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  @Guarded(by = STARTED)
  public long getLastThirtyDays(final String repositoryName, final String assetName) {
    long lastThirty = 0;
    for (long day : getDailyCounts(repositoryName, assetName)) {
      lastThirty += day;
    }
    lastThirty += cache.getUnchecked(new CacheEntryKey(repositoryName, assetName)).get();
    return lastThirty;
  }
}
